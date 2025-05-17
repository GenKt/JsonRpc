package io.genkt.mcp.client

import io.genkt.jsonprc.client.JsonRpcClient
import io.genkt.jsonrpc.GenericInterceptorScope
import io.genkt.jsonrpc.Interceptor
import io.genkt.jsonrpc.JsonRpcClientCall
import io.genkt.jsonrpc.JsonRpcTransport
import io.genkt.jsonrpc.RequestId
import io.genkt.jsonrpc.server.JsonRpcServer
import io.genkt.mcp.common.ProgressingResult
import io.genkt.mcp.common.dto.McpInit
import io.genkt.mcp.common.dto.McpNotification
import io.genkt.mcp.common.dto.McpRoot
import io.genkt.mcp.common.dto.McpSampling

public class McpClientInterceptor(
    public val interceptInfo: Interceptor<McpInit.ClientInfo> = {it},
    public val interceptCapabilities: Interceptor<McpInit.ClientCapabilities> = {it},
    public val interceptRootHandler: Interceptor<suspend () -> McpRoot.ListResponse> = {it},
    public val interceptSamplingHandler: Interceptor<suspend (McpSampling.Request) -> McpSampling.Response> = {it},
    public val interceptNotificationHandler: Interceptor<suspend (McpNotification) -> Unit> = {it},
    public val interceptTransport: Interceptor<JsonRpcTransport> = {it},
    public val interceptRequestIdGenerator: Interceptor<() -> RequestId> = {it},
    public val interceptSendJsonRpcCall: Interceptor<suspend (JsonRpcClientCall<*>) -> Any?> = {it},
    public val interceptCall: Interceptor<suspend (McpClient.Call<*, *>) -> ProgressingResult<*>> = {it},
    public val interceptStart: Interceptor<suspend () -> Unit> = {it},
    public val interceptClose: Interceptor<suspend () -> Unit> = {it},
    public val interceptRpcServer: Interceptor<JsonRpcServer> = {it},
    public val interceptRpcClient: Interceptor<JsonRpcClient> = {it},
): Interceptor<McpClient> {
    override fun invoke(client: McpClient): McpClient = InterceptedMcpClient(client, this)
    public class Builder: GenericInterceptorScope {
        public var infoInterceptor: Interceptor<McpInit.ClientInfo> = {it}
        public var capabilitiesInterceptor: Interceptor<McpInit.ClientCapabilities> = {it}
        public var rootHandlerInterceptor: Interceptor<suspend () -> McpRoot.ListResponse> = {it}
        public var samplingHandlerInterceptor: Interceptor<suspend (McpSampling.Request) -> McpSampling.Response> = {it}
        public var notificationHandlerInterceptor: Interceptor<suspend (McpNotification) -> Unit> = {it}
        public var transportInterceptor: Interceptor<JsonRpcTransport> = {it}
        public var requestIdGeneratorInterceptor: Interceptor<() -> RequestId> = {it}
        public var sendJsonRpcCallInterceptor: Interceptor<suspend (JsonRpcClientCall<*>) -> Any?> = {it}
        public var callInterceptor: Interceptor<suspend (McpClient.Call<*, *>) -> ProgressingResult<*>> = {it}
        public var startInterceptor: Interceptor<suspend () -> Unit> = {it}
        public var closeInterceptor: Interceptor<suspend () -> Unit> = {it}
        public var rpcServerInterceptor: Interceptor<JsonRpcServer> = {it}
        public var rpcClientInterceptor: Interceptor<JsonRpcClient> = {it}
    }
    public companion object {
        public operator fun invoke(block: Builder.() -> Unit): McpClientInterceptor =
            Builder().apply(block).build()
    }
}

public fun McpClientInterceptor.Builder.build(): McpClientInterceptor =
    McpClientInterceptor(
        interceptInfo = infoInterceptor,
        interceptCapabilities = capabilitiesInterceptor,
        interceptRootHandler = rootHandlerInterceptor,
        interceptSamplingHandler = samplingHandlerInterceptor,
        interceptNotificationHandler = notificationHandlerInterceptor,
        interceptTransport = transportInterceptor,
        interceptRequestIdGenerator = requestIdGeneratorInterceptor,
        interceptSendJsonRpcCall = sendJsonRpcCallInterceptor,
        interceptCall = callInterceptor,
        interceptStart = startInterceptor,
        interceptClose = closeInterceptor,
        interceptRpcServer = rpcServerInterceptor,
        interceptRpcClient = rpcClientInterceptor,
    )

public fun McpClient.interceptWith(
    interceptor: Interceptor<McpClient>
) = interceptor(this)

public fun McpClient.intercepted(
    block: McpClientInterceptor.Builder.() -> Unit
) = interceptWith(McpClientInterceptor(block))