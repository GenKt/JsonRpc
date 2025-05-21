package io.genkt.mcp.client

import io.genkt.jsonrpc.GenericInterceptorScope
import io.genkt.jsonrpc.Interceptor
import io.genkt.mcp.common.dto.McpClientNotification
import io.genkt.mcp.common.dto.McpClientRequest
import io.genkt.mcp.common.dto.McpServerNotification
import io.genkt.mcp.common.dto.McpServerRequest
import kotlinx.coroutines.CoroutineScope

public class McpClientInterceptor(
    public val onRequestInterceptor: Interceptor<suspend (McpServerRequest<*>) -> Any>,
    public val onNotificationInterceptor: Interceptor<suspend (McpServerNotification) -> Unit>,
    public val requestInterceptor: Interceptor<suspend (McpClientRequest<*>) -> Any>,
    public val notificationInterceptor: Interceptor<suspend (McpClientNotification) -> Unit>,
    public val errorHandlerInterceptor: Interceptor<suspend CoroutineScope.(Throwable) -> Unit>,
) : Interceptor<McpClient> {
    override fun invoke(client: McpClient): McpClient = InterceptedMcpClient(client, this)
    public class Builder : GenericInterceptorScope {
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