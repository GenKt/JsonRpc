package io.github.genkt.jsonrpc.server

import io.github.genkt.jsonrpc.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonPrimitive
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

public class JsonRpcServer(
    public val transport: JsonRpcServerTransport,
    onRequest: suspend CoroutineScope.(JsonRpcRequest) -> JsonRpcServerMessage,
    private val onNotification: suspend CoroutineScope.(JsonRpcNotification) -> Unit,
    coroutineContext: CoroutineContext = Dispatchers.Default + Job(),
) : AutoCloseable {
    private val onRequest = onRequest.safeHandler()
    private val coroutineScope = CoroutineScope(coroutineContext)
    private val receiveJob = coroutineScope.launch {
        transport.receiveFlow
            .collect { handleMessageSafe(it) }
    }

    private fun CoroutineScope.handleMessageSafe(request: JsonRpcClientMessage) {
        launch(receiveJob) {
            when (request) {
                is JsonRpcRequest -> transport.sendChannel.send(onRequest(request))
                is JsonRpcNotification -> onNotification(request)
                is JsonRpcClientMessageBatch -> request.messages.forEach { handleMessageSafe(it) }
            }
        }
    }

    override fun close() {
        receiveJob.cancel()
        transport.close()
    }
}

private fun (suspend CoroutineScope.(JsonRpcRequest) -> JsonRpcServerMessage).safeHandler():
        (suspend CoroutineScope.(JsonRpcRequest) -> JsonRpcServerMessage) {
    return { request ->
        try {
            this.this@safeHandler(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            JsonRpcFailResponse(
                id = request.id,
                error = JsonRpcFailResponse.Error(
                    code = JsonRpcFailResponse.Error.Code.InternalError,
                    message = e.message ?: "Internal error",
                    data = JsonPrimitive(e.stackTraceToString())
                )
            )
        }
    }
}