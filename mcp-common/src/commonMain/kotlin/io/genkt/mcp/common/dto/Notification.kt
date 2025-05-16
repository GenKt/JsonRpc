package io.genkt.mcp.common.dto

import io.genkt.jsonrpc.RequestId
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
    public data class Progress(
        public val progressToken: String,
        public val progress: Int,
        public val total: Int,
        public val message: String,
    ): McpNotification

    @Serializable
    public data class Cancellation(
        public val requestId: RequestId,
        public val reason: String,
    ): McpNotification
}