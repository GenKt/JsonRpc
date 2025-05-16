package io.genkt.jsonrpc

import io.github.stream29.streamlin.globalCached
import kotlinx.atomicfu.atomic
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

public val JsonObject.Companion.Empty: JsonObject by globalCached { JsonObject(mapOf()) }

public object JsonRpc {
    public const val VERSION: String = "2.0"
    public val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }
    @Suppress("FunctionName")
    public fun NumberIdGenerator(from: Long = 0L, delta: Long = 1L): () -> RequestId.NumberId {
        val id = atomic(from)
        return { RequestId.NumberId(id.getAndAdd(delta)) }
    }
}