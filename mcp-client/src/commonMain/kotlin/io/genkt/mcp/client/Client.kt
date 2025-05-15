package io.genkt.mcp.client

import io.genkt.mcp.common.dto.*

public interface McpClient {
    public val name: String
    public val version: String
    public val capabilities: McpClientCapabilities
    public val onRoot: suspend () -> McpRoot.ListResponse
    public val onSampling: suspend (McpSampling.Request) -> McpSampling.Response
    public val onNotification: suspend (McpNotification) -> Unit
    public suspend fun listPrompt(request: McpPrompt.ListRequest): McpPrompt.ListResponse
    public suspend fun getPrompt(request: McpPrompt.GetRequest): McpPrompt.GetResponse
    public suspend fun listResource(request: McpResource.ListRequest): McpResource.ListResponse
    public suspend fun readResource(request: McpResource.ReadRequest): McpResource.ReadResponse
    public suspend fun readResourceTemplate(): McpResource.ListTemplateResponse
    public suspend fun listTools(request: McpTool.ListRequest): McpTool.ListResponse
    public suspend fun callTool(request: McpTool.CallRequest): McpTool.CallResponse
    public suspend fun getCompletion(request: McpCompletion.Request): McpCompletion.Response
    public suspend fun setLoggingLevel(request: McpLogging.SetLevelRequest)
    public suspend fun close()
}

public suspend fun McpClient.listPrompt(cursor: String? = null): McpPrompt.ListResponse =
    listPrompt(McpPrompt.ListRequest(cursor))

public suspend fun McpClient.getPrompt(name: String, args: Map<String, String> = emptyMap()): McpPrompt.GetResponse =
    getPrompt(McpPrompt.GetRequest(name, args))

public suspend fun McpClient.listResource(cursor: String? = null): McpResource.ListResponse =
    listResource(McpResource.ListRequest(cursor))

public suspend fun McpClient.readResource(uri: String): McpResource.ReadResponse =
    readResource(McpResource.ReadRequest(uri))

public suspend fun McpClient.listTools(cursor: String? = null): McpTool.ListResponse =
    listTools(McpTool.ListRequest(cursor))

public suspend fun McpClient.callTool(name: String, args: Map<String, String> = emptyMap()): McpTool.CallResponse =
    callTool(McpTool.CallRequest(name, args))

public suspend fun McpClient.getPromptCompletion(name: String, argName: String = "", argValue: String = ""): McpCompletion.Response =
    getCompletion(
        McpCompletion.Request(
            ref = McpCompletion.Reference.Prompt(name),
            argument = McpCompletion.Argument(
                argName,
                argValue
            )
        )
    )

public suspend fun McpClient.getResourceCompletion(uri: String, argName: String = "", argValue: String = ""): McpCompletion.Response =
    getCompletion(
        McpCompletion.Request(
            ref = McpCompletion.Reference.Resource(uri),
            argument = McpCompletion.Argument(
                argName,
                argValue
            )
        )
    )

public suspend fun McpClient.setLoggingLevel(level: String) = setLoggingLevel(McpLogging.SetLevelRequest(level))