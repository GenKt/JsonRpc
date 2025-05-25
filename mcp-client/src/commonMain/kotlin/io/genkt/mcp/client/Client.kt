package io.genkt.mcp.client

import io.genkt.jsonrpc.JsonRpcTransport
import io.genkt.jsonrpc.RequestId
import io.genkt.mcp.common.dto.*
import kotlinx.coroutines.CoroutineScope

public interface McpClient {
    public val info: McpInit.Implementation
    public val capabilities: McpInit.ClientCapabilities
    public val transport: JsonRpcTransport
    public val uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit

    public val coroutineScope: CoroutineScope
    public suspend fun <R> call(mcpCall: McpClientCall<R>): R
    public suspend fun start()
    public suspend fun close()
}