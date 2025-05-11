package io.github.genkt.jsonrpc.server

import io.github.genkt.jsonrpc.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

public class JsonRpcServer(
    public val transport: JsonRpcServerTransport,
    public val onRequest: suspend (JsonRpcRequest) -> JsonRpcServerMessage,
    public val onNotification: suspend (JsonRpcNotification) -> Unit,
    public val coroutineContext: CoroutineContext = Dispatchers.Default,
) : AutoCloseable {
    private val receiveJob = CoroutineScope(coroutineContext).launch {
        transport.receiveFlow
            .catch { coroutineContext.handleException(it) }
            .collect { handleMessage(it) }
    }

    private suspend fun handleMessage(request: JsonRpcClientMessage) {
        when (request) {
            is JsonRpcRequest -> transport.sendChannel.send(onRequest(request))
            is JsonRpcNotification -> onNotification(request)
            is JsonRpcClientMessageBatch -> request.messages.forEach { handleMessage(it) }
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