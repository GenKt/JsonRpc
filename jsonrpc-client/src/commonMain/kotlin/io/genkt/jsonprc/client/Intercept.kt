package io.genkt.jsonprc.client

import io.genkt.jsonrpc.*
import kotlinx.coroutines.CoroutineScope

public class JsonRpcClientInterceptor(
    public val interceptTransport: Interceptor<JsonRpcClientTransport> = { it },
    public val interceptRequest: Interceptor<suspend (JsonRpcRequest) -> JsonRpcSuccessResponse> = { it },
    public val interceptNotification: Interceptor<suspend (JsonRpcNotification) -> Unit> = { it },
    public val interceptErrorHandler: Interceptor<suspend CoroutineScope.(Throwable) -> Unit> = { it },
) : Interceptor<JsonRpcClient> by { InterceptedJsonRpcClient(it, this) } {
    public class Builder {
        public var transportInterceptor: Interceptor<JsonRpcClientTransport> = { it }
        public var requestInterceptor: Interceptor<suspend (JsonRpcRequest) -> JsonRpcSuccessResponse> = { it }
        public var notificationInterceptor: Interceptor<suspend (JsonRpcNotification) -> Unit> = { it }
        public var errorHandlerInterceptor: Interceptor<suspend CoroutineScope.(Throwable) -> Unit> = { it }
    }
}

public fun JsonRpcClientInterceptor.Builder.build(): JsonRpcClientInterceptor =
    JsonRpcClientInterceptor(
        interceptTransport = transportInterceptor,
        interceptRequest = requestInterceptor,
        interceptNotification = notificationInterceptor,
        interceptErrorHandler = errorHandlerInterceptor,
    )

public fun JsonRpcClientInterceptor.Builder.interceptRequest(value: Interceptor<suspend (JsonRpcRequest) -> JsonRpcSuccessResponse>) {
    requestInterceptor = value
}

public fun JsonRpcClientInterceptor.Builder.interceptNotification(value: Interceptor<suspend (JsonRpcNotification) -> Unit>) {
    notificationInterceptor = value
}

public fun JsonRpcClientInterceptor.Builder.interceptErrorHandler(value: Interceptor<suspend CoroutineScope.(Throwable) -> Unit>) {
    errorHandlerInterceptor = value
}

public fun JsonRpcClientInterceptor.Builder.interceptTransport(value: Interceptor<JsonRpcClientTransport>) {
    transportInterceptor = value
}

public fun JsonRpcClientInterceptor.Builder.interceptTransport(buildAction: TransportInterceptor.Builder<JsonRpcClientMessage, JsonRpcServerMessage>.() -> Unit) {
    transportInterceptor = TransportInterceptor.Builder<JsonRpcClientMessage, JsonRpcServerMessage>()
        .apply(buildAction)
        .build()
}

public fun JsonRpcClient.intercept(
    interceptor: JsonRpcClientInterceptor,
): JsonRpcClient = interceptor(this)

public fun JsonRpcClient.intercepted(buildAction: JsonRpcClientInterceptor.Builder.() -> Unit): JsonRpcClient =
    intercept(JsonRpcClientInterceptor.Builder().apply(buildAction).build())
