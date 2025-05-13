package io.github.genkt.mcp.common.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.jvm.JvmInline

@Serializable
public data class McpServerCapabilities(
    val experimental: JsonObject? = null,
    val sampling: JsonObject? = null,
    val logging: JsonObject? = null,
    val prompts: PromptsCapability? = null,
    val resources: ResourcesCapability? = null,
    val tools: ToolsCapability? = null,
) {
    @Serializable
    public data class PromptsCapability(
        val listChanged: Boolean = false,
    )

    @Serializable
    public data class ResourcesCapability(
        val subscribe: Boolean = false,
        val listChanged: Boolean = false,
    )

    @Serializable
    public data class ToolsCapability(
        val listChanged: Boolean = false,
    )
}

@Serializable
public data class McpPrompt(
    public val name: String,
    public val description: String,
    public val arguments: List<Argument>,
) {

    @Serializable
    public data class ListRequest(
        public val cursor: String? = null
    )

    @Serializable
    public data class ListResponse(
        public val prompts: List<McpPrompt>,
        public val nextCursor: String? = null,
    )

    @Serializable
    public data class GetRequest(
        public val name: String,
        public val arguments: Map<String, String>
    )

    @Serializable
    public data class Argument(
        public val name: String,
        public val description: String,
        public val required: Boolean
    )

    @Serializable
    public data class GetResponse(
        public val description: String,
        public val messages: List<Message>,
    )

    @Serializable
    public data class Message(
        public val role: String,
        public val content: Content,
    )

    @Serializable(with = McpPromptContentSerializer::class)
    public sealed interface Content {
        @Serializable(with = McpPromptTextContentSerializer::class)
        @JvmInline
        public value class Text(
            public val text: String,
        ): Content
        @Serializable(with = McpPromptImageContentSerializer::class)
        public data class Image(
            public val data: String,
            public val mimeType: String,
        ): Content
        @Serializable(with = McpPromptAudioContentSerializer::class)
        public data class Audio(
            public val data: String,
            public val mimeType: String,
        ): Content
        @Serializable(with = McpPromptResourceContentSerializer::class)
        public data class Resource(
            public val uri: String,
            public val mimeType: String,
            public val text: String,
        ): Content
    }
}