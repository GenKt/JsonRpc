package io.genkt.jsonprc.client

import io.genkt.jsonrpc.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

public fun JsonRpcClient(
    transport: JsonRpcClientTransport,
    errorHandler: suspend CoroutineScope.(Throwable) -> Unit = {},
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
): JsonRpcClient = JsonRpcClientImpl(
    transport,
    errorHandler,
    coroutineContext
)

public interface JsonRpcClient : AutoCloseable {
    public val transport: JsonRpcClientTransport
    public val errorHandler: suspend CoroutineScope.(Throwable) -> Unit
    public val coroutineScope: CoroutineScope
    public suspend fun start()
    public suspend fun send(request: JsonRpcRequest): JsonRpcSuccessResponse
    public suspend fun send(notification: JsonRpcNotification)
    public suspend fun <T, R> onCall(call: Call<T, R>): R
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