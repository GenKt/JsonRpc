package io.genkt.mcp.common.dto

import io.genkt.mcp.common.McpMethods
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.jvm.JvmInline

@Serializable
public data class McpRoot(
    public val uri: String,
    public val name: String? = null,
) {
    @Serializable
    public data object ListRequest : McpServerBasicRequest<ListResponse> {
        override val method: String get() = McpMethods.Roots.List
        override val resultSerializer: KSerializer<ListResponse>
            get() = ListResponse.serializer()
    }

    @Serializable
    public data class ListResponse(
        public val roots: List<McpRoot>,
    )

    @Serializable
    public data object ListChangedNotification : McpClientBasicNotification {
        override val method: String get() = McpMethods.Notifications.Roots.ListChanged
    }
}

public sealed interface McpSampling {
    @Serializable
    public data class CreateMessageRequest(
        public val messages: List<Message>,
        public val modelPreferences: ModelPreferences? = null,
        public val systemPrompt: String? = null,
        public val includeContext: IncludeContext? = null,
        public val temperature: Double? = null,
        public val maxTokens: Long? = null,
        public val stopSequences: List<String>? = null,
        public val metadata: JsonObject? = null,
    ) : McpServerBasicRequest<CreateMessageResult> {
        override val method: String get() = McpMethods.Sampling.CreateMessage
        override val resultSerializer: KSerializer<CreateMessageResult>
            get() = CreateMessageResult.serializer()
    }

    @Suppress("EnumEntryName")
    @Serializable
    public enum class IncludeContext {
        none, thisServer, allServers
    }

    @Serializable
    public data class ModelPreferences(
        public val hints: List<ModelHint>? = null,
        public val costPriority: Double? = null,
        public val speedPriority: Double? = null,
        public val intelligencePriority: Double? = null,
    )

    @Serializable
    public data class ModelHint(
        public val name: String? = null,
    )

    @Serializable
    public data class Message(
        public val role: String,
        public val content: McpContent.Sampling,
    )

    @Serializable
    public data class CreateMessageResult(
        public val role: String,
        public val content: McpContent.Sampling,
        public val model: String,
        public val stopReason: StopReason? = null,
    )

    @JvmInline
    @Serializable
    public value class StopReason(public val value: String) {
        public companion object {
            public val endTurn: StopReason = StopReason("endTurn")
            public val stopSequence: StopReason = StopReason("stopSequence")
            public val maxTokens: StopReason = StopReason("maxTokens")
        }
    }
}