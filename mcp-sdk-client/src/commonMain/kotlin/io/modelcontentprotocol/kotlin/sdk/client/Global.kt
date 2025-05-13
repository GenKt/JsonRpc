package io.modelcontentprotocol.kotlin.sdk.client

import io.modelcontextprotocol.kotlin.sdk.JSONRPCMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

internal const val LIB_VERSION: String = "TODO"

internal const val IMPLEMENTATION_NAME = "mcp-ktor"

internal const val MCP_SUBPROTOCOL = "mcp"

internal val EmptyJsonObject = JsonObject(emptyMap())

@OptIn(ExperimentalSerializationApi::class)
@PublishedApi
internal val McpJson: Json by lazy {
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        classDiscriminatorMode = ClassDiscriminatorMode.NONE
        explicitNulls = false
    }
}

internal fun serializeMessage(message: JSONRPCMessage): String {
    return McpJson.encodeToString(message) + "\n"
}