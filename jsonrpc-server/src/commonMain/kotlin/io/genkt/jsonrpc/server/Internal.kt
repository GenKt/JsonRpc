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
    val onRequest: suspend (JsonRpcRequest) -> JsonRpcServerMessage,
    val onNotification: suspend (JsonRpcNotification) -> Unit,
    val uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit = {},
    additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext,
) : JsonRpcServer {
    override val coroutineScope: CoroutineScope = transport.coroutineScope.newChild(additionalCoroutineContext)
    override fun start() {
        transport.start()
        coroutineScope.launch {
            transport.receiveFlow.cancellable()
                .collect { result ->
                    result.onSuccess {
                        handleMessageSafe(it)
                    }.onFailure {
                        uncaughtErrorHandler(it)
                    }
                }
        }
    }

    private fun CoroutineScope.handleMessageSafe(request: JsonRpcClientMessage) {
        launch {
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
            launch { uncaughtErrorHandler(e) }
        }
    }

    private suspend fun CoroutineScope.onRequestSafe(request: JsonRpcRequest) {
        val response = try {
            onRequest(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            launch { uncaughtErrorHandler(e) }
            fallbackFailResponse(request, e)
        }
        launch { transport.sendChannel.sendOrThrow(response) }
    }

    override fun close() {
        coroutineScope.cancel()
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
    CoroutineScope(this.coroutineContext + coroutineContext + SupervisorJob(this.coroutineContext[Job]))