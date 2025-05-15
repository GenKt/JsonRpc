package io.genkt.mcp.common.dto

import io.genkt.mcp.common.McpConstants
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

public sealed interface McpInit {
    @Serializable
    public data class Request(
        public val capabilities: ClientCapabilities,
        public val clientInfo: ClientInfo,
        public val protocolVersion: String = McpConstants.ProtocolVersion,
    )

    @Serializable
    public data class Response(
        public val capabilities: ServerCapabilities,
        public val serverInfo: ServerInfo,
        public val instructions: String,
        public val protocolVersion: String = McpConstants.ProtocolVersion,
    )

    @Serializable
    public data class ServerInfo(
        public val name: String,
        public val version: String,
    )

    @Serializable
    public data class ClientInfo(
        public val name: String,
        public val version: String,
    )

    @Serializable
    public data class ClientCapabilities(
        public val sampling: JsonObject? = null,
        public val roots: RootsCapability? = null,
    ) {
        @Serializable
        public data class RootsCapability(
            public val listChanged: Boolean = false,
        )
    }

    @Serializable
    public data class ServerCapabilities(
        val experimental: JsonObject? = null,
        val logging: JsonObject? = null,
        val prompts: PromptsCapability? = null,
        val resources: ResourcesCapability? = null,
        val tools: ToolsCapability? = null,
        val completion: JsonObject? = null,
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
}