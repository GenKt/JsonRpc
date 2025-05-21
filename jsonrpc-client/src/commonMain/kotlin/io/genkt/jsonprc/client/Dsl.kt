package io.genkt.jsonprc.client

import io.genkt.jsonrpc.*
import kotlinx.coroutines.CoroutineScope
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

public fun JsonRpcClient(
    transport: JsonRpcClientTransport,
    uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit = {},
    callInterceptor: Interceptor<suspend (JsonRpcClientCall<*>) -> Any?> = { it },
    additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext,
): JsonRpcClient = JsonRpcClientImpl(
    transport,
    uncaughtErrorHandler,
    callInterceptor,
    additionalCoroutineContext,
)

@OptIn(ExperimentalContracts::class)
public inline fun JsonRpcClient(buildAction: JsonRpcClient.Builder.() -> Unit): JsonRpcClient {
    contract {
        callsInPlace(buildAction, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return JsonRpcClient.Builder().apply(buildAction).build()
}

public fun JsonRpcClient.Builder.build(): JsonRpcClient = JsonRpcClient(
    transport = transport,
    uncaughtErrorHandler = uncaughtErrorHandler,
    callInterceptor = callInterceptor,
    additionalCoroutineContext = additionalCoroutineContext,
)

@Suppress("unchecked_cast")
public fun JsonRpcClient.Builder.interceptRequest(intercept: Interceptor<suspend (JsonRpcRequest) -> JsonRpcSuccessResponse>) {
    callInterceptor += interceptor@{ callHandler ->
        val intercepted = intercept(callHandler as suspend (JsonRpcRequest) -> JsonRpcSuccessResponse)
        return@interceptor { call ->
            when (call) {
                is JsonRpcNotification -> callHandler(call)
                is JsonRpcRequest -> intercepted(call)
            }
        }
    }
}

@Suppress("unchecked_cast")
public fun JsonRpcClient.Builder.interceptNotification(intercept: Interceptor<suspend (JsonRpcNotification) -> Unit>) {
    callInterceptor += interceptor@{ callHandler ->
        val intercepted = intercept(callHandler as suspend (JsonRpcNotification) -> Unit)
        return@interceptor { call ->
            when (call) {
                is JsonRpcNotification -> intercepted(call)
                is JsonRpcRequest -> callHandler(call)
            }
        }
    }
}

public fun JsonRpcClient.Builder.requestTimeOut(timeOut: Duration) {
    interceptRequest(TimeOut(timeOut))
}
