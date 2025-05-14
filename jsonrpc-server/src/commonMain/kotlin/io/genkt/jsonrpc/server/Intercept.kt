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

public fun JsonRpcServerInterceptor.Builder.customErrorResponse(value: suspend (Throwable) -> JsonRpcServerSingleMessage) {
    requestHandlerInterceptor = { requestHandler ->
        { request ->
            try {
                requestHandler(request)
            } catch (e: Throwable) {
                value(e)
            }
        }
    }
}

public fun JsonRpcServerInterceptor.Builder.interceptNotificationHandler(value: Interceptor<suspend (JsonRpcNotification) -> Unit>) {
    notificationHandlerInterceptor = value
}

public fun JsonRpcServerInterceptor.Builder.interceptErrorHandler(value: Interceptor<suspend CoroutineScope.(Throwable) -> Unit>) {
    errorHandlerInterceptor = value
}

public fun JsonRpcServerInterceptor.Builder.interceptTransport(value: Interceptor<JsonRpcServerTransport>) {
    transportInterceptor = value
}

public fun JsonRpcServerInterceptor.Builder.buildTransportInterceptor(buildAction: TransportInterceptor.Builder<JsonRpcServerMessage, JsonRpcClientMessage>.() -> Unit) {
    transportInterceptor = TransportInterceptor.Builder<JsonRpcServerMessage, JsonRpcClientMessage>()
        .apply(buildAction)
        .build()
}

public fun JsonRpcServerInterceptor.Builder.addCoroutineContext(value: CoroutineContext) {
    additionalCoroutineContext = additionalCoroutineContext + value
}

public fun JsonRpcServer.intercept(
    interceptor: JsonRpcServerInterceptor
): JsonRpcServer = interceptor(this)

public fun JsonRpcServer.intercepted(
    block: JsonRpcServerInterceptor.Builder.() -> Unit
) = intercept(JsonRpcServerInterceptor.Builder().apply(block).build())