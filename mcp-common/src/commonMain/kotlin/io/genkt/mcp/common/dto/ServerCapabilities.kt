package io.genkt.mcp.common.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.jvm.JvmInline

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

@Serializable
public data class McpResource(
    public val uri: String,
    public val name: String,
    public val description: String? = null,
    public val mimeType: String? = null,
    public val size: Long? = null,
) {
    @Serializable
    public data class ListRequest(
        public val cursor: String? = null
    )

    @Serializable
    public data class ListResponse(
        public val resources: List<McpResource>,
        public val nextCursor: String? = null,
    )

    @Serializable
    public data class ReadRequest(
        public val uri: String,
    )

    @Serializable
    public data class ReadResponse(
        public val contents: List<Content>,
    )

    @Serializable(with = McpResourceContentSerializer::class)
    public sealed interface Content {
        public val uri: String
        public val mimeType: String

        @Serializable
        public data class Text(
            override val uri: String,
            override val mimeType: String,
            val text: String,
        ): Content

        @Serializable
        public data class Binary(
            override val uri: String,
            override val mimeType: String,
            val blob: String,
        ): Content
    }

    @Serializable
    public data class Template(
        public val uriTemplate: String,
        public val name: String,
        public val description: String,
        public val mimeType: String,
    )

    @Serializable
    public data class ListTemplateResponse(
        public val resourceTemplates: List<Template>,
    )

    @Serializable
    public data class SubscribeRequest(
        public val uri: String,
    )

    @Serializable
    public data class SubscribeNotification(
        public val uri: String,
    )
}

@Serializable
public data class McpTool(
    public val name: String,
    public val description: String,
    public val inputSchema: Input,
    public val annotations: JsonObject? = null,
) {
    @Serializable
    public data class Input(
        public val properties: JsonObject,
        public val required: List<String>
    )

    @Serializable
    public data class ListRequest(
        public val cursor: String? = null
    )

    @Serializable
    public data class ListResponse(
        public val tools: List<McpTool>,
        public val nextCursor: String? = null,
    )

    @Serializable
    public data class CallRequest(
        public val name: String,
        public val arguments: Map<String, String>
    )

    @Serializable
    public data class CallResponse(
        public val content: List<Content>,
    )

    @Serializable(with = McpToolContentSerializer::class)
    public sealed interface Content {
        @Serializable(with = McpToolTextContentSerializer::class)
        @JvmInline
        public value class Text(
            public val text: String,
        ): Content
        @Serializable(with = McpToolImageContentSerializer::class)
        public data class Image(
            public val data: String,
            public val mimeType: String,
        ): Content
        @Serializable(with = McpToolAudioContentSerializer::class)
        public data class Audio(
            public val data: String,
            public val mimeType: String,
        ): Content
        @Serializable(with = McpToolResourceContentSerializer::class)
        public data class Resource(
            public val uri: String,
            public val mimeType: String,
            public val text: String,
        ): Content
    }
}