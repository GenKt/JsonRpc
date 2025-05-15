@file:OptIn(InternalMcpClientApi::class)
package io.genkt.mcp.client

import io.genkt.jsonprc.client.JsonRpcClient
import io.genkt.jsonrpc.*
import io.genkt.jsonrpc.server.JsonRpcServer
import io.genkt.mcp.common.McpMethods
import io.genkt.mcp.common.dto.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal class McpClientImpl(
    override val name: String,
    override val version: String,
    override val capabilities: McpClientCapabilities,
    override val onRoot: suspend () -> McpRoot.ListResponse,
    override val onSampling: suspend (McpSampling.Request) -> McpSampling.Response,
    override val onNotification: suspend (McpNotification) -> Unit,
    transport: JsonRpcTransport,
    override val requestIdProvider: () -> RequestId = { RequestId.NumberId(1) },
    additionalContext: CoroutineContext = EmptyCoroutineContext,
) : McpClient {
    private var isActive = false
    private val requestMutex = Mutex()
    private val requestMap = mutableMapOf<RequestId, Deferred<JsonElement>>()
    private val coroutineScope = transport.coroutineScope.newChild(additionalContext)
    private val transportPair = transport.shareAsClientAndServerIn()
    @property:InternalMcpClientApi
    override val jsonRpcServer = JsonRpcServer(
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
    @property:InternalMcpClientApi
    override val jsonRpcClient = JsonRpcClient(transportPair.first)

    override suspend fun start() {
        jsonRpcServer.start()
    }

    override suspend fun close() {
        TODO("Not yet implemented")
    }
}

private fun CoroutineScope.newChild(coroutineContext: CoroutineContext): CoroutineScope =
    CoroutineScope(this.coroutineContext + coroutineContext + Job(this.coroutineContext[Job]))