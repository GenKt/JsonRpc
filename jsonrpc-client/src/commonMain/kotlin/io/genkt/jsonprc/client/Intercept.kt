package io.genkt.jsonprc.client

import io.genkt.jsonrpc.*
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

public class JsonRpcClientInterceptor(
    public val interceptTransport: Interceptor<JsonRpcClientTransport>,
    public val interceptCall: Interceptor<suspend (JsonRpcClientCall<*>) -> Any?>,
    public val interceptErrorHandler: Interceptor<suspend CoroutineScope.(Throwable) -> Unit>,
    public val additionalCoroutineContext: CoroutineContext,
) : Interceptor<JsonRpcClient> {
    override fun invoke(client: JsonRpcClient): JsonRpcClient = InterceptedJsonRpcClient(client, this)
    public class Builder : GenericInterceptorScope {
        public var transportInterceptor: Interceptor<JsonRpcClientTransport> = { it }
        public var callInterceptor: Interceptor<suspend (JsonRpcClientCall<*>) -> Any?> = { it }
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
        interceptCall = callInterceptor,
        interceptErrorHandler = errorHandlerInterceptor,
        additionalCoroutineContext = additionalCoroutineContext,
    )

public fun JsonRpcClient.interceptWith(
    interceptor: JsonRpcClientInterceptor,
): JsonRpcClient = interceptor(this)

public fun JsonRpcClient.intercept(
    buildAction: JsonRpcClientInterceptor.Builder.() -> Unit
): JsonRpcClient = interceptWith(JsonRpcClientInterceptor(buildAction))
