package io.modelcontextprotocol.kotlin.sdk

import io.github.stream29.streamlin.globalCached
import kotlinx.serialization.json.JsonObject

public val JsonObject.Companion.Empty: JsonObject by globalCached { JsonObject(mapOf()) }

public object JsonRpc {
    public const val VERSION: String = "2.0"
}