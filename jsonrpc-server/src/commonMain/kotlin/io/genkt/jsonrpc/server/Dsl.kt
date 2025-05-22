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
 * Creates a [JsonRpcServer] with the specified transport, request handler, notification handler, error handler, and coroutine context.
 *
 * @param transport The [JsonRpcServerTransport] to use for communication.
 * @param onRequest A suspend function to handle incoming [JsonRpcRequest] messages and return a [JsonRpcServerMessage] response.
 * @param onNotification A suspend function to handle incoming [JsonRpcNotification] messages.
 * @param errorHandler A suspend function to handle uncaught errors from the server's coroutine scope. Defaults to an empty handler.
 * @param additionalCoroutineContext Additional [CoroutineContext] elements to combine with the server's scope. Defaults to [EmptyCoroutineContext].
 * @return A new [JsonRpcServer] instance.
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
 * Creates a [JsonRpcServer] using the DSL builder pattern.
 *
 * @param buildAction A lambda function to configure the [JsonRpcServer.Builder].
 * @return A new [JsonRpcServer] instance.
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
 * This applies any configured interceptors to the request and notification handlers.
 *
 * @receiver The [JsonRpcServer.Builder] instance.
 * @return A new [JsonRpcServer] instance.
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
 * Adds a request timeout interceptor to the [JsonRpcServer.Builder].
 * This will apply a timeout to the processing of all incoming [JsonRpcRequest]s.
 *
 * @receiver The [JsonRpcServer.Builder] instance.
 * @param timeout The [Duration] for the timeout.
 */
public fun JsonRpcServer.Builder.requestTimeout(timeout: Duration) {
    requestInterceptor += TimeOut(timeout)
}