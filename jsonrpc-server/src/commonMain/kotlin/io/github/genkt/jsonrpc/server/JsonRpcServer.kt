package io.github.genkt.jsonrpc.server

import io.github.genkt.jsonrpc.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.serialization.json.JsonPrimitive
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.JvmName

public class JsonRpcServer(
    public val transport: JsonRpcServerTransport,
    onRequest: suspend CoroutineScope.(JsonRpcRequest) -> JsonRpcServerMessage,
    onNotification: suspend CoroutineScope.(JsonRpcNotification) -> Unit,
    coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob(),
) : AutoCloseable {
    private val onRequest = onRequest.safeHandler()
    private val onNotification = onNotification.safeHandler()
    private val coroutineScope = CoroutineScope(coroutineContext)
    private val receiveJob = coroutineScope.launch {
        transport.receiveFlow
            .catch { coroutineContext.handleException(it) }
            .collect { handleMessage(it) }
    }

    private fun CoroutineScope.handleMessage(request: JsonRpcClientMessage) {
        launch(receiveJob) {
            when (request) {
                is JsonRpcRequest -> transport.sendChannel.send(onRequest(request))
                is JsonRpcNotification -> onNotification(request)
                is JsonRpcClientMessageBatch -> request.messages.forEach { handleMessage(it) }
            }
        }
    }

    override fun close() {
        receiveJob.cancel()
        transport.close()
    }
}

private fun CoroutineContext.handleException(
    exception: Throwable,
) {
    this[CoroutineExceptionHandler]?.handleException(this, exception)
}

@JvmName("safeHandler\$JsonRpcRequest")
private fun (suspend CoroutineScope.(JsonRpcRequest) -> JsonRpcServerMessage).safeHandler():
        (suspend CoroutineScope.(JsonRpcRequest) -> JsonRpcServerMessage) {
    return { request ->
        try {
            this.this@safeHandler(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            currentCoroutineContext().handleException(e)
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

@JvmName("safeHandler\$JsonRpcNotification")
private fun (suspend CoroutineScope.(JsonRpcNotification) -> Unit).safeHandler():
        (suspend CoroutineScope.(JsonRpcNotification) -> Unit) {
    return { notification ->
        try {
            this.this@safeHandler(notification)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            currentCoroutineContext().handleException(e)
        }
    }
}