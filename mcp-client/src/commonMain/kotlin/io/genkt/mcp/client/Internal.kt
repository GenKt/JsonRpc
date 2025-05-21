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
import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

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
            TODO()
        },
        { notification ->
            TODO()
        }
    )
    override val jsonRpcClient = JsonRpcClient(transportPair.first)

    override suspend fun <R> call(mcpCall: McpClientCall<R>): R {
        TODO("Not yet implemented")
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
    // TODO
}

private fun CoroutineScope.newChild(coroutineContext: CoroutineContext): CoroutineScope =
    CoroutineScope(this.coroutineContext + coroutineContext + Job(this.coroutineContext[Job]))