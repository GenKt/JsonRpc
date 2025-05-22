package io.genkt.jsonrpc.server

import io.genkt.jsonrpc.*
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Represents a JSON-RPC server capable of receiving requests and notifications from clients.
 * It handles the underlying transport, message deserialization/serialization, and dispatches
 * calls to appropriate handlers.
 *
 * The server must be started using the [start] method to begin listening on the [transport].
 * It is [AutoCloseable] and should be closed when no longer needed to release resources.
 * Otherwise, the [coroutineScope] leaks.
 */
public interface JsonRpcServer : AutoCloseable {
    /**
     * The [JsonRpcServerTransport] used by this server for communication.
     */
    public val transport: JsonRpcServerTransport
    /**
     * A handler for uncaught exceptions which are not handled by request/notification handlers.
     */
    public val uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit
    /**
     * The [CoroutineScope] used by this server for all its operations.
     * Closing it won't close the underlying [transport].
     */
    public val coroutineScope: CoroutineScope

    /**
     * Starts the transport and the server, begin to listen for incoming client messages.
     */
    public fun start()

    /**
     * A builder class for configuring [JsonRpcServer].
     *
     * The receiver for the DSL.
     */
    public class Builder : GenericInterceptorScope {
        /**
         * Will be [JsonRpcServer.transport].
         *
         * Defaults to transport that throws an error if not initialized.
         * This **must** be set for the server to function.
         */
        public var transport: JsonRpcServerTransport =
            Transport.ThrowingException { error("Using an uninitialized transport") }
        /**
         * A handler for uncaught exceptions which are not handled by request/notification handlers.
         * Defaults to an empty handler.
         */
        public var uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit = {}
        /**
         * Additional [CoroutineContext] elements to be added to the server's [coroutineScope].
         * Defaults to [EmptyCoroutineContext].
         */
        public var additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext
        /**
         * Handle incoming [JsonRpcRequest] messages and return [JsonRpcServerMessage] response.
         * Defaults to a handler that throws [NotImplementedError]. This **must** be set for request handling.
         */
        public var onRequest: suspend (JsonRpcRequest) -> JsonRpcServerSingleMessage = {
            throw NotImplementedError("Server feature not implemented.")
        }
        /**
         * Handle incoming [JsonRpcNotification] messages.
         * Defaults to a handler that throws [NotImplementedError]. This **must** be set if notifications are expected.
         */
        public var onNotification: suspend (JsonRpcNotification) -> Unit = {
            throw NotImplementedError("Server feature not implemented.")
        }
        /**
         * An [Interceptor] for modifying the behavior of the [onRequest] handler.
         * Defaults to an identity interceptor (no modification).
         */
        public var requestInterceptor: Interceptor<suspend (JsonRpcRequest) -> JsonRpcServerSingleMessage> = { it }
        /**
         * An [Interceptor] for modifying the behavior of the [onNotification] handler
         * Defaults to an identity interceptor (no modification).
         */
        public var notificationInterceptor: Interceptor<suspend (JsonRpcNotification) -> Unit> = { it }
    }
}