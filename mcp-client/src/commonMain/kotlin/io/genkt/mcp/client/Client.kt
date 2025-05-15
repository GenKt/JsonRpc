package io.genkt.mcp.client

import io.genkt.mcp.common.dto.McpClientCapabilities
import io.genkt.mcp.common.dto.McpNotification
import io.genkt.mcp.common.dto.McpRoot
import io.genkt.mcp.common.dto.McpSampling

public interface McpClient {
    public val name: String
    public val version: String
    public val capabilities: McpClientCapabilities
    public val onRoot: suspend () -> McpRoot.ListResponse
    public val onSampling: suspend (McpSampling.Request) -> McpSampling.Response
    public val onNotification: suspend (McpNotification) -> Unit
}