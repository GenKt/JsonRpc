package io.genkt.mcp.server

import io.genkt.mcp.common.dto.McpServerCall
import kotlinx.coroutines.CoroutineScope

public interface McpServer: AutoCloseable {
    public val uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit
    public val coroutineScope: CoroutineScope
    public fun start()
    public suspend fun <R> call(call: McpServerCall<R>): R
}