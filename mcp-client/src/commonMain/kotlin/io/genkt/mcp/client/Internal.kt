package io.genkt.mcp.client

import io.genkt.jsonprc.client.JsonRpcClient
import io.genkt.jsonrpc.*
import io.genkt.jsonrpc.server.JsonRpcServer
import io.genkt.mcp.common.McpMethodNotFoundException
import io.genkt.mcp.common.McpParamParseException
import io.genkt.mcp.common.dto.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException

internal class McpClientImpl(
    override val info: McpInit.Implementation,
    override val capabilities: McpInit.ClientCapabilities,
    val onRoot: suspend (McpRoot.ListRequest) -> McpRoot.ListResponse,
    val onSampling: suspend (McpSampling.CreateMessageRequest) -> McpSampling.CreateMessageResult,
    val onNotification: suspend (McpServerNotification<McpServerBasicNotification>) -> Unit,
    override val transport: JsonRpcTransport,
    val requestIdGenerator: () -> RequestId = JsonRpc.NumberIdGenerator(),
    val progressTokenGenerator: () -> McpProgress.Token = McpProgress.defaultStringTokenGenerator,
    override val uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit = {},
    val callInterceptor: Interceptor<suspend (McpClientCall<*>) -> Any?> = { it },
    additionalContext: CoroutineContext = EmptyCoroutineContext,
) : McpClient {
    override val coroutineScope = transport.coroutineScope.newChild(additionalContext)
    private val onGoingProgressMutex = Mutex()
    private val ongoingProgress = mutableMapOf<McpProgress.Token, SendChannel<McpProgress.Notification>>()
    private val transportPair = transport.shareAsClientAndServerIn()
    private val jsonRpcServer = JsonRpcServer(
        transport = transportPair.second,
        onRequest = ::handleJsonRpcRequest,
        onNotification = ::handleJsonRpcNotification,
    )
    private val jsonRpcClient = JsonRpcClient(transportPair.first)

    private suspend fun handleJsonRpcRequest(request: JsonRpcRequest): JsonRpcServerSingleMessage {
        val paramJsonObject = request.params as? JsonObject
        val paramSerializer = McpServerRawRequest.serializerOf(request.method)
            ?: return jsonRpcMethodNotSupportedResponse(request)
        val mcpRequest = try {
            JsonRpc.json.decodeFromJsonElement(paramSerializer, paramJsonObject ?: JsonNull)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return jsonRpcParseErrorResponse(request, paramSerializer, e)
        }
        TODO()
    }

    private suspend fun handleJsonRpcNotification(notification: JsonRpcNotification) {
        try {
            val deserializer = McpServerRawNotification.serializerOf(notification.method)
                ?: throw McpMethodNotFoundException(notification)
            val paramsJsonElement = notification.params as? JsonObject
                ?: throw McpParamParseException(
                    notification,
                    IllegalArgumentException("Param should be JsonObject.")
                )
            val mcpServerRawNotification = try {
                JsonRpc.json.decodeFromJsonElement(deserializer, paramsJsonElement)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                throw McpParamParseException(notification, e)
            }
            onNotification(McpServerNotification(mcpServerRawNotification.notification, null))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            coroutineScope.uncaughtErrorHandler(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <R> call(mcpCall: McpClientCall<R>): R {
        TODO()
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

private fun jsonRpcParseErrorResponse(
    jsonRpcRequest: JsonRpcRequest,
    paramDeserializer: KSerializer<*>,
    e: Throwable
): JsonRpcFailResponse = JsonRpcFailResponse(
    id = jsonRpcRequest.id,
    error = JsonRpcFailResponse.Error(
        code = JsonRpcFailResponse.Error.Code.ParseError,
        message = "Fail to parse as ${paramDeserializer.descriptor.serialName}: ${e.message}",
    )
)

private fun jsonRpcMethodNotSupportedResponse(jsonRpcRequest: JsonRpcRequest): JsonRpcFailResponse =
    JsonRpcFailResponse(
        id = jsonRpcRequest.id,
        error = JsonRpcFailResponse.Error(
            code = JsonRpcFailResponse.Error.Code.MethodNotFound,
            message = "Requested method ${jsonRpcRequest.method} is not supported",
        )
    )

private fun pongResponse(id: RequestId): JsonRpcSuccessResponse =
    JsonRpcSuccessResponse(
        id = id,
        result = JsonRpc.json.encodeToJsonElement(McpUtilities.Pong.serializer(), McpUtilities.Pong)
    )

private fun CoroutineScope.newChild(coroutineContext: CoroutineContext): CoroutineScope =
    CoroutineScope(this.coroutineContext + coroutineContext + SupervisorJob(this.coroutineContext[Job]))