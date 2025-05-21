@file:OptIn(McpClientInterceptionApi::class)

package io.genkt.mcp.client

import io.genkt.jsonprc.client.JsonRpcClient
import io.genkt.jsonrpc.*
import io.genkt.jsonrpc.server.JsonRpcServer
import io.genkt.mcp.common.dto.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.Channel 
import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.serialization.KSerializer // For paramsSerializer
import kotlinx.serialization.SerializationStrategy 
import kotlinx.serialization.builtins.serializer // Required for Unit.serializer()
import kotlinx.serialization.json.JsonElement 
import kotlinx.serialization.json.JsonObject 
import kotlinx.serialization.json.JsonPrimitive 

// Custom Exception for McpClientImpl.call
public class McpCallException(
    message: String, 
    public val error: JsonRpcError? = null, 
    cause: Throwable? = null
) : RuntimeException(message, cause)

internal class McpClientImpl(
    override val info: McpInit.Implementation,
    override val capabilities: McpInit.ClientCapabilities,
    override val onRoot: suspend (McpRoot.ListRequest) -> McpRoot.ListResponse,
    override val onSampling: suspend (McpSampling.CreateMessageRequest) -> McpSampling.CreateMessageResult,
    override val onNotification: suspend (McpServerNotification) -> Unit,
    override val transport: JsonRpcTransport,
    override val requestIdGenerator: () -> RequestId = JsonRpc.NumberIdGenerator(),
    override val progressTokenGenerator: () -> String = McpProgress.defaultTokenGenerator,
    additionalContext: CoroutineContext = EmptyCoroutineContext,
) : McpClient {
    val mutex = Mutex()
    private val ongoingProgress = mutableMapOf<RequestId, SendChannel<McpProgress.Notification>>()
    override val coroutineScope = transport.coroutineScope.newChild(additionalContext)
    private val transportPair = transport.shareAsClientAndServerIn()
    
    override val jsonRpcServer = JsonRpcServer(
        transportPair.second,
        { request -> 
            try { 
                when (request.method) {
                    McpMethods.Roots.List -> {
                        val params = request.params?.let { paramsJson ->
                            JsonRpc.decodeFromJsonElement(McpRoot.ListRequest.serializer(), paramsJson)
                        } ?: throw IllegalArgumentException("Missing params for method ${request.method}") 
                        val result = onRoot(params)
                        JsonRpcResult(request.id, JsonRpc.encodeToJsonElement(McpRoot.ListResponse.serializer(), result))
                    }
                    McpMethods.Sampling.CreateMessage -> {
                        val params = request.params?.let { paramsJson ->
                            JsonRpc.decodeFromJsonElement(McpSampling.CreateMessageRequest.serializer(), paramsJson)
                        } ?: throw IllegalArgumentException("Missing params for method ${request.method}") 
                        val result = onSampling(params)
                        JsonRpcResult(request.id, JsonRpc.encodeToJsonElement(McpSampling.CreateMessageResult.serializer(), result))
                    }
                    // Example of how server-sent progress *requests* might be handled if they arrive here
                    // This is highly dependent on the specific JSON-RPC server implementation for progress
                    // This case assumes the server sends a request to the client for progress, which is unusual.
                    // Typically, progress is a server-to-client notification ($/progress).
                    McpMethods.Notifications.Progress -> { // Assuming server sends progress as a request to client.
                        val progressNotification = request.params?.let { JsonRpc.decodeFromJsonElement(McpProgress.Notification.serializer(), it) }
                            ?: throw IllegalArgumentException("Missing params for progress update request")
                        
                        // Attempt to route progress. This part is complex as RequestId is needed.
                        // This assumes a mechanism where the progress notification's token or some other field
                        // can be used to find the original RequestId or the channel directly.
                        // The current `ongoingProgress` map is keyed by `RequestId`.
                        // For simplicity, if a channel is found associated with this request's ID (unlikely for server-sent progress), use it.
                        val targetChannel = ongoingProgress[request.id] 
                        if (targetChannel != null) {
                            if (!targetChannel.isClosedForSend) targetChannel.trySend(progressNotification) else println("[McpClientImpl] Progress channel for request ID ${request.id} is closed.")
                        } else {
                             // If not found by ID, this indicates a potential issue or that progress is handled differently
                             println("[McpClientImpl] Received progress update as a REQUEST for id ${request.id}, but no specific channel was registered.")
                        }
                        // Acknowledge the request if it's a request-response style progress update
                        JsonRpcResult(request.id, JsonPrimitive("Progress update request acknowledged by client")) 
                    }
                    else -> throw JsonRpcException.methodNotFound(request.method)
                }
            } catch (e: kotlinx.serialization.SerializationException) {
                throw JsonRpcException.invalidParams("Invalid params for method ${request.method}: ${e.message}", cause = e)
            } catch (e: IllegalArgumentException) {
                throw JsonRpcException.invalidParams("Invalid params for method ${request.method}: ${e.message}", cause = e)
            } catch (e: JsonRpcException) {
                throw e
            } catch (e: Exception) {
                throw JsonRpcException.internalError("Internal error processing method ${request.method}: ${e.message}", cause = e)
            }
        }, 
        { notification -> // Server-to-client notifications (e.g. $/progress)
            try {
                val deserializer: kotlinx.serialization.DeserializationStrategy<McpServerNotification> = McpServerNotification.deserializer(notification.method)
                val paramsJsonElement = notification.params
                    ?: throw IllegalArgumentException("Missing params for notification method ${notification.method}. Cannot deserialize from null.")
                val deserializedNotification: McpServerNotification = JsonRpc.decodeFromJsonElement(deserializer, paramsJsonElement)
                
                if (deserializedNotification is McpProgress.Notification) {
                    // This is where progress notifications from the server (sent as JSON-RPC notifications, e.g. $/progress) are handled.
                    // The `ongoingProgress` map is keyed by `RequestId`.
                    // The server's progress notification MUST include the original `RequestId` for this routing to work.
                    // We assume McpProgress.Notification DTO might contain the original RequestId or a token that can be mapped to it.
                    
                    var targetChannel: SendChannel<McpProgress.Notification>? = null
                    val tokenFromNotification = deserializedNotification.token

                    // Attempt to find the channel. This requires a robust way to map token to RequestId,
                    // or for McpProgress.Notification to carry the RequestId.
                    // Simplified approach: Iterate and check if any RequestId string/number matches the token.
                    // This is NOT robust if tokens are not identical to RequestId string/number forms.
                    val foundEntry = ongoingProgress.entries.find { (reqId, _) ->
                        when(reqId) {
                            is RequestId.StringId -> reqId.id == tokenFromNotification
                            is RequestId.NumberId -> reqId.id.toString() == tokenFromNotification
                        }
                    }
                    targetChannel = foundEntry?.value

                    if (targetChannel != null) {
                        if (!targetChannel.isClosedForSend) {
                            val sendResult = targetChannel.trySend(deserializedNotification) 
                            if (sendResult.isSuccess) {
                                // println("[McpClientImpl] Routed progress for token (matched to RequestId): ${deserializedNotification.token}")
                            } else {
                                println("[McpClientImpl] Failed to send progress for token ${deserializedNotification.token}, channel full or closed. Error: ${sendResult.exceptionOrNull()?.message}")
                            }
                        } else {
                            // println("[McpClientImpl] Channel for progress token ${deserializedNotification.token} is closed.")
                            // If channel is closed by client (e.g. in finally block), this is expected.
                        }
                    } else {
                         println("[McpClientImpl] No ongoing progress channel found for token: ${deserializedNotification.token} via direct token match to RequestId in notification handler.")
                    }
                } else {
                    this@McpClientImpl.onNotification(deserializedNotification)
                }
            } catch (e: kotlinx.serialization.SerializationException) {
                println("[McpClientImpl] Deserialization error for notification (method: ${notification.method}). Details: ${e.message}")
            } catch (e: IllegalArgumentException) {
                println("[McpClientImpl] Processing error for notification (method: ${notification.method}). Details: ${e.message}")
            } catch (e: Exception) {
                println("[McpClientImpl] Unexpected error handling notification (method: ${notification.method}). Details: ${e.message}")
            }
        }
    )
    override val jsonRpcClient = JsonRpcClient(transportPair.first)

    @Suppress("UNCHECKED_CAST")
    override suspend fun <R> call(mcpCall: McpClientCall<R>): R {
        return when (mcpCall) {
            is McpClientProgressRequest<*, *> -> {
                val progressRequest = mcpCall as McpClientProgressRequest<R, McpProgress.Notification>
                
                val token = progressRequest.progressToken ?: progressTokenGenerator()
                val requestId = requestIdGenerator() 
                
                var manageChannelLocally = false
                // The type P_PROGRESS is McpProgress.Notification as per prompt.
                val progressActualReceiver: SendChannel<McpProgress.Notification> = progressRequest.progressReceiver 
                    ?: Channel<McpProgress.Notification>(Channel.UNLIMITED).also {
                        ongoingProgress[requestId] = it 
                        manageChannelLocally = true
                    }

                try {
                    var serializedParamsAsElement: JsonElement = JsonRpc.encodeToJsonElement(
                        progressRequest.paramsSerializer as KSerializer<Any?>, // Use the paramsSerializer from the DTO
                        progressRequest.params 
                    )

                    // Inject _progressToken if the serialized form is a JsonObject
                    if (serializedParamsAsElement is JsonObject) {
                        val newMap = serializedParamsAsElement.toMutableMap()
                        newMap["_progressToken"] = JsonPrimitive(token)
                        serializedParamsAsElement = JsonObject(newMap)
                    } else {
                         println("[McpClientImpl] Warning: Could not inject _progressToken for method ${progressRequest.method}. Main params did not serialize to a JsonObject.")
                    }
                    
                    val rpcRequest = JsonRpcRequest(
                        id = requestId,
                        method = progressRequest.method,
                        params = serializedParamsAsElement
                    )
                    
                    val response = jsonRpcClient.call(rpcRequest)

                    response.error?.let {
                        throw McpCallException("Server error for progress request ${progressRequest.method}: ${it.message}", it)
                    }
                    
                    val resultJson = response.result ?: run {
                        // For Unit return type, null result is acceptable.
                        if (progressRequest.resultSerializer.descriptor == Unit.serializer().descriptor) {
                            return Unit as R
                        }
                        throw McpCallException("Server returned no result for progress request ${progressRequest.method}", null)
                    }
                    
                    JsonRpc.decodeFromJsonElement(progressRequest.resultSerializer, resultJson)
                } catch (e: kotlinx.serialization.SerializationException) {
                    throw McpCallException("Serialization/deserialization error for progress request ${progressRequest.method}: ${e.message}", null, e)
                } catch (e: Exception) {
                    throw McpCallException("Error in progress request ${progressRequest.method}: ${e.message}", null, e)
                } finally {
                    if (manageChannelLocally) { 
                        ongoingProgress.remove(requestId)?.close()
                    }
                }
            }
            is McpClientNotification -> {
                // R must be Unit for notifications.
                // The cast `Unit as R` will throw ClassCastException if R is not Unit.
                 if (R::class != Unit::class && R::class.java != Void.TYPE) { // Attempt a more robust check
                     throw McpCallException("McpClientNotification must be called with McpClientCall<Unit>. Call is for <${R::class.simpleName}>", null)
                 }
                
                val serializedParams = JsonRpc.encodeToJsonElement(
                     mcpCall.paramsSerializer as KSerializer<Any?>, 
                     mcpCall.params
                )
                
                jsonRpcClient.notify(
                    JsonRpcNotification(
                        method = mcpCall.method,
                        params = serializedParams
                    )
                )
                Unit as R 
            }
            is McpClientRequest<R> -> { 
                val serializedParams = JsonRpc.encodeToJsonElement(
                    mcpCall.paramsSerializer as KSerializer<Any?>, 
                    mcpCall.params
                )
                val rpcRequest = JsonRpcRequest(
                    id = requestIdGenerator(),
                    method = mcpCall.method,
                    params = serializedParams
                )
                val response = jsonRpcClient.call(rpcRequest)

                response.error?.let {
                    throw McpCallException("Server error for request ${mcpCall.method}: ${it.message}", it)
                }
                val resultJson = response.result ?: run {
                     if (mcpCall.resultSerializer.descriptor == Unit.serializer().descriptor) {
                         return Unit as R
                     }
                     throw McpCallException("Server returned no result for request ${mcpCall.method}", null)
                }
                
                try {
                    JsonRpc.decodeFromJsonElement(mcpCall.resultSerializer, resultJson)
                } catch (e: kotlinx.serialization.SerializationException) {
                    throw McpCallException("Failed to deserialize result for method ${mcpCall.method}: ${e.message}", null, e)
                }
            }
            else -> {
                throw McpCallException("Unsupported McpClientCall type: ${mcpCall::class.simpleName}", null)
            }
        }
    }

    override suspend fun start() {
        jsonRpcServer.start()
    }

    override suspend fun close() {
        coroutineScope.cancel()
        transport.close()
    }
}

internal class InterceptedMcpClient(
    private val delegate: McpClient,
    private val interceptor: Interceptor<McpClient>,
) : McpClient {
    // Properties delegation
    override val info: McpInit.Implementation get() = delegate.info
    override val capabilities: McpInit.ClientCapabilities get() = delegate.capabilities
    override val onRoot: suspend (McpRoot.ListRequest) -> McpRoot.ListResponse get() = delegate.onRoot
    override val onSampling: suspend (McpSampling.CreateMessageRequest) -> McpSampling.CreateMessageResult get() = delegate.onSampling
    override val onNotification: suspend (McpServerNotification) -> Unit get() = delegate.onNotification
    override val transport: JsonRpcTransport get() = delegate.transport
    override val requestIdGenerator: () -> RequestId get() = delegate.requestIdGenerator
    override val progressTokenGenerator: () -> String get() = delegate.progressTokenGenerator
    override val coroutineScope: CoroutineScope get() = delegate.coroutineScope

    @McpClientInterceptionApi
    override val jsonRpcServer: JsonRpcServer get() = delegate.jsonRpcServer
    
    @McpClientInterceptionApi
    override val jsonRpcClient: JsonRpcClient get() = delegate.jsonRpcClient

    override suspend fun <R> call(mcpCall: McpClientCall<R>): R {
        val clientInterceptor = interceptor as? McpClientInterceptor
            ?: throw IllegalStateException("Interceptor is not an McpClientInterceptor. Actual type: ${interceptor::class.simpleName}")

        return clientInterceptor.interceptActualCall(delegate, mcpCall)
    }

    override suspend fun start() {
        delegate.start()
    }

    override suspend fun close() {
        delegate.close()
    }
}

private fun CoroutineScope.newChild(coroutineContext: CoroutineContext): CoroutineScope =
    CoroutineScope(this.coroutineContext + coroutineContext + Job(this.coroutineContext[Job]))