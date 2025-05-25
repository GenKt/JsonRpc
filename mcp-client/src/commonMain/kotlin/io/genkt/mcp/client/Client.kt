package io.genkt.mcp.client

import io.genkt.jsonrpc.*
import io.genkt.mcp.common.dto.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

public interface McpClient {
    public val info: McpInit.Implementation
    public val capabilities: McpInit.ClientCapabilities
    public val transport: JsonRpcTransport
    public val uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit

    public val coroutineScope: CoroutineScope
    public suspend fun <R> call(mcpCall: McpClientCall<R>): R
    public suspend fun start()
    public suspend fun close()
    public class Builder {
        public var info: McpInit.Implementation =
            McpInit.Implementation(
                name = "GenKtMcpClient",
                version = "0.0.1-SNAPSHOT"
            )
        public var experimentalCapabilities: JsonObject? = null
        public var sampling: SamplingCapability? = null
        public var root: RootCapability? = null
        public var transport: JsonRpcTransport =
            Transport.ThrowingException { error("Using an uninitialized transport") }
        public var uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit = {}
        public var onNotification: suspend (McpServerNotification<McpServerBasicNotification>) -> Unit = {}
        public var requestIdGenerator: () -> RequestId = JsonRpc.NumberIdGenerator()
        public var progressTokenGenerator: () -> McpProgress.Token = McpProgress.defaultStringTokenGenerator
        public var additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext
        public var callInterceptor: Interceptor<suspend (McpClientCall<*>) -> Any?> = { it }
        public var onCallInterceptor: Interceptor<suspend (McpServerNotification<McpServerBasicNotification>) -> Unit> =
            { it }

        public class RootCapability {
            public var onListRequest: (suspend (McpServerRequest<McpRoot.ListRequest, McpRoot.ListResponse>) -> McpRoot.ListResponse) =
                { throw NotImplementedError() }
            public var listChanged: Boolean = false
        }

        public class SamplingCapability {
            public var onCreateMessageRequest: (suspend (McpServerRequest<McpSampling.CreateMessageRequest, McpSampling.CreateMessageResult>) -> McpSampling.CreateMessageResult) =
                { throw NotImplementedError() }
        }
    }
}