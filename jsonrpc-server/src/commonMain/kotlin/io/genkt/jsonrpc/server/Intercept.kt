package io.genkt.jsonrpc.server

import io.genkt.jsonrpc.*
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

public class JsonRpcServerInterceptor(
    public val interceptTransport: Interceptor<JsonRpcServerTransport>,
    public val interceptRequestHandler: Interceptor<suspend (JsonRpcRequest) -> JsonRpcServerMessage> = { it },
    public val interceptNotificationHandler: Interceptor<suspend (JsonRpcNotification) -> Unit>,
    public val interceptErrorHandler: Interceptor<suspend CoroutineScope.(Throwable) -> Unit>,
    public val additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext,
) : Interceptor<JsonRpcServer> {
    override fun invoke(server: JsonRpcServer): JsonRpcServer = InterceptedJsonRpcServer(server, this)
    public class Builder {
        public var transportInterceptor: Interceptor<JsonRpcServerTransport> = { it }
        public var requestHandlerInterceptor: Interceptor<suspend (JsonRpcRequest) -> JsonRpcServerMessage> = { it }
        public var notificationHandlerInterceptor: Interceptor<suspend (JsonRpcNotification) -> Unit> = { it }
        public var errorHandlerInterceptor: Interceptor<suspend CoroutineScope.(Throwable) -> Unit> = { it }
        public var additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext
    }

    public companion object {
        public operator fun invoke(buildAction: Builder.() -> Unit): JsonRpcServerInterceptor =
            Builder().apply(buildAction).build()
    }
}

public fun JsonRpcServerInterceptor.Builder.build(): JsonRpcServerInterceptor =
    JsonRpcServerInterceptor(
        transportInterceptor,
        requestHandlerInterceptor,
        notificationHandlerInterceptor,
        errorHandlerInterceptor,
        additionalCoroutineContext,
    )

public fun JsonRpcServerInterceptor.Builder.interceptRequestHandler(value: Interceptor<suspend (JsonRpcRequest) -> JsonRpcServerMessage>) {
    requestHandlerInterceptor = value
}

@Suppress("FunctionName")
public inline fun JsonRpcServerInterceptor.Builder.CustomErrorResponse(
    crossinline generateErrorResponse: suspend (Throwable) -> JsonRpcServerSingleMessage
): Interceptor<suspend (JsonRpcRequest) -> JsonRpcServerMessage> =
    { requestHandler ->
        { request ->
            try {
                requestHandler(request)
            } catch (e: Throwable) {
                generateErrorResponse(e)
            }
        }
    }

public fun JsonRpcServer.intercept(
    interceptor: JsonRpcServerInterceptor
): JsonRpcServer = interceptor(this)

public fun JsonRpcServer.intercepted(
    buildAction: JsonRpcServerInterceptor.Builder.() -> Unit
) = intercept(JsonRpcServerInterceptor(buildAction))