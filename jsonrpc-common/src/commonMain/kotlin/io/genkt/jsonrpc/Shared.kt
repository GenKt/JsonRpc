package io.genkt.jsonrpc

import io.github.stream29.streamlin.globalCached
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/** An empty [JsonObject]. */
public val JsonObject.Companion.Empty: JsonObject by globalCached { JsonObject(mapOf()) }

/**
 * Contains constants and utility functions for JSON-RPC.
 */
public object JsonRpc {
    /** The JSON-RPC version string ("2.0"). */
    public const val VERSION: String = "2.0"
    /**
     * The default [Json] instance used for JSON-RPC serialization and deserialization.
     * It is configured to ignore unknown keys, be lenient, encode defaults, and not pretty print.
     */
    public val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    /**
     * Creates a generator function for numeric request IDs.
     * The generator is thread-safe.
     * @param from The starting value for the ID generator (inclusive).
     * @param delta The increment value for the ID generator.
     * @return A function that returns a new [RequestId.NumberId] on each invocation.
     */
    @OptIn(ExperimentalAtomicApi::class)
    @Suppress("FunctionName")
    public fun NumberIdGenerator(from: Long = 0L, delta: Long = 1L): () -> RequestId.NumberId {
        val id = AtomicLong(from)
        return { RequestId.NumberId(id.fetchAndAdd(delta)) }
    }
}