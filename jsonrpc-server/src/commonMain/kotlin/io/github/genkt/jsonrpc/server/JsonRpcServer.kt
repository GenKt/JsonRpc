package io.github.genkt.jsonrpc.server

import io.github.genkt.jsonrpc.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.cancellable
import kotlinx.serialization.json.JsonPrimitive
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

public fun CoroutineScope.JsonRpcServer(
    transport: JsonRpcServerTransport,
    onRequest: suspend CoroutineScope.(JsonRpcRequest) -> JsonRpcServerMessage,
    onNotification: suspend CoroutineScope.(JsonRpcNotification) -> Unit,
    errorHandler: suspend CoroutineScope.(Throwable) -> Unit = defaultErrorHandler
): JsonRpcServer = JsonRpcServer(
    transport,
    onRequest,
    onNotification,
    coroutineContext,
    errorHandler
)

public class JsonRpcServer(
    public val transport: JsonRpcServerTransport,
    private val onRequest: suspend CoroutineScope.(JsonRpcRequest) -> JsonRpcServerMessage,
    private val onNotification: suspend CoroutineScope.(JsonRpcNotification) -> Unit,
    coroutineContext: CoroutineContext = Dispatchers.Default + Job(),
    private val errorHandler: suspend CoroutineScope.(Throwable) -> Unit = defaultErrorHandler
) : AutoCloseable {
    private val coroutineScope = CoroutineScope(coroutineContext)
    private val receiveJob = coroutineScope.launch {
        transport.receiveFlow.cancellable()
            .collect { handleMessageSafe(it) }
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
            JsonRpcFailResponse(
                request.id,
                JsonRpcFailResponse.Error(
                    code = JsonRpcFailResponse.Error.Code.InternalError,
                    message = e.message ?: "Internal error",
                    data = JsonPrimitive(e.stackTraceToString())
                )
            )
        }
        transport.sendChannel.send(response)
    }

    override fun close() {
        coroutineScope.cancel()
        transport.close()
    }
}

private val defaultErrorHandler: suspend CoroutineScope.(Throwable) -> Unit = { throwable ->
    if (throwable is CancellationException) throw throwable
    else coroutineContext[CoroutineExceptionHandler]?.handleException(coroutineContext, throwable)
}