package io.genkt.jsonrpc.server

import io.genkt.jsonrpc.*
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Represents a JSON-RPC server capable of receiving requests and notifications from clients.
 * It handles the underlying transport, message deserialization/serialization, and dispatches
 * calls to appropriate handlers.
 * The server must be started using the [start] method to begin processing messages.
 * It is [AutoCloseable] and should be closed when no longer needed to release resources.
 */
public interface JsonRpcServer : AutoCloseable {
    /** The [JsonRpcServerTransport] used by this server for communication. */
    public val transport: JsonRpcServerTransport
    /**
     * A handler for uncaught exceptions that occur within the server's [coroutineScope]
     * or during request/notification processing if not handled by specific interceptors.
     * Defaults to an empty handler.
     */
    public val uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit
    /** The [CoroutineScope] used by this server for its operations, including message handling. */
    public val coroutineScope: CoroutineScope

    /**
     * Starts the server, initializing the transport and beginning to listen for incoming client messages.
     * This method must be called before the server can process any requests or notifications.
     */
    public fun start()

    /**
     * A builder class for configuring and creating [JsonRpcServer] instances.
     * Provides a DSL for setting up the transport, request/notification handlers,
     * error handlers, interceptors, and coroutine context.
     */
    public class Builder : GenericInterceptorScope {
        /**
         * The [JsonRpcServerTransport] to be used by the server.
         * Defaults to a transport that throws an error if not initialized.
         * This **must** be set for the server to function.
         */
        public var transport: JsonRpcServerTransport =
            Transport.ThrowingException { error("Using an uninitialized transport") }
        /**
         * A handler for uncaught exceptions that occur within the server's coroutine scope.
         * Defaults to an empty handler.
         */
        public var uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit = {}
        /**
         * Additional [CoroutineContext] elements to be added to the server's [coroutineScope].
         * Defaults to [EmptyCoroutineContext].
         */
        public var additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext
        /**
         * The primary handler for incoming [JsonRpcRequest] messages.
         * This suspend function takes a [JsonRpcRequest] and is expected to return a [JsonRpcServerSingleMessage]
         * (either [JsonRpcSuccessResponse] or [JsonRpcFailResponse]).
         * Defaults to a handler that throws [NotImplementedError]. This **must** be set for request handling.
         */
        public var onRequest: suspend (JsonRpcRequest) -> JsonRpcServerSingleMessage = {
            throw NotImplementedError("Server feature not implemented.")
        }
        /**
         * The primary handler for incoming [JsonRpcNotification] messages.
         * This suspend function takes a [JsonRpcNotification]. Notifications do not send responses.
         * Defaults to a handler that throws [NotImplementedError]. This **must** be set if notifications are expected.
         */
        public var onNotification: suspend (JsonRpcNotification) -> Unit = {
            throw NotImplementedError("Server feature not implemented.")
        }
        /**
         * An [Interceptor] for modifying the behavior of the [onRequest] handler.
         * Allows pre-processing of requests or post-processing of responses.
         * Defaults to an identity interceptor (no modification).
         */
        public var requestInterceptor: Interceptor<suspend (JsonRpcRequest) -> JsonRpcServerSingleMessage> = { it }
        /**
         * An [Interceptor] for modifying the behavior of the [onNotification] handler.
         * Allows pre-processing of notifications.
         * Defaults to an identity interceptor (no modification).
         */
        public var notificationInterceptor: Interceptor<suspend (JsonRpcNotification) -> Unit> = { it }
    }
}