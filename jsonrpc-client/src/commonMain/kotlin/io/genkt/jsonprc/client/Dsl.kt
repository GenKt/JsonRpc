package io.genkt.jsonprc.client

import io.genkt.jsonrpc.*
import kotlinx.coroutines.CoroutineScope
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

/**
 * @param transport The [JsonRpcClientTransport] to use for communication.
 * @param uncaughtErrorHandler A handler for uncaught exceptions which are not handled by [JsonRpcClient.execute] calls.
 * @param callInterceptor An [Interceptor] to modify the behavior of client calls.
 * @param additionalCoroutineContext Additional [CoroutineContext] elements to combine with the client's scope.
 */
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

/**
 * @param buildAction Configure the [JsonRpcClient.Builder].
 */
@OptIn(ExperimentalContracts::class)
public inline fun JsonRpcClient(buildAction: JsonRpcClient.Builder.() -> Unit): JsonRpcClient {
    contract {
        callsInPlace(buildAction, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return JsonRpcClient.Builder().apply(buildAction).build()
}

/**
 * Builds a [JsonRpcClient] from the current [JsonRpcClient.Builder] configuration.
 */
public fun JsonRpcClient.Builder.build(): JsonRpcClient = JsonRpcClient(
    transport = transport,
    uncaughtErrorHandler = uncaughtErrorHandler,
    callInterceptor = callInterceptor,
    additionalCoroutineContext = additionalCoroutineContext,
)

/**
 * Adds an interceptor for [JsonRpcRequest] calls to the [JsonRpcClient.Builder].
 *
 * @param intercept The [Interceptor] for [JsonRpcRequest] calls.
 */
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

/**
 * Adds an interceptor for [JsonRpcNotification] calls to the [JsonRpcClient.Builder].
 *
 * @param intercept The [Interceptor] for [JsonRpcNotification] calls.
 */
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

/**
 * Adds a request timeout interceptor to the [JsonRpcClient.Builder].
 * This will apply a [TimeOut] to all [JsonRpcRequest] calls made by the client.
 *
 * @param timeOut The [Duration] for the timeout.
 */
public fun JsonRpcClient.Builder.requestTimeOut(timeOut: Duration) {
    interceptRequest(TimeOut(timeOut))
}
