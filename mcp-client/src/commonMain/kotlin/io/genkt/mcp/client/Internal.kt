package io.genkt.mcp.client

import io.genkt.jsonprc.client.JsonRpcClient
import io.genkt.jsonrpc.*
import io.genkt.jsonrpc.server.JsonRpcServer
import io.genkt.mcp.common.McpMethods
import io.genkt.mcp.common.dto.*
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

public class McpClientImpl(
    override val name: String,
    override val version: String,
    override val capabilities: McpClientCapabilities,
    override val onRoot: suspend () -> McpRoot.ListResponse,
    override val onSampling: suspend (McpSampling.Request) -> McpSampling.Response,
    override val onNotification: suspend (McpNotification) -> Unit,
    transportPair: Pair<JsonRpcClientTransport, JsonRpcServerTransport>,
    private val requestIdProvider: () -> RequestId = { RequestId.NumberId(1) },
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
) : McpClient {
    private val requestMutex = Mutex()
    private val requestMap = mutableMapOf<RequestId, CancellableContinuation<JsonElement>>()
    private val coroutineScope = CoroutineScope(coroutineContext)
    private val jsonRpcServer = JsonRpcServer(
        transportPair.second,
        { request ->
            val result = suspendCancellableCoroutine { continuation ->
                coroutineScope.launch {
                    requestMutex.withLock {
                        requestMap[request.id] = continuation
                    }
                    try {
                        val response = when (request.method) {
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

                            else -> error("Unknown method: ${request.method}")
                        }
                        continuation.resumeWith(Result.success(response))
                        requestMutex.withLock {
                            requestMap.remove(request.id)
                        }
                    } catch (e: Throwable) {
                        continuation.resumeWith(Result.failure(e))
                    }
                }
            }
            JsonRpcSuccessResponse(
                id = request.id,
                result = result
            )
        },
        {}
    )
    private val jsonRpcClient = JsonRpcClient(transportPair.first)
    override suspend fun start() {
        jsonRpcServer.start()
    }

    override suspend fun listPrompt(request: McpPrompt.ListRequest): McpPrompt.ListResponse {
        val rpcRequest = JsonRpcRequest(
            id = requestIdProvider(),
            method = McpMethods.Prompts.List,
            params = JsonRpc.json.encodeToJsonElement(McpPrompt.ListRequest.serializer(), request)
        )
        val response = jsonRpcClient.send(rpcRequest)
        return response.result.let { JsonRpc.json.decodeFromJsonElement(McpPrompt.ListResponse.serializer(), it) }
    }

    override suspend fun getPrompt(request: McpPrompt.GetRequest): McpPrompt.GetResponse {
        TODO("Not yet implemented")
    }

    override suspend fun listResource(request: McpResource.ListRequest): McpResource.ListResponse {
        TODO("Not yet implemented")
    }

    override suspend fun readResource(request: McpResource.ReadRequest): McpResource.ReadResponse {
        TODO("Not yet implemented")
    }

    override suspend fun readResourceTemplate(): McpResource.ListTemplateResponse {
        TODO("Not yet implemented")
    }

    override suspend fun listTools(request: McpTool.ListRequest): McpTool.ListResponse {
        TODO("Not yet implemented")
    }

    override suspend fun callTool(request: McpTool.CallRequest): McpTool.CallResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getCompletion(request: McpCompletion.Request): McpCompletion.Response {
        TODO("Not yet implemented")
    }

    override suspend fun setLoggingLevel(request: McpLogging.SetLevelRequest) {
        TODO("Not yet implemented")
    }

    override suspend fun close() {
        TODO("Not yet implemented")
    }
}