package io.genkt.mcp.client

import io.genkt.jsonprc.client.JsonRpcClient
import io.genkt.jsonprc.client.sendNotification
import io.genkt.jsonprc.client.sendRequest
import io.genkt.jsonrpc.*
import io.genkt.jsonrpc.server.JsonRpcServer
import io.genkt.mcp.common.McpErrorResponseException
import io.genkt.mcp.common.McpMethodNotFoundException
import io.genkt.mcp.common.McpParamParseException
import io.genkt.mcp.common.dto.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException


internal class McpClientImpl(
    override val jsonRpcTransport: JsonRpcTransport,
    val onRoot: suspend (McpServerRequest<McpRoot.ListRequest, McpRoot.ListResponse>) -> McpRoot.ListResponse,
    val onSampling: suspend (McpServerRequest<McpSampling.CreateMessageRequest, McpSampling.CreateMessageResult>) -> McpSampling.CreateMessageResult,
    val onNotification: suspend (McpServerNotification<McpServerBasicNotification>) -> Unit,
    val requestIdGenerator: () -> RequestId,
    val progressTokenGenerator: () -> McpProgress.Token,
    override val uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit,
    callInterceptor: Interceptor<suspend (McpClientCall<*>) -> Any?>,
    additionalCoroutineContext: CoroutineContext,
) : McpClient {
    override val coroutineScope = jsonRpcTransport.coroutineScope.newChild(additionalCoroutineContext)
    private val onGoingProgressMutex = Mutex()
    private val ongoingProgress = mutableMapOf<McpProgress.Token, SendChannel<McpProgress.Notification>>()
    private val transportPair = jsonRpcTransport.shareAsClientAndServerIn()
    private val jsonRpcServer = JsonRpcServer(
        transport = transportPair.second,
        onRequest = ::handleJsonRpcRequest,
        onNotification = ::handleJsonRpcNotification,
        uncaughtErrorHandler = uncaughtErrorHandler,
        additionalCoroutineContext = additionalCoroutineContext
    )
    private val jsonRpcClient = JsonRpcClient(transportPair.first)

    private suspend fun handleJsonRpcRequest(request: JsonRpcRequest): JsonRpcServerSingleMessage {
        val paramJsonObject = request.params as? JsonObject
        val paramSerializer = McpServerRawRequest.serializerOf(request.method)
            ?: return jsonRpcMethodNotSupportedResponse(request)
        val mcpRawRequest = try {
            JsonRpc.json.decodeFromJsonElement(paramSerializer, paramJsonObject ?: JsonNull)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return jsonRpcParseErrorResponse(request, paramSerializer, e)
        }
        return try {
            JsonRpcSuccessResponse(
                id = request.id,
                result = handleMcpRawRequest(mcpRawRequest)
            )
        } catch (e: McpErrorResponseException) {
            JsonRpcFailResponse(
                id = request.id,
                error = e.error,
            )
        }
    }

    private suspend fun handleMcpRawRequest(request: McpServerRawRequest<*, *>): JsonElement {
        val progressToken = request.meta?.progressToken
        val progressNotificationChannel = progressToken?.let { Channel<McpProgress.Notification>() }
        val progressChannel = progressNotificationChannel?.forwarded<McpProgress, _>(coroutineScope) {
            it.map { (progress, total, message) ->
                McpProgress.Notification(
                    progressToken = progressToken,
                    progress = progress,
                    total = total,
                    message = message,
                )
            }
        }
        val forwardingJob = progressNotificationChannel?.let { progressNotificationChannel ->
            coroutineScope.launch {
                progressNotificationChannel.consumeEach {
                    jsonRpcClient.sendNotification(
                        it.method,
                        JsonRpc.json.encodeToJsonElement(McpProgress.Notification.serializer(), it)
                    )
                }
            }
        }
        val mcpRequest = McpServerRequest(
            request.request,
            request.meta?.let { McpServerRequest.Meta(progressChannel) }
        )
        return try {
            handleMcpRequest(mcpRequest)
        } finally {
            progressChannel?.close()
            progressNotificationChannel?.close()
            forwardingJob?.cancel()
        }
    }

    @Suppress("unchecked_cast")
    private suspend fun handleMcpRequest(request: McpServerRequest<*, *>): JsonElement {
        val handler = when (request.basicRequest) {
            is McpRoot.ListRequest -> onRoot
            is McpSampling.CreateMessageRequest -> onSampling
            is McpUtilities.Ping -> handlePing
        } as suspend (McpServerRequest<*, *>) -> Any
        val resultSerializer = request.basicRequest.resultSerializer as KSerializer<Any>
        val result = handler(request)
        return JsonRpc.json.encodeToJsonElement(resultSerializer, result)
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
            val mcpServerBasicNotification = mcpServerRawNotification.notification
            if (mcpServerBasicNotification is McpProgress.Notification) {
                onGoingProgressMutex.withLock {
                    ongoingProgress[mcpServerBasicNotification.progressToken]?.send(mcpServerBasicNotification)
                }
            }
            onNotification(McpServerNotification(mcpServerBasicNotification, null))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            coroutineScope.uncaughtErrorHandler(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <R> execute(call: McpClientCall<R>): R {
        return interceptedCallHandler(call) as R
    }

    private val interceptedCallHandler = callInterceptor { handleCall(it) }

    @Suppress("unchecked_cast")
    private suspend fun <R> handleCall(mcpCall: McpClientCall<R>): R {
        return when (mcpCall) {
            is McpClientNotification<*> -> sendNotification(mcpCall as McpClientNotification<McpClientBasicNotification>)
            is McpClientBasicNotification -> sendNotification(McpClientNotification(mcpCall, null))
            is McpClientRequest<*, *> -> sendRequest(mcpCall as McpClientRequest<McpClientBasicRequest<Any>, Any>)
            is McpClientBasicRequest<*> -> sendRequest(McpClientRequest(mcpCall, null))
        } as R
    }

    private suspend fun sendNotification(notification: McpClientNotification<McpClientBasicNotification>) {
        val rawNotification = McpClientRawNotification(notification.basicNotification, null)
        jsonRpcClient.sendNotification(
            notification.method,
            JsonRpc.json.encodeToJsonElement(rawNotification.serializer(), rawNotification)
        )
    }

    private suspend fun <Request, Result> sendRequest(request: McpClientRequest<Request, Result>): Result
            where Request : McpClientBasicRequest<Result> {
        val progressChannel = request.meta?.progressChannel
        val progressToken = progressChannel?.let { progressTokenGenerator() }
        val progressNotificationChannel = progressChannel?.let { progressChannel ->
            progressChannel.forwarded(coroutineScope) { it: Flow<McpProgress.Notification> ->
                it.map { (_, progress, total, message) ->
                    McpProgress(progress, total, message)
                }
            }
        }?.also {
            onGoingProgressMutex.withLock { ongoingProgress[progressToken!!] = it }
        }
        val result = sendRawRequest(
            McpClientRawRequest(
                request.basicRequest,
                McpClientRawRequest.Meta(progressToken)
            )
        )
        progressNotificationChannel?.close()
        progressChannel?.close()
        onGoingProgressMutex.withLock { ongoingProgress.remove(progressToken) }
        return result
    }

    private suspend fun <Request, Result> sendRawRequest(request: McpClientRawRequest<Request, Result>): Result
            where Request : McpClientBasicRequest<Result> {
        val resultDeserializer = request.request.resultSerializer
        return JsonRpc.json.decodeFromJsonElement(
            resultDeserializer,
            jsonRpcClient.sendRequest(
                requestIdGenerator(),
                request.request.method,
                JsonRpc.json.encodeToJsonElement(request.serializer(), request)
            ).result
        )
    }

    override suspend fun start(request: McpInit.InitializeRequest): McpInit.InitializeResult {
        jsonRpcTransport.start()
        jsonRpcServer.start()
        jsonRpcClient.start()
        val result = execute(request)
        execute(McpInit.InitializedNotification)
        return result
    }

    override fun close() {
        jsonRpcTransport.close()
        jsonRpcServer.close()
        jsonRpcClient.close()
        coroutineScope.cancel()
    }
}

private val handlePing: suspend (McpServerRequest<McpUtilities.Ping, McpUtilities.Pong>) -> McpUtilities.Pong =
    { McpUtilities.Pong }

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

private fun CoroutineScope.newChild(coroutineContext: CoroutineContext): CoroutineScope =
    CoroutineScope(this.coroutineContext + coroutineContext + SupervisorJob(this.coroutineContext[Job]))