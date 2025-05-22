package io.genkt.mcp.client

import io.genkt.jsonprc.client.JsonRpcClient
import io.genkt.jsonprc.client.sendNotification
import io.genkt.jsonprc.client.sendRequest
import io.genkt.jsonrpc.*
import io.genkt.jsonrpc.server.JsonRpcServer
import io.genkt.mcp.common.McpMethodNotFoundException
import io.genkt.mcp.common.McpParamParseException
import io.genkt.mcp.common.dto.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException

// This constructor may be unsafe
internal class McpClientImpl(
    override val info: McpInit.Implementation,
    override val capabilities: McpInit.ClientCapabilities,
    override val onRoot: suspend (McpRoot.ListRequest) -> McpRoot.ListResponse,
    override val onSampling: suspend (McpSampling.CreateMessageRequest) -> McpSampling.CreateMessageResult,
    override val onNotification: suspend (McpServerNotification) -> Unit,
    override val transport: JsonRpcTransport,
    override val requestIdGenerator: () -> RequestId = JsonRpc.NumberIdGenerator(),
    override val progressTokenGenerator: () -> McpProgress.Token = McpProgress.defaultStringTokenGenerator,
    override val errorHandler: suspend CoroutineScope.(Throwable) -> Unit = {},
    additionalContext: CoroutineContext = EmptyCoroutineContext,
) : McpClient {
    val onGoingProgressMutex = Mutex()
    private val ongoingProgress = mutableMapOf<McpProgress.Token, SendChannel<McpProgress.Notification>>()
    override val coroutineScope = transport.coroutineScope.newChild(additionalContext)
    private val transportPair = transport.shareAsClientAndServerIn()

    override val jsonRpcServer = JsonRpcServer(
        transportPair.second,
        { jsonRpcRequest ->
            val paramJsonObject = jsonRpcRequest.params as? JsonObject
                ?: return@JsonRpcServer JsonRpcFailResponse(
                    id = jsonRpcRequest.id,
                    error = JsonRpcFailResponse.Error(
                        code = JsonRpcFailResponse.Error.Code.ParseError,
                        message = "Invalid params for method ${jsonRpcRequest.method}: params must be a JsonObject",
                    )
                )
            val paramDeserializer = McpServerRequest.deserializer(jsonRpcRequest.method)
                ?: return@JsonRpcServer JsonRpcFailResponse(
                    id = jsonRpcRequest.id,
                    error = JsonRpcFailResponse.Error(
                        code = JsonRpcFailResponse.Error.Code.MethodNotFound,
                        message = "Requested method ${jsonRpcRequest.method} is not supported",
                    )
                )
            val mcpRequest = try {
                JsonRpc.json.decodeFromJsonElement(paramDeserializer, paramJsonObject)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                return@JsonRpcServer JsonRpcFailResponse(
                    id = jsonRpcRequest.id,
                    error = JsonRpcFailResponse.Error(
                        code = JsonRpcFailResponse.Error.Code.ParseError,
                        message = "Fail to parse as ${paramDeserializer.descriptor.serialName}: ${e.message}",
                    )
                )
            }
            val mcpResultJson = try {
                when (mcpRequest) {
                    is McpRoot.ListRequest -> JsonRpc.json.encodeToJsonElement(
                        McpRoot.ListResponse.serializer(),
                        onRoot(mcpRequest)
                    )

                    is McpSampling.CreateMessageRequest ->
                        JsonRpc.json.encodeToJsonElement(
                            McpSampling.CreateMessageResult.serializer(),
                            onSampling(mcpRequest)
                        )

                    McpUtilities.Ping ->
                        JsonRpc.json.encodeToJsonElement(
                            McpUtilities.Pong.serializer(),
                            McpUtilities.Pong
                        )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                return@JsonRpcServer JsonRpcFailResponse(
                    id = jsonRpcRequest.id,
                    error = JsonRpcFailResponse.Error(
                        code = JsonRpcFailResponse.Error.Code.InternalError,
                        message = e.stackTraceToString(),
                    )
                )
            }
            JsonRpcSuccessResponse(
                id = jsonRpcRequest.id,
                result = mcpResultJson,
            )
        },
        { jsonRpcNotification ->
            val deserializer = McpServerNotification.deserializer(jsonRpcNotification.method)
                ?: run {
                    coroutineScope.errorHandler(McpMethodNotFoundException(jsonRpcNotification))
                    return@JsonRpcServer
                }
            val paramsJsonElement = jsonRpcNotification.params as? JsonObject
                ?: throw IllegalArgumentException("Missing params for notification method ${jsonRpcNotification.method}. Cannot deserialize from null.")
            val deserializedNotification = try {
                JsonRpc.json.decodeFromJsonElement(deserializer, paramsJsonElement)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                coroutineScope.errorHandler(McpParamParseException(jsonRpcNotification, e.message ?: e.toString()))
                return@JsonRpcServer
            }
            try {
                onNotification(deserializedNotification)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                coroutineScope.errorHandler(e)
                return@JsonRpcServer
            }
            if (deserializedNotification is McpProgress.Notification) {
                onGoingProgressMutex.withLock {
                    ongoingProgress[deserializedNotification.progressToken]?.send(
                        deserializedNotification
                    )
                }
            }
        }
    )
    override val jsonRpcClient = JsonRpcClient(transportPair.first)

    @Suppress("UNCHECKED_CAST")
    override suspend fun <R> call(mcpCall: McpClientCall<R>): R {
        return when (mcpCall) {
            is McpClientRequest<*> -> {
                val jsonRpcResponse = jsonRpcClient.sendRequest(
                    id = requestIdGenerator(),
                    method = mcpCall.method,
                    params = JsonRpc.json.encodeToJsonElement(McpClientRequest.serializer(), mcpCall)
                )
                JsonRpc.json.decodeFromJsonElement(mcpCall.resultDeserializer, jsonRpcResponse.result)
            }

            is McpClientNotification -> {
                jsonRpcClient.sendNotification(
                    method = mcpCall.method,
                    params = JsonRpc.json.encodeToJsonElement(McpClientNotification.serializer(), mcpCall),
                )
            }

            is McpProgress.ClientRequest<*, *> -> {
                try {
                    onGoingProgressMutex.withLock {
                        ongoingProgress[mcpCall.rawRequest.token] = mcpCall.progressChannel
                    }
                    call(mcpCall.rawRequest)
                } finally {
                    onGoingProgressMutex.withLock {
                        ongoingProgress.remove(mcpCall.rawRequest.token)
                    }
                    mcpCall.progressChannel.close()
                }
            }

            is McpProgress.RawClientRequest<*, *> -> {
                val jsonRpcResponse = jsonRpcClient.sendRequest(
                    id = requestIdGenerator(),
                    method = mcpCall.method,
                    params = JsonRpc.json.encodeToJsonElement(
                        McpProgress.RawClientRequest.serializer(
                            Unit.serializer(),
                            McpClientRequest.serializer() as KSerializer<McpClientRequest<*>>,
                        ) as SerializationStrategy<McpProgress.RawClientRequest<*, *>>,
                        mcpCall
                    )
                )
                JsonRpc.json.decodeFromJsonElement(mcpCall.request.resultDeserializer, jsonRpcResponse.result)
            }
        } as R
    }

    override suspend fun start() {
        transport.start()
        jsonRpcServer.start()
        jsonRpcClient.start()
        TODO()
    }

    override suspend fun close() {
        transport.close()
        jsonRpcServer.close()
        jsonRpcClient.close()
        coroutineScope.cancel()
    }
}

private fun CoroutineScope.newChild(coroutineContext: CoroutineContext): CoroutineScope =
    CoroutineScope(this.coroutineContext + coroutineContext + Job(this.coroutineContext[Job]))