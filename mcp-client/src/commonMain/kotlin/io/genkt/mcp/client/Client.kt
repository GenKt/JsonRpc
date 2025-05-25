package io.genkt.mcp.client

import io.genkt.jsonrpc.JsonRpcTransport
import io.genkt.jsonrpc.Transport
import io.genkt.mcp.common.dto.McpClientCall
import io.genkt.mcp.common.dto.McpInit
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonObject

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
        public var capabilities: McpInit.ClientCapabilities = McpInit.ClientCapabilities()
        public var transport: JsonRpcTransport =
            Transport.ThrowingException { error("Using an uninitialized transport") }
        public var uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit = {}
    }
}