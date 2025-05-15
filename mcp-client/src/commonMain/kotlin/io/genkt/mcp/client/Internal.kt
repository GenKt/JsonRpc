package io.genkt.mcp.client

import io.genkt.mcp.common.dto.*

public class McpClientImpl(
    override val name: String,
    override val version: String,
    override val capabilities: McpClientCapabilities,
    override val onRoot: suspend () -> McpRoot.ListResponse,
    override val onSampling: suspend (McpSampling.Request) -> McpSampling.Response,
    override val onNotification: suspend (McpNotification) -> Unit,
): McpClient {
    override suspend fun start() {
        TODO("Not yet implemented")
    }
    override suspend fun listPrompt(request: McpPrompt.ListRequest): McpPrompt.ListResponse {
        TODO("Not yet implemented")
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