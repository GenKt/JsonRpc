package io.genkt.mcp.client

import io.genkt.jsonprc.client.JsonRpcClient
import io.genkt.jsonrpc.*
import io.genkt.jsonrpc.server.JsonRpcServer
import io.genkt.mcp.common.McpMethods
import io.genkt.mcp.common.dto.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

public fun McpClient(
    info: McpInit.ClientInfo,
    capabilities: McpInit.ClientCapabilities,
    onRoot: suspend () -> McpRoot.ListResponse,
    onSampling: suspend (McpSampling.Request) -> McpSampling.Response,
    onNotification: suspend (McpNotification) -> Unit,
    transport: JsonRpcTransport,
    requestIdGenerator: () -> RequestId = JsonRpc.NumberIdGenerator(),
    additionalContext: CoroutineContext = EmptyCoroutineContext,
): McpClient =
    McpClientImpl(
        info,
        capabilities,
        onRoot,
        onSampling,
        onNotification,
        transport,
        requestIdGenerator,
        additionalContext,
    )

public interface McpClient {
    public val info: McpInit.ClientInfo
    public val capabilities: McpInit.ClientCapabilities
    public val onRoot: suspend () -> McpRoot.ListResponse
    public val onSampling: suspend (McpSampling.Request) -> McpSampling.Response
    public val onNotification: suspend (McpNotification) -> Unit
    public val transport: JsonRpcTransport
    public val requestIdGenerator: () -> RequestId

    public val jsonRpcServer: JsonRpcServer
    public val jsonRpcClient: JsonRpcClient

    @McpClientInterceptionApi
    public fun nextRequestId(): RequestId

    @McpClientInterceptionApi
    public suspend fun sendJsonRpcRequest(request: JsonRpcRequest): JsonRpcSuccessResponse

    @McpClientInterceptionApi
    public suspend fun sendJsonRpcNotification(notification: JsonRpcNotification)
    public suspend fun <T, R> call(mcpCall: Call<T, R>): R
    public suspend fun start()
    public suspend fun close()
    public sealed interface Call<T, R> {
        public suspend fun execute(mcpClient: McpClient): R
        public data class Request<T, R>(
            public val method: String,
            public val param: T,
            public val paramSerializer: SerializationStrategy<T>,
            public val resultDeserializer: DeserializationStrategy<R>,
        ) : Call<T, R> {
            @OptIn(McpClientInterceptionApi::class)
            public override suspend fun execute(mcpClient: McpClient): R {
                val response = mcpClient.sendJsonRpcRequest(
                    JsonRpcRequest(
                        id = mcpClient.nextRequestId(),
                        method = method,
                        params = JsonRpc.json.encodeToJsonElement(paramSerializer, param),
                    )
                )
                return JsonRpc.json.decodeFromJsonElement(resultDeserializer, response.result)
            }
        }

        public data class Notification<T>(
            public val method: String,
            public val param: T,
            public val paramSerializer: SerializationStrategy<T>,
        ) : Call<T, Unit> {
            @OptIn(McpClientInterceptionApi::class)
            public override suspend fun execute(mcpClient: McpClient) {
                mcpClient.sendJsonRpcNotification(
                    JsonRpcNotification(
                        method = method,
                        params = JsonRpc.json.encodeToJsonElement(paramSerializer, param),
                    )
                )
            }
        }

        public data class PreparedRequest<T, R>(
            public val method: String,
            public val paramSerializer: SerializationStrategy<T>,
            public val resultDeserializer: DeserializationStrategy<R>,
        ) : (T) -> Call<T, R> {
            override fun invoke(param: T): Call<T, R> =
                Request(
                    method = method,
                    param = param,
                    paramSerializer = paramSerializer,
                    resultDeserializer = resultDeserializer
                )
        }

        public data class PreparedNotification<T>(
            public val method: String,
            public val paramSerializer: SerializationStrategy<T>,
        ) : (T) -> Call<T, Unit> {
            override fun invoke(param: T): Call<T, Unit> =
                Notification(
                    method = method,
                    param = param,
                    paramSerializer = paramSerializer
                )
        }

        public companion object Intrinsics {
            public val listPrompt: PreparedRequest<McpPrompt.ListRequest, McpPrompt.ListResponse> =
                PreparedRequest(
                    method = McpMethods.Prompts.List,
                    paramSerializer = McpPrompt.ListRequest.serializer(),
                    resultDeserializer = McpPrompt.ListResponse.serializer()
                )
            public val getPrompt: PreparedRequest<McpPrompt.GetRequest, McpPrompt.GetResponse> =
                PreparedRequest(
                    method = McpMethods.Prompts.Get,
                    paramSerializer = McpPrompt.GetRequest.serializer(),
                    resultDeserializer = McpPrompt.GetResponse.serializer()
                )
            public val listResource: PreparedRequest<McpResource.ListRequest, McpResource.ListResponse> =
                PreparedRequest(
                    method = McpMethods.Resources.List,
                    paramSerializer = McpResource.ListRequest.serializer(),
                    resultDeserializer = McpResource.ListResponse.serializer()
                )
            public val readResource: PreparedRequest<McpResource.ReadRequest, McpResource.ReadResponse> =
                PreparedRequest(
                    method = McpMethods.Resources.Read,
                    paramSerializer = McpResource.ReadRequest.serializer(),
                    resultDeserializer = McpResource.ReadResponse.serializer()
                )
            public val listTool: PreparedRequest<McpTool.ListRequest, McpTool.ListResponse> =
                PreparedRequest(
                    method = McpMethods.Tools.List,
                    paramSerializer = McpTool.ListRequest.serializer(),
                    resultDeserializer = McpTool.ListResponse.serializer()
                )
            public val callTool: PreparedRequest<McpTool.CallRequest, McpTool.CallResponse> =
                PreparedRequest(
                    method = McpMethods.Tools.Call,
                    paramSerializer = McpTool.CallRequest.serializer(),
                    resultDeserializer = McpTool.CallResponse.serializer()
                )
            public val getCompletion: PreparedRequest<McpCompletion.Request, McpCompletion.Response> =
                PreparedRequest(
                    method = McpMethods.Completion.Complete,
                    paramSerializer = McpCompletion.Request.serializer(),
                    resultDeserializer = McpCompletion.Response.serializer()
                )
            public val setLogLevel: PreparedNotification<McpLogging.SetLevelRequest> =
                PreparedNotification(
                    method = McpMethods.Logging.SetLevel,
                    paramSerializer = McpLogging.SetLevelRequest.serializer(),
                )
        }
    }
}

public suspend fun <T, R> McpClient.call(callBuilder: McpClient.Call.Intrinsics.() -> McpClient.Call<T, R>): R {
    return call(McpClient.Call.Intrinsics.callBuilder())
}

public suspend fun McpClient.listPrompt(cursor: String? = null) =
    call {
        listPrompt(McpPrompt.ListRequest(cursor))
    }

public suspend fun McpClient.getPrompt(name: String, arguments: Map<String, String> = emptyMap()) =
    call {
        getPrompt(McpPrompt.GetRequest(name, arguments))
    }

public suspend fun McpClient.listResource(cursor: String? = null) =
    call {
        listResource(McpResource.ListRequest(cursor))
    }

public suspend fun McpClient.readResource(uri: String) =
    call {
        readResource(McpResource.ReadRequest(uri))
    }

public suspend fun McpClient.listTool(cursor: String? = null) =
    call {
        listTool(McpTool.ListRequest(cursor))
    }

public suspend fun McpClient.callTool(name: String, arguments: JsonObject) =
    call {
        callTool(McpTool.CallRequest(name, arguments))
    }

public suspend fun McpClient.getPromptCompletion(name: String, argName: String = "", argValue: String = "") =
    call {
        getCompletion(
            McpCompletion.Request(
                McpCompletion.Reference.Prompt(name),
                McpCompletion.Argument(argName, argValue)
            )
        )
    }

public suspend fun McpClient.getResourceCompletion(uri: String, argName: String = "", argValue: String = "") =
    call {
        getCompletion(
            McpCompletion.Request(
                McpCompletion.Reference.Resource(uri),
                McpCompletion.Argument(argName, argValue)
            )
        )
    }

public suspend fun McpClient.setLogLevel(level: McpLogging.Level) =
    call {
        setLogLevel(McpLogging.SetLevelRequest(level))
    }