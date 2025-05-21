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
    // TODO
): Interceptor<McpClient> {
    override fun invoke(client: McpClient): McpClient = InterceptedMcpClient(client, this)
    public class Builder: GenericInterceptorScope {
        // TODO
    }
    public companion object {
        public operator fun invoke(block: Builder.() -> Unit): McpClientInterceptor =
            Builder().apply(block).build()
    }
}

public fun McpClientInterceptor.Builder.build(): McpClientInterceptor =
    McpClientInterceptor(
        //TODO
    )

public fun McpClient.interceptWith(
    interceptor: Interceptor<McpClient>
) = interceptor(this)

public fun McpClient.intercepted(
    block: McpClientInterceptor.Builder.() -> Unit
) = interceptWith(McpClientInterceptor(block))