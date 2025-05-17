package io.genkt.jsonrpc.server

import io.genkt.jsonrpc.JsonRpcNotification
import io.genkt.jsonrpc.JsonRpcRequest
import io.genkt.jsonrpc.JsonRpcServerMessage
import io.genkt.jsonrpc.JsonRpcServerTransport
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

public fun JsonRpcServer(
    transport: JsonRpcServerTransport,
    onRequest: suspend (JsonRpcRequest) -> JsonRpcServerMessage,
    onNotification: suspend (JsonRpcNotification) -> Unit,
    errorHandler: suspend CoroutineScope.(Throwable) -> Unit = {},
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    buildInterceptor: (JsonRpcServerInterceptor.Builder.() -> Unit)? = null,
): JsonRpcServer = JsonRpcServerImpl(
    transport,
    onRequest,
    onNotification,
    errorHandler,
    coroutineContext
).let {
    if (buildInterceptor == null) it
    else it.intercept(buildInterceptor)
}

public interface JsonRpcServer : AutoCloseable {
    public val transport: JsonRpcServerTransport
    public val onRequest: suspend (JsonRpcRequest) -> JsonRpcServerMessage
    public val onNotification: suspend (JsonRpcNotification) -> Unit
    public val errorHandler: suspend CoroutineScope.(Throwable) -> Unit
    public val coroutineScope: CoroutineScope
    public fun start()
}