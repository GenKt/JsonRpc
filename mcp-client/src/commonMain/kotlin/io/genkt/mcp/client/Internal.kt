@file:OptIn(McpClientInterceptionApi::class)

package io.genkt.mcp.client

import io.genkt.jsonprc.client.JsonRpcClient
import io.genkt.jsonrpc.*
import io.genkt.jsonrpc.server.JsonRpcServer
import io.genkt.mcp.common.McpMethods
import io.genkt.mcp.common.dto.McpInit
import io.genkt.mcp.common.dto.McpNotification
import io.genkt.mcp.common.dto.McpRoot
import io.genkt.mcp.common.dto.McpSampling
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal class McpClientImpl(
    override val info: McpInit.ClientInfo,
    override val capabilities: McpInit.ClientCapabilities,
    override val onRoot: suspend () -> McpRoot.ListResponse,
    override val onSampling: suspend (McpSampling.Request) -> McpSampling.Response,
    override val onNotification: suspend (McpNotification) -> Unit,
    val transport: JsonRpcTransport,
    val requestIdGenerator: () -> RequestId = JsonRpc.NumberIdGenerator(),
    additionalContext: CoroutineContext = EmptyCoroutineContext,
) : McpClient {
    private val requestMutex = Mutex()
    private val requestMap = mutableMapOf<RequestId, Deferred<JsonElement>>()
    private val coroutineScope = transport.coroutineScope.newChild(additionalContext)
    private val transportPair = transport.shareAsClientAndServerIn()
    val jsonRpcServer = JsonRpcServer(
        transportPair.second,
        { request ->
            val deferred = coroutineScope.async {
                when (request.method) {
                    McpMethods.Roots.List -> JsonRpc.json.encodeToJsonElement(
                        McpRoot.ListResponse.serializer(),
                        onRoot()
                    )
                    McpMethods.Sampling.CreateMessage ->
                        JsonRpc.json.encodeToJsonElement(
                            McpSampling.Response.serializer(), onSampling(
                                JsonRpc.json.decodeFromJsonElement(
                                    McpSampling.Request.serializer(),
                                    request.params!!
                                )
                            )
                        )
                    McpMethods.Ping ->
                        buildJsonObject {}
                    else -> error("Unknown method: ${request.method}")
                }
            }
            requestMutex.withLock {
                requestMap[request.id] = deferred
            }
            deferred.invokeOnCompletion {
                coroutineScope.launch {
                    requestMutex.withLock {
                        requestMap.remove(request.id)
                    }
                }
            }
            JsonRpcSuccessResponse(
                id = request.id,
                result = deferred.await()
            )
        },
        {}
    )
    val jsonRpcClient = JsonRpcClient(transportPair.first)

    @McpClientInterceptionApi
    override fun nextRequestId(): RequestId = requestIdGenerator()

    @McpClientInterceptionApi
    override suspend fun sendJsonRpcRequest(request: JsonRpcRequest): JsonRpcSuccessResponse {
        return jsonRpcClient.send(request)
    }

    @McpClientInterceptionApi
    override suspend fun sendJsonRpcNotification(notification: JsonRpcNotification) {
        return jsonRpcClient.send(notification)
    }

    override suspend fun <T, R> call(mcpCall: McpClient.Call<T, R>): R {
        return mcpCall.execute(this)
    }

    override suspend fun start() {
        jsonRpcServer.start()
    }

    override suspend fun close() {
        coroutineScope.cancel()
        transport.close()
    }
}

private fun CoroutineScope.newChild(coroutineContext: CoroutineContext): CoroutineScope =
    CoroutineScope(this.coroutineContext + coroutineContext + Job(this.coroutineContext[Job]))