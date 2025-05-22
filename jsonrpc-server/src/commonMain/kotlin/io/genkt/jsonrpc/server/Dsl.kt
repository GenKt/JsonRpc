package io.genkt.jsonrpc.server

import io.genkt.jsonrpc.JsonRpcNotification
import io.genkt.jsonrpc.JsonRpcRequest
import io.genkt.jsonrpc.plus
import io.genkt.jsonrpc.JsonRpcServerMessage
import io.genkt.jsonrpc.JsonRpcServerTransport
import io.genkt.jsonrpc.TimeOut
import kotlinx.coroutines.CoroutineScope
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

/**
 * @param transport The [JsonRpcServerTransport] to use for communication.
 * @param onRequest Handle incoming [JsonRpcRequest] messages and return [JsonRpcServerMessage] response.
 * @param onNotification Handle incoming [JsonRpcNotification] messages.
 * @param errorHandler Handle uncaught errors that are not processed with [onRequest] or [onNotification]. Defaults to an empty handler.
 * @param additionalCoroutineContext Additional [CoroutineContext] elements to combine with the server's scope. Defaults to [EmptyCoroutineContext].
 */
public fun JsonRpcServer(
    transport: JsonRpcServerTransport,
    onRequest: suspend (JsonRpcRequest) -> JsonRpcServerMessage,
    onNotification: suspend (JsonRpcNotification) -> Unit,
    errorHandler: suspend CoroutineScope.(Throwable) -> Unit = {},
    additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext,
): JsonRpcServer = JsonRpcServerImpl(
    transport,
    onRequest,
    onNotification,
    errorHandler,
    additionalCoroutineContext
)

/**
 * @param buildAction Configure the [JsonRpcServer.Builder].
 */
@OptIn(ExperimentalContracts::class)
public fun JsonRpcServer(buildAction: JsonRpcServer.Builder.() -> Unit): JsonRpcServer {
    contract {
        callsInPlace(buildAction, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return JsonRpcServer.Builder().apply(buildAction).build()
}

/**
 * Builds a [JsonRpcServer] from the current [JsonRpcServer.Builder] configuration.
 */
public fun JsonRpcServer.Builder.build(): JsonRpcServer =
    JsonRpcServer(
        transport = transport,
        onRequest = requestInterceptor(onRequest),
        onNotification = notificationInterceptor(onNotification),
        errorHandler = uncaughtErrorHandler,
        additionalCoroutineContext = additionalCoroutineContext
    )

/**
 * Adds a request [TimeOut] interceptor to the [JsonRpcServer.Builder.requestInterceptor].
 *
 * @param timeout The [Duration] for the timeout.
 */
public fun JsonRpcServer.Builder.requestTimeout(timeout: Duration) {
    requestInterceptor += TimeOut(timeout)
}