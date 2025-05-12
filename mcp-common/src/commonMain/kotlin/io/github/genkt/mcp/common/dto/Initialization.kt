package io.github.genkt.mcp.common.dto

import io.github.genkt.mcp.common.McpConstants
import kotlinx.serialization.Serializable

@Serializable
public data class McpClientInitRequest(
    public val capabilities: McpClientCapabilities,
    public val clientInfo: McpClientInfo,
    public val protocolVersion: String = McpConstants.ProtocolVersion,
)

@Serializable
public data class McpServerInitResponse(
    public val capabilities: McpServerCapabilities,
    public val serverInfo: McpServerInfo,
    public val instructions: String,
    public val protocolVersion: String = McpConstants.ProtocolVersion,
)

@Serializable
public data class McpServerInfo(
    public val name: String,
    public val version: String,
)

@Serializable
public data class McpClientInfo(
    public val name: String,
    public val version: String,
)