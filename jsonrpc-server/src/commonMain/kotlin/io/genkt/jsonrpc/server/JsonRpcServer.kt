package io.genkt.jsonrpc.server

import io.genkt.jsonrpc.JsonRpcNotification
import io.genkt.jsonrpc.JsonRpcRequest
import io.genkt.jsonrpc.JsonRpcServerMessage
import io.genkt.jsonrpc.JsonRpcServerTransport
import kotlinx.coroutines.*
import kotlin.coroutines.cancellation.CancellationException

public fun JsonRpcServer(
    transport: JsonRpcServerTransport,
    onRequest: suspend (JsonRpcRequest) -> JsonRpcServerMessage,
    onNotification: suspend (JsonRpcNotification) -> Unit,
    errorHandler: suspend CoroutineScope.(Throwable) -> Unit = {}
): JsonRpcServer = JsonRpcServerImpl(
    transport,
    onRequest,
    onNotification,
    errorHandler
)

public interface JsonRpcServer: AutoCloseable {
    public val transport: JsonRpcServerTransport
    public val onRequest: suspend (JsonRpcRequest) -> JsonRpcServerMessage
    public val onNotification: suspend (JsonRpcNotification) -> Unit
    public val errorHandler: suspend CoroutineScope.(Throwable) -> Unit
}