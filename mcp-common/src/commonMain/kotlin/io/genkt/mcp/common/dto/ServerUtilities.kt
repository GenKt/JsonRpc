package io.genkt.mcp.common.dto

import io.genkt.mcp.common.McpMethods
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
public data class McpCompletion(
    public val values: List<String>,
    public val total: Int? = null,
    public val hasMore: Boolean? = null,
) {
    @Serializable
    public data class Request(
        public val ref: Reference,
        public val argument: Argument,
    ) : McpClientRequest<Result> {
        override val method: String get() = McpMethods.Completion.Complete
        override val resultSerializer: KSerializer<Result>
            get() = Result.serializer()

        @Serializable
        public data class Argument(
            public val name: String,
            public val value: String,
        )
    }

    @Serializable(with = McpCompletionReferenceSerializer::class)
    public sealed interface Reference {
        @Serializable
        public data class Prompt(
            public val type: String = "ref/prompt",
            public val name: String,
        ) : Reference

        @Serializable
        public data class Resource(
            public val type: String = "ref/resource",
            public val uri: String,
        ) : Reference
    }

    @Serializable
    public data class Result(
        public val completion: McpCompletion,
    )
}

public sealed interface McpLogging {
    @Serializable
    @Suppress("EnumEntryName")
    public enum class Level {
        debug, info, notice, warning, error, critical, alert, emergency
    }

    @Serializable
    public data class SetLevelRequest(
        public val level: Level,
    ) : McpClientRequest<SetLevelResult> {
        override val method: String get() = McpMethods.Logging.SetLevel
        override val resultSerializer: KSerializer<SetLevelResult>
            get() = SetLevelResult.serializer()
    }

    @Serializable
    public data object SetLevelResult

    @Serializable
    public data class LogMessage(
        public val level: Level,
        public val logger: String,
        public val data: JsonElement,
    ) : McpServerNotification {
        override val method: String get() = McpMethods.Notifications.Message
    }
}