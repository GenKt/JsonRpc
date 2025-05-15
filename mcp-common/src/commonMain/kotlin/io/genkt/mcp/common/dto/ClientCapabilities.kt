package io.genkt.mcp.common.dto

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
public data class McpRoot(
    public val name: String,
    public val uri: String,
) {
    @Serializable
    public data class ListResponse(
        public val roots: List<McpRoot>,
    )
}

public sealed interface McpSampling {
    @Serializable
    public data class Request(
        public val messages: List<Message>,
        public val modelPreferences: ModelPreferences,
    )

    @Serializable
    public data class ModelPreferences(
        public val systemPrompt: String? = null,
        public val maxTokens: Long? = null,
    )

    @Serializable
    public data class Message(
        public val role: String,
        public val content: Content,
    )

    @Serializable
    public data class Response(
        public val role: String,
        public val content: Content,
        public val model: String,
        public val stopReason: String? = null,
    )

    @Serializable(with = McpSamplingContentSerializer::class)
    public sealed interface Content {
        @Serializable(with = McpSamplingTextContentSerializer::class)
        @JvmInline
        public value class Text(
            public val text: String,
        ) : Content

        @Serializable(with = McpSamplingImageContentSerializer::class)
        public data class Image(
            public val data: String,
            public val mimeType: String,
        ) : Content

        @Serializable(with = McpSamplingAudioContentSerializer::class)
        public data class Audio(
            public val data: String,
            public val mimeType: String,
        ) : Content
    }
}