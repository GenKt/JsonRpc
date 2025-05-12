package io.github.genkt.mcp.common.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

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
}