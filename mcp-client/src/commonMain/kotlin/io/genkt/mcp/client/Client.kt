package io.genkt.mcp.client

import io.genkt.jsonprc.client.JsonRpcClient
import io.genkt.jsonrpc.JsonRpcTransport
import io.genkt.jsonrpc.RequestId
import io.genkt.jsonrpc.server.JsonRpcServer
import io.genkt.mcp.common.dto.*
import kotlinx.coroutines.CoroutineScope

public interface McpClient {
    public val info: McpInit.Implementation
    public val capabilities: McpInit.ClientCapabilities
    public val onRoot: suspend (McpRoot.ListRequest) -> McpRoot.ListResponse
    public val onSampling: suspend (McpSampling.CreateMessageRequest) -> McpSampling.CreateMessageResult
    public val onNotification: suspend (McpServerNotification) -> Unit
    public val transport: JsonRpcTransport
    public val requestIdGenerator: () -> RequestId
    public val progressTokenGenerator: () -> McpProgress.Token
    public val errorHandler: suspend CoroutineScope.(Throwable) -> Unit

    @McpClientInterceptionApi
    public val jsonRpcServer: JsonRpcServer

    @McpClientInterceptionApi
    public val jsonRpcClient: JsonRpcClient

    public val coroutineScope: CoroutineScope
    public suspend fun <R> call(mcpCall: McpClientCall<R>): R
    public suspend fun start()
    public suspend fun close()
}