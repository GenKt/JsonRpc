package io.genkt.jsonprc.client

import io.genkt.jsonrpc.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

public interface JsonRpcClient : AutoCloseable {
    public val transport: JsonRpcClientTransport
    public val uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit
    public val coroutineScope: CoroutineScope
    public suspend fun start()
    public suspend fun <R> execute(call: JsonRpcClientCall<R>): R
    public class Builder: GenericInterceptorScope {
        public var transport: JsonRpcClientTransport = Transport.ThrowingException { error("Using an uninitialized transport") }
        public var uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit = {}
        public var callInterceptor: Interceptor<suspend (JsonRpcClientCall<*>) -> Any?> = { it }
        public var additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext
    }
}

public suspend fun JsonRpcClient.sendRequest(
    id: RequestId,
    method: String,
    params: JsonElement? = null,
    jsonrpc: String = JsonRpc.VERSION,
): JsonRpcSuccessResponse = execute(
    JsonRpcRequest(
        id = id,
        method = method,
        params = params ?: JsonNull,
        jsonrpc = jsonrpc,
    )
)

public suspend fun JsonRpcClient.sendNotification(
    method: String,
    params: JsonElement? = null,
    jsonrpc: String = JsonRpc.VERSION,
): Unit = execute(
    JsonRpcNotification(
        method = method,
        params = params ?: JsonNull,
        jsonrpc = jsonrpc,
    )
)