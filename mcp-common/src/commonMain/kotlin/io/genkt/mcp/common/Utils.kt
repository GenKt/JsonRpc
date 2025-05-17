package io.genkt.mcp.common

import io.genkt.jsonrpc.JsonRpcRequest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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

@OptIn(ExperimentalTime::class)
public val defaultProgressTokenGenerator: () -> String = {
    val timestamp = Clock.System.now().toEpochMilliseconds()

    val random1 = Random.nextInt(0, 0xFFFF).toString(16).padStart(4, '0')
    val random2 = Random.nextInt(0, 0xFFFF).toString(16).padStart(4, '0')
    val random3 = Random.nextInt(0, 0xFFFF).toString(16).padStart(4, '0')
    val random4 = Random.nextInt(0, 0xFFFF).toString(16).padStart(4, '0')
    val random5 = Random.nextInt(0, 0xFFFF).toString(16).padStart(4, '0')

    val timestampHex = timestamp.toString(16)

    "prog-$timestampHex-$random1-$random2-$random3-$random4-$random5"
}