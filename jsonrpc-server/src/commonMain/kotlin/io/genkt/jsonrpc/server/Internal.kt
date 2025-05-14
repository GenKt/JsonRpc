package io.genkt.jsonrpc.server

import io.genkt.jsonrpc.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.cancellable
import kotlinx.serialization.json.JsonPrimitive
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException

internal class JsonRpcServerImpl(
    override val transport: JsonRpcServerTransport,
    override val onRequest: suspend (JsonRpcRequest) -> JsonRpcServerMessage,
    override val onNotification: suspend (JsonRpcNotification) -> Unit,
    override val errorHandler: suspend CoroutineScope.(Throwable) -> Unit = {},
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
) : JsonRpcServer {
    override val coroutineScope: CoroutineScope = transport.coroutineScope.newChild(coroutineContext)
    private val receiveJob = coroutineScope.launch(start = CoroutineStart.LAZY) {
        transport.receiveFlow.cancellable()
            .collect { result ->
                result.onSuccess {
                    handleMessageSafe(it)
                }.onFailure {
                    errorHandler(it)
                }
            }
    }

    override fun start() {
        receiveJob.start()
    }

    private fun CoroutineScope.handleMessageSafe(request: JsonRpcClientMessage) {
        launch(receiveJob) {
            when (request) {
                is JsonRpcRequest -> onRequestSafe(request)
                is JsonRpcNotification -> onNotificationSafe(request)
                is JsonRpcClientMessageBatch -> request.messages.forEach { handleMessageSafe(it) }
            }
        }
    }

    private suspend fun CoroutineScope.onNotificationSafe(request: JsonRpcNotification) {
        try {
            onNotification(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            launch { errorHandler(e) }
        }
    }

    private suspend fun CoroutineScope.onRequestSafe(request: JsonRpcRequest) {
        val response = try {
            onRequest(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            launch { errorHandler(e) }
            fallbackFailResponse(request, e)
        }
        launch { transport.sendChannel.sendOrThrow(response) }
    }

    override fun close() {
        coroutineScope.cancel()
        transport.close()
    }
}

internal class InterceptedJsonRpcServer(
    private val delegate: JsonRpcServer,
    interceptor: JsonRpcServerInterceptor,
) : JsonRpcServer by JsonRpcServerImpl(
    transport = interceptor.interceptTransport(delegate.transport),
    onRequest = interceptor.interceptRequestHandler(delegate.onRequest),
    onNotification = interceptor.interceptNotificationHandler(delegate.onNotification),
    errorHandler = interceptor.interceptErrorHandler(delegate.errorHandler),
    coroutineContext = delegate.coroutineScope.coroutineContext + interceptor.additionalCoroutineContext,
) {
    override fun close() {
        // TODO: check if this ordering is proper
        coroutineScope.cancel()
        delegate.close()
        transport.close()
    }
}

private fun fallbackFailResponse(
    request: JsonRpcRequest,
    e: Throwable
): JsonRpcFailResponse = JsonRpcFailResponse(
    request.id,
    JsonRpcFailResponse.Error(
        code = JsonRpcFailResponse.Error.Code.InternalError,
        message = e.message ?: "Internal error",
        data = JsonPrimitive(e.stackTraceToString())
    )
)

private fun CoroutineScope.newChild(coroutineContext: CoroutineContext): CoroutineScope =
    CoroutineScope(this.coroutineContext + coroutineContext + Job(this.coroutineContext[Job]))