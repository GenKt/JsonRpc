package io.genkt.mcp.common.dto

import io.genkt.jsonrpc.JsonRpc
import io.genkt.jsonrpc.JsonRpcNotification
import io.genkt.jsonrpc.JsonRpcRequest
import io.genkt.mcp.common.McpMethods
import io.github.stream29.streamlin.safeCast
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.long

public fun McpClientRequest.Companion.of(request: JsonRpcRequest): McpClientRequest<*>? {
    val method = request.method
    val deserializer: DeserializationStrategy<McpClientRequest<*>> = when (method) {
        McpMethods.Tools.Call -> McpTool.CallRequest.serializer()
        else -> return null
    }
    return JsonRpc.json.decodeFromJsonElement(deserializer, request.params ?: error("JsonRpc param is null"))
}

public fun McpServerRequest.Companion.of(request: JsonRpcRequest): McpServerRequest<*>? {
    val progressToken = request.params
        ?.safeCast<JsonObject>()
        ?.get("_meta")
        .safeCast<JsonObject>()
        ?.get("progressToken")
        ?.safeCast<JsonPrimitive>()
    val requestNoProgress = parseMcpServerRequestNoProgress(request.method, request.params)
        ?: return null
    if (progressToken != null) {
        return McpProgress.ServerRequest(
            requestNoProgress,
            if (progressToken.isString) McpProgress.Token.StringToken(progressToken.content)
            else McpProgress.Token.IntegerToken(progressToken.long),
        )
    }
    return requestNoProgress
}

private fun parseMcpServerRequestNoProgress(method: String, params: JsonElement?): McpServerRequest<*>? {
    val deserializer: DeserializationStrategy<McpServerRequest<*>> =
        when (method) {
            McpMethods.Roots.List -> McpRoot.ListRequest.serializer()
            McpMethods.Sampling.CreateMessage -> McpSampling.CreateMessageRequest.serializer()
            McpMethods.Ping -> return McpUtilities.Ping
            else -> return null
        }
    return JsonRpc.json.decodeFromJsonElement(deserializer, params ?: error("JsonRpc param is null"))
}

public fun McpServerNotification.Companion.of(notification: JsonRpcNotification): McpServerNotification? {
    val method = notification.method
    val deserializer: DeserializationStrategy<McpServerNotification> = when (method) {
        McpMethods.Notifications.Cancelled -> McpUtilities.Cancellation.serializer()
        McpMethods.Notifications.Prompts.ListChanged -> McpPrompt.ListChangedNotification.serializer()
        McpMethods.Notifications.Resources.ListChanged -> McpResource.ListChangedNotification.serializer()
        McpMethods.Notifications.Resources.Updated -> McpResource.UpdatedNotification.serializer()
        McpMethods.Notifications.Tools.ListChanged -> McpTool.ListChangedNotification.serializer()
        McpMethods.Notifications.Message -> McpLogging.LogMessage.serializer()
        McpMethods.Notifications.Progress -> McpProgress.Notification.serializer()
        else -> return null
    }
    return JsonRpc.json.decodeFromJsonElement(deserializer, notification.params ?: error("JsonRpc param is null"))
}