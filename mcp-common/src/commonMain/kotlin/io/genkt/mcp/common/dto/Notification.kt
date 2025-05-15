package io.genkt.mcp.common.dto

import kotlinx.serialization.Serializable

public enum class ListChangeResource {
    ROOTS,
    PROMPTS,
    RESOURCES,
    TOOLS,
}

public sealed interface McpNotification {
    public data class ListChange(
        public val type: ListChangeResource,
    ): McpNotification

    @Serializable
    public data class SubscribeResource(
        public val uri: String,
    ): McpNotification

    @Serializable
    public data class LogMessage(
        public val message: McpLogging.LogMessage,
    ): McpNotification

    // TODO: Other Notification data classes
}