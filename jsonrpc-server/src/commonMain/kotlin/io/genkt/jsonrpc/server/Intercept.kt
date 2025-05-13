package io.genkt.jsonrpc.server

import io.genkt.jsonrpc.Interceptor
import io.genkt.jsonrpc.JsonRpcClientMessage
import io.genkt.jsonrpc.JsonRpcNotification
import io.genkt.jsonrpc.JsonRpcRequest
import io.genkt.jsonrpc.JsonRpcServerMessage
import io.genkt.jsonrpc.JsonRpcServerTransport
import io.genkt.jsonrpc.TransportInterceptor
import io.genkt.jsonrpc.build
import kotlinx.coroutines.CoroutineScope

public class JsonRpcServerInterceptor(
    public val interceptTransport: Interceptor<JsonRpcServerTransport>,
    public val interceptRequestHandler: Interceptor<suspend (JsonRpcRequest) -> JsonRpcServerMessage> = { it },
    public val interceptNotificationHandler: Interceptor<suspend (JsonRpcNotification) -> Unit>,
    public val interceptErrorHandler: Interceptor<suspend CoroutineScope.(Throwable) -> Unit>,
): Interceptor<JsonRpcServer> by { InterceptedJsonRpcServer(it, this) } {
    public class Builder {
        public var transportInterceptor: Interceptor<JsonRpcServerTransport> = { it }
        public var requestHandlerInterceptor: Interceptor<suspend (JsonRpcRequest) -> JsonRpcServerMessage> = { it }
        public var notificationHandlerInterceptor: Interceptor<suspend (JsonRpcNotification) -> Unit> = { it }
        public var errorHandlerInterceptor: Interceptor<suspend CoroutineScope.(Throwable) -> Unit> = { it }
    }
}

public fun JsonRpcServerInterceptor.Builder.build(): JsonRpcServerInterceptor
    = JsonRpcServerInterceptor(
        transportInterceptor,
        requestHandlerInterceptor,
        notificationHandlerInterceptor,
        errorHandlerInterceptor
    )

public fun JsonRpcServerInterceptor.Builder.interceptRequestHandler(value: Interceptor<suspend (JsonRpcRequest) -> JsonRpcServerMessage>) {
    requestHandlerInterceptor = value
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

public fun JsonRpcServerInterceptor.Builder.interceptTransport(buildAction: TransportInterceptor.Builder<JsonRpcServerMessage, JsonRpcClientMessage>.() -> Unit) {
    transportInterceptor = TransportInterceptor.Builder<JsonRpcServerMessage, JsonRpcClientMessage>()
        .apply(buildAction)
        .build()
}

public fun JsonRpcServer.intercept(
    interceptor: JsonRpcServerInterceptor
): JsonRpcServer = interceptor(this)

public fun JsonRpcServer.intercepted(
    block: JsonRpcServerInterceptor.Builder.() -> Unit
) = intercept(JsonRpcServerInterceptor.Builder().apply(block).build())