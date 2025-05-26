package io.genkt.mcp.client

import io.genkt.jsonrpc.Interceptor
import io.genkt.jsonrpc.JsonRpc
import io.genkt.jsonrpc.JsonRpcTransport
import io.genkt.jsonrpc.RequestId
import io.genkt.mcp.common.dto.*
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

public interface McpClient: AutoCloseable {
    public val jsonRpcTransport: JsonRpcTransport
    public val uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit
    public val coroutineScope: CoroutineScope
    public suspend fun <R> call(mcpCall: McpClientCall<R>): R
    public suspend fun start(request: McpInit.InitializeRequest): McpInit.InitializeResult
}

public fun McpClient(
    jsonRpcTransport: JsonRpcTransport,
    onRoot: suspend (McpServerRequest<McpRoot.ListRequest, McpRoot.ListResponse>) -> McpRoot.ListResponse,
    onSampling: suspend (McpServerRequest<McpSampling.CreateMessageRequest, McpSampling.CreateMessageResult>) -> McpSampling.CreateMessageResult,
    onNotification: suspend (McpServerNotification<McpServerBasicNotification>) -> Unit,
    requestIdGenerator: () -> RequestId = JsonRpc.NumberIdGenerator(),
    progressTokenGenerator: () -> McpProgress.Token = McpProgress.defaultStringTokenGenerator,
    uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit = {},
    callInterceptor: Interceptor<suspend (McpClientCall<*>) -> Any?> = { it },
    additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext,
): McpClient = McpClientImpl(
    jsonRpcTransport,
    onRoot,
    onSampling,
    onNotification,
    requestIdGenerator,
    progressTokenGenerator,
    uncaughtErrorHandler,
    callInterceptor,
    additionalCoroutineContext,
)