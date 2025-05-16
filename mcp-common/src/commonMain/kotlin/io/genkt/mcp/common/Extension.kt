package io.genkt.mcp.common

import io.genkt.jsonrpc.JsonRpcRequest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

public fun JsonRpcRequest.withProgressToken(token: String): JsonRpcRequest {
    if (params == null || params !is JsonObject) error("params must be JsonObject")
    val newParams = buildJsonObject {
        putJsonObject("_meta") {
            put("progressToken", token)
        }
        params?.jsonObject?.entries?.forEach { (key, value) -> put(key, value) }
    }
    return copy(params = newParams)
}