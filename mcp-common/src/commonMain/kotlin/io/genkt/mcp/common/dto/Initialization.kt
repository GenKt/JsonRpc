package io.genkt.mcp.common.dto

import io.genkt.mcp.common.McpConstants
import io.genkt.mcp.common.McpMethods
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

public sealed interface McpInit {
    @Serializable
    public data class InitializeRequest(
        public val capabilities: ClientCapabilities,
        public val clientInfo: Implementation,
        public val protocolVersion: String = McpConstants.ProtocolVersion,
    ): McpClientRequest<InitializeResult> {
        override val method: String get() = McpMethods.Initialize
        override val resultSerializer: KSerializer<InitializeResult>
            get() = InitializeResult.serializer()
    }

    @Serializable
    public data class InitializeResult(
        public val capabilities: ServerCapabilities,
        public val serverInfo: Implementation,
        public val instructions: String? = null,
        public val protocolVersion: String = McpConstants.ProtocolVersion,
    )

    @Serializable
    public data object InitializedNotification: McpClientNotification {
        override val method: String get() = McpMethods.Notifications.Initialized
    }

    @Serializable
    public data class Implementation(
        public val name: String,
        public val version: String,
    )

    @Serializable
    public data class ClientCapabilities(
        public val experimental: JsonObject? = null,
        public val sampling: Sampling? = null,
        public val roots: RootsCapability? = null,
    ) {
        @Serializable
        public data class RootsCapability(
            public val listChanged: Boolean = false,
        )
        @Serializable
        public data object Sampling
    }

    @Serializable
    public data class ServerCapabilities(
        val experimental: JsonObject? = null,
        val logging: JsonObject? = null,
        val completions: JsonObject? = null,
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
}
