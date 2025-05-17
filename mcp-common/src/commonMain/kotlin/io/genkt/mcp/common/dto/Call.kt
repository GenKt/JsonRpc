package io.genkt.mcp.common.dto

import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonElement

public sealed interface McpCall<out R> {
    public val method: String
    public val param: JsonElement
}
public sealed interface McpProgressRequest<out Result, out Request>: McpCall<Result> {
    public val token: McpProgress.Token
    public val request: Request
    public val progressChannel: SendChannel<McpProgress.ProgressNotification>
}
public sealed interface McpServerCall<out R>: McpCall<R>
public sealed interface McpClientCall<out R>: McpCall<R>
public sealed interface McpClientRequest<out R>: McpClientCall<R> {
    public val resultDeserializer: DeserializationStrategy<R>
}
public sealed interface McpClientProgressRequest<out R>: McpProgressRequest<R, McpClientRequest<R>>, McpClientRequest<R>
public sealed interface McpClientNotification: McpClientCall<Unit>
public sealed interface McpServerRequest<out R>: McpServerCall<R>
public sealed interface McpServerProgressRequest<out R>: McpProgressRequest<R, McpServerRequest<R>>, McpServerRequest<R>
public sealed interface McpServerNotification: McpServerCall<Unit>