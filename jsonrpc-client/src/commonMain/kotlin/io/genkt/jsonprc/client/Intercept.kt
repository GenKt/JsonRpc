package io.genkt.jsonprc.client

import io.genkt.jsonrpc.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

public class JsonRpcClientInterceptor(
    public val interceptTransport: Interceptor<JsonRpcClientTransport> = { it },
    public val interceptRequest: Interceptor<suspend (JsonRpcRequest) -> JsonRpcSuccessResponse> = { it },
    public val interceptNotification: Interceptor<suspend (JsonRpcNotification) -> Unit> = { it },
    public val interceptErrorHandler: Interceptor<suspend CoroutineScope.(Throwable) -> Unit> = { it },
    public val additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext,
) : Interceptor<JsonRpcClient> {
    override fun invoke(client: JsonRpcClient): JsonRpcClient = InterceptedJsonRpcClient(client, this)
    public class Builder {
        public var transportInterceptor: Interceptor<JsonRpcClientTransport> = { it }
        public var requestInterceptor: Interceptor<suspend (JsonRpcRequest) -> JsonRpcSuccessResponse> = { it }
        public var notificationInterceptor: Interceptor<suspend (JsonRpcNotification) -> Unit> = { it }
        public var errorHandlerInterceptor: Interceptor<suspend CoroutineScope.(Throwable) -> Unit> = { it }
        public var additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext
    }
    public companion object {
        public operator fun invoke(buildAction: Builder.() -> Unit): JsonRpcClientInterceptor =
            Builder().apply(buildAction).build()
    }
}

public fun JsonRpcClientInterceptor.Builder.build(): JsonRpcClientInterceptor =
    JsonRpcClientInterceptor(
        interceptTransport = transportInterceptor,
        interceptRequest = requestInterceptor,
        interceptNotification = notificationInterceptor,
        interceptErrorHandler = errorHandlerInterceptor,
    )

@Suppress("FunctionName")
public fun <T, R> JsonRpcClientInterceptor.Builder.TimeOut(duration: Duration): Interceptor<suspend (T) -> R> =
    { f -> { param -> withTimeout(duration) { f(param) } } }

public fun JsonRpcClient.intercept(
    interceptor: JsonRpcClientInterceptor,
): JsonRpcClient = interceptor(this)

public fun JsonRpcClient.intercepted(buildAction: JsonRpcClientInterceptor.Builder.() -> Unit): JsonRpcClient =
    intercept(JsonRpcClientInterceptor.Builder().apply(buildAction).build())
