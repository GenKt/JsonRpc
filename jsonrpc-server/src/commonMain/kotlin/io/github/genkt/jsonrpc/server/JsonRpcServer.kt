package io.github.genkt.jsonrpc.server

import io.github.genkt.jsonrpc.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlin.coroutines.CoroutineContext

public class JsonRpcServer(
    public val transport: JsonRpcServerTransport,
    public val onRequest: suspend (JsonRpcRequest) -> JsonRpcServerMessage,
    public val onNotification: suspend CoroutineScope.(JsonRpcNotification) -> Unit,
    public val coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob(),
) : AutoCloseable {
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