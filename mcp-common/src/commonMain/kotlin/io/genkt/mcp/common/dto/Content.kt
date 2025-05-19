package io.genkt.mcp.common.dto

import kotlinx.serialization.Serializable

public sealed interface McpContent {
    @Serializable(with = McpResourceContentSerializer::class)
    public sealed interface Resource {
        public val uri: String
        public val mimeType: String?

        @Serializable
        public data class Text(
            override val uri: String,
            override val mimeType: String? = null,
            val text: String,
        ) : Resource

        @Serializable
        public data class Blob(
            override val uri: String,
            override val mimeType: String? = null,
            val blob: String,
        ) : Resource
    }

    @Serializable(with = McpPromptContentSerializer::class)
    public sealed interface Prompt
    @Serializable(with = McpSamplingContentSerializer::class)
    public sealed interface Sampling

    @Serializable
    public data class Text(
        public val type: String = "text",
        public val text: String,
        public val annotations: Annotations? = null,
    ) : Prompt, Sampling

    @Serializable
    public data class Image(
        public val type: String = "image",
        public val data: String,
        public val mimeType: String,
        public val annotations: Annotations? = null,
    ) : Prompt, Sampling

    @Serializable
    public data class Audio(
        public val type: String = "audio",
        public val data: String,
        public val mimeType: String,
        public val annotations: Annotations? = null,
    ) : Prompt, Sampling

    @Serializable
    public data class EmbeddedResource(
        public val type: String = "resource",
        public val resource: Resource,
        public val annotations: Annotations? = null,
    ) : Prompt

    @Serializable
    public data class Annotations(
        public val audience: List<Role>? = null,
        public val priority: Double? = null,
    )

    @Suppress("EnumEntryName")
    @Serializable
    public enum class Role {
        user, assistant
    }
}
