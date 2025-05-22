package io.genkt.jsonprc.client

import io.genkt.jsonrpc.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Represents a JSON-RPC client capable of sending requests and notifications to a server.
 * It handles the underlying transport and message serialization/deserialization.
 * The client must be started using the [start] method before sending messages.
 * It is [AutoCloseable] and should be closed when no longer needed to release resources.
 * Otherwise, the [coroutineScope] leaks.
 */
public interface JsonRpcClient : AutoCloseable {
    /**
     * The [JsonRpcClientTransport] used by this client for communication.
     */
    public val transport: JsonRpcClientTransport
    /**
     * A handler for uncaught exceptions which are not handled by [execute] calls.
     */
    public val uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit
    /**
     * The [CoroutineScope] used by this client for its operations.
     */
    public val coroutineScope: CoroutineScope

    /**
     * Starts the client, start the transport, begin to listen for incoming messages.
     * This method must be called before any requests or notifications can be sent.
     * Otherwise, [execute] blocks.
     */
    public suspend fun start()

    /**
     * Executes a JSON-RPC client call.
     * This is a generic method to handle both requests and notifications.
     *
     * @param R The expected result type for the call.
     * @param call The [JsonRpcClientCall] to execute (either a [JsonRpcRequest] or [JsonRpcNotification]).
     * @return The result of the call. For [JsonRpcRequest], it's the corresponding [JsonRpcSuccessResponse]. For [JsonRpcNotification], it's [Unit].
     */
    public suspend fun <R> execute(call: JsonRpcClientCall<R>): R

    /**
     * A builder class for configuring [JsonRpcClient].
     *
     * The receiver for the DSL.
     */
    public class Builder: GenericInterceptorScope {
        /**
         * The [JsonRpcClientTransport] to be used by the client.
         * Defaults to transport that throws an error if not initialized.
         */
        public var transport: JsonRpcClientTransport = Transport.ThrowingException { error("Using an uninitialized transport") }
        /**
         * A handler for uncaught exceptions which are not handled by [execute] calls.
         * Defaults to an empty handler.
         */
        public var uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit = {}
        /**
         * An [Interceptor] for modifying the behavior of client calls (both requests and notifications).
         * Defaults to an identity interceptor (no modification).
         */
        public var callInterceptor: Interceptor<suspend (JsonRpcClientCall<*>) -> Any?> = { it }
        /**
         * Additional [CoroutineContext] elements to be added to the client's [coroutineScope].
         * Defaults to [EmptyCoroutineContext].
         */
        public var additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext
    }
}

/**
 * Sends a JSON-RPC request to the server.
 *
 * @receiver The [JsonRpcClient] instance.
 * @param id The [RequestId] for this request.
 * @param method The name of the method to be invoked on the server.
 * @param params The parameters for the method, as a [JsonElement]. Defaults to `null`.
 * @param jsonrpc The JSON-RPC version string. Defaults to [JsonRpc.VERSION].
 * @return The [JsonRpcSuccessResponse] from the server.
 * @throws JsonRpcResponseException if the server returns an error response.
 * @throws kotlinx.coroutines.TimeoutCancellationException if a timeout occurs.
 */
public suspend fun JsonRpcClient.sendRequest(
    id: RequestId,
    method: String,
    params: JsonElement? = null,
    jsonrpc: String = JsonRpc.VERSION,
): JsonRpcSuccessResponse = execute(
    JsonRpcRequest(
        id = id,
        method = method,
        params = params ?: JsonNull,
        jsonrpc = jsonrpc,
    )
)

/**
 * Sends a JSON-RPC notification to the server.
 * Notifications do not receive responses.
 *
 * @receiver The [JsonRpcClient] instance.
 * @param method The name of the method to be invoked on the server.
 * @param params The parameters for the method, as a [JsonElement]. Defaults to `null`.
 * @param jsonrpc The JSON-RPC version string. Defaults to [JsonRpc.VERSION].
 */
public suspend fun JsonRpcClient.sendNotification(
    method: String,
    params: JsonElement? = null,
    jsonrpc: String = JsonRpc.VERSION,
): Unit = execute(
    JsonRpcNotification(
        method = method,
        params = params ?: JsonNull,
        jsonrpc = jsonrpc,
    )
)