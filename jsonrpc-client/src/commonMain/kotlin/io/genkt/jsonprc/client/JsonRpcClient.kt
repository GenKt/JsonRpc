package io.genkt.jsonprc.client

import io.genkt.jsonrpc.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

public fun JsonRpcClient(
    transport: JsonRpcClientTransport,
    errorHandler: suspend CoroutineScope.(Throwable) -> Unit = {},
): JsonRpcClient = JsonRpcClientImpl(
    transport,
    errorHandler,
)

public interface JsonRpcClient : AutoCloseable {
    public val transport: JsonRpcClientTransport
    public val errorHandler: suspend CoroutineScope.(Throwable) -> Unit
    public suspend fun send(request: JsonRpcRequest): JsonRpcSuccessResponse
    public suspend fun send(notification: JsonRpcNotification)
}

public suspend fun JsonRpcClient.sendRequest(
    id: RequestId,
    method: String,
    params: JsonElement? = null,
    jsonrpc: String = JsonRpc.VERSION,
): JsonRpcSuccessResponse {
    return send(
        JsonRpcRequest(
            id = id,
            method = method,
            params = params ?: JsonNull,
            jsonrpc = jsonrpc,
        )
    )
}

public suspend fun JsonRpcClient.sendNotification(
    method: String,
    params: JsonElement? = null,
    jsonrpc: String = JsonRpc.VERSION,
) {
    send(
        JsonRpcNotification(
            method = method,
            params = params ?: JsonNull,
            jsonrpc = jsonrpc,
        )
    )
}