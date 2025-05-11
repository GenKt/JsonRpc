package io.github.genkt.jsonrpc.server

import io.github.genkt.jsonrpc.JsonRpcClientMessage
import io.github.genkt.jsonrpc.JsonRpcClientMessageBatch
import io.github.genkt.jsonrpc.JsonRpcNotification
import io.github.genkt.jsonrpc.JsonRpcRequest
import io.github.genkt.jsonrpc.JsonRpcServerMessage
import io.github.genkt.jsonrpc.JsonRpcServerTransport
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

public class JsonRpcServer(
    public val transport: JsonRpcServerTransport,
    public val onRequest: suspend (JsonRpcRequest) -> JsonRpcServerMessage,
    public val onNotification: suspend (JsonRpcNotification) -> Unit,
    public val coroutineContext: CoroutineContext = Dispatchers.Default,
) : AutoCloseable {
    private val receiveJob = CoroutineScope(coroutineContext).launch {
        transport.receiveChannel.consumeAsFlow()
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
        transport.sendChannel.close()
        transport.receiveChannel.cancel()
    }
}

private fun CoroutineContext.handleException(
    exception: Throwable,
) {
    this[CoroutineExceptionHandler]?.handleException(this, exception)
}