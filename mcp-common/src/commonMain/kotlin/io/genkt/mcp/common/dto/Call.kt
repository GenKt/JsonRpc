package io.genkt.mcp.common.dto

import kotlinx.serialization.DeserializationStrategy

public sealed interface McpCall<out R> {
    public val method: String
    public companion object
}
public sealed interface McpProgressRequest<out Result, out Request>: McpCall<Result> {
    public val token: McpProgress.Token
    public val request: Request
}
public sealed interface McpServerCall<out R>: McpCall<R> {
    public companion object
}
public sealed interface McpClientCall<out R>: McpCall<R> {
    public companion object
}
public sealed interface McpClientRequest<out R>: McpClientCall<R> {
    public val resultDeserializer: DeserializationStrategy<R>
    public companion object
}
public sealed interface McpClientNotification: McpClientCall<Unit> {
    public companion object
}
public sealed interface McpServerRequest<out R>: McpServerCall<R> {
    public val resultDeserializer: DeserializationStrategy<R>
    public companion object
}
public sealed interface McpServerNotification: McpServerCall<Unit> {
    public companion object
}