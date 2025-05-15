package io.genkt.mcp.client

import io.genkt.jsonprc.client.JsonRpcClient
import io.genkt.jsonprc.client.sendRequest
import io.genkt.jsonrpc.JsonRpc
import io.genkt.jsonrpc.RequestId
import io.genkt.jsonrpc.server.JsonRpcServer
import io.genkt.mcp.common.McpMethods
import io.genkt.mcp.common.dto.McpCompletion
import io.genkt.mcp.common.dto.McpNotification
import io.genkt.mcp.common.dto.McpPrompt
import io.genkt.mcp.common.dto.McpResource
import io.genkt.mcp.common.dto.McpRoot
import io.genkt.mcp.common.dto.McpSampling
import io.genkt.mcp.common.dto.McpTool
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy

public interface McpClient {
    public val name: String
    public val version: String
    public val capabilities: McpClientCapabilities
    public val onRoot: suspend () -> McpRoot.ListResponse
    public val onSampling: suspend (McpSampling.Request) -> McpSampling.Response
    public val onNotification: suspend (McpNotification) -> Unit
    public val requestIdProvider: () -> RequestId
    public val jsonRpcServer: JsonRpcServer
    public val jsonRpcClient: JsonRpcClient
    public suspend fun start()
    public suspend fun close()
}

public suspend fun <T, R> McpClient.send(
    method: String,
    params: T,
    paramSerializer: SerializationStrategy<T>,
    resultSerializer: DeserializationStrategy<R>
): R =
    jsonRpcClient.sendRequest(
        id = requestIdProvider(),
        method = method,
        params = JsonRpc.json.encodeToJsonElement(paramSerializer, params),
    ).let { response ->
        JsonRpc.json.decodeFromJsonElement(resultSerializer, response.result)
    }

public data class McpClientCapability<in T, out R>(
    public val method: String,
    public val paramSerializer: SerializationStrategy<T>,
    public val resultSerializer: DeserializationStrategy<R>,
)

public suspend fun <T, R> McpClient.requestWith(capability: McpClientCapability<T, R>, params: T): R =
    send(capability.method, params, capability.paramSerializer, capability.resultSerializer)

public object McpClientCapabilities {
    public val ListPrompt: McpClientCapability<McpPrompt.ListRequest, McpPrompt.ListResponse> =
        McpClientCapability(
            McpMethods.Prompts.List,
            McpPrompt.ListRequest.serializer(),
            McpPrompt.ListResponse.serializer()
        )
    public val GetPrompt: McpClientCapability<McpPrompt.GetRequest, McpPrompt.GetResponse> =
        McpClientCapability(
            McpMethods.Prompts.Get,
            McpPrompt.GetRequest.serializer(),
            McpPrompt.GetResponse.serializer()
        )
    public val ListResource: McpClientCapability<McpResource.ListRequest, McpResource.ListResponse> =
        McpClientCapability(
            McpMethods.Resources.List,
            McpResource.ListRequest.serializer(),
            McpResource.ListResponse.serializer()
        )
    public val GetResource: McpClientCapability<McpResource.ReadRequest, McpResource.ReadResponse> =
        McpClientCapability(
            McpMethods.Resources.Read,
            McpResource.ReadRequest.serializer(),
            McpResource.ReadResponse.serializer()
        )
    public val ListTool: McpClientCapability<McpTool.ListRequest, McpTool.ListResponse> =
        McpClientCapability(
            McpMethods.Tools.List,
            McpTool.ListRequest.serializer(),
            McpTool.ListResponse.serializer()
        )
    public val CallTool: McpClientCapability<McpTool.CallRequest, McpTool.CallResponse> =
        McpClientCapability(
            McpMethods.Tools.Call,
            McpTool.CallRequest.serializer(),
            McpTool.CallResponse.serializer()
        )
    public val GetCompletion: McpClientCapability<McpCompletion.Request, McpCompletion.Response> =
        McpClientCapability(
            McpMethods.Completion.Complete,
            McpCompletion.Request.serializer(),
            McpCompletion.Response.serializer()
        )
}