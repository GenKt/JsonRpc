package io.genkt.mcp.common.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.jvm.JvmInline

public sealed interface McpCompletion {
    @Serializable
    public data class Request(
        public val ref: Reference,
        public val argument: Argument,
    )

    @Serializable(with = McpCompletionReferenceSerializer::class)
    public sealed interface Reference {
        @Serializable(with = McpCompletionPromptReferenceSerializer::class)
        @JvmInline
        public value class Prompt(
            public val name: String,
        ) : Reference

        @Serializable(with = McpCompletionResourceReferenceSerializer::class)
        @JvmInline
        public value class Resource(
            public val uri: String,
        ) : Reference
    }

    @Serializable
    public data class Argument(
        public val name: String,
        public val value: String,
    )

    @Serializable
    public data class Response(
        public val completion: Completion,
    )

    @Serializable
    public data class Completion(
        public val values: List<String>,
        public val total: Int,
        public val hasMore: Boolean,
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
    )

    @Serializable
    public data class LogMessage(
        public val level: Level,
        public val logger: String,
        public val data: JsonElement,
    ): McpNotification
}