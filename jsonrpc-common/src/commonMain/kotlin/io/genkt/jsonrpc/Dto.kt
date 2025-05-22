package io.genkt.jsonrpc

import io.github.stream29.streamlin.globalCached
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.jvm.JvmInline

/**
 * Base interface for all JSON-RPC messages.
 */
@Serializable(with = JsonRpcMessageSerializer::class)
public sealed interface JsonRpcMessage

/**
 * Base interface for all JSON-RPC client messages.
 */
@Serializable(with = JsonRpcClientMessageSerializer::class)
public sealed interface JsonRpcClientMessage : JsonRpcMessage

/**
 * Base interface for all JSON-RPC server messages.
 */
@Serializable(with = JsonRpcServerMessageSerializer::class)
public sealed interface JsonRpcServerMessage : JsonRpcMessage

/**
 * Base interface for all single JSON-RPC client messages (non-batch).
 */
@Serializable(with = JsonRpcClientSingleMessageSerializer::class)
public sealed interface JsonRpcClientSingleMessage : JsonRpcClientMessage {
    /** The method name to be invoked. */
    public val method: String
    /** The parameters to be used for the method. */
    public val params: JsonElement?
    /** The JSON-RPC version string. */
    public val jsonrpc: String
}

/**
 * Base interface for all single JSON-RPC server messages (non-batch).
 */
@Serializable(with = JsonRpcServerSingleMessageSerializer::class)
public sealed interface JsonRpcServerSingleMessage : JsonRpcServerMessage {
    /** The request ID. */
    public val id: RequestId
    /** The JSON-RPC version string. */
    public val jsonrpc: String
}

/**
 * Represents a batch of JSON-RPC server messages.
 * @property messages The list of server messages.
 */
@Serializable
@JvmInline
public value class JsonRpcServerMessageBatch(
    public val messages: List<JsonRpcServerSingleMessage>,
) : JsonRpcServerMessage

/**
 * Represents a batch of JSON-RPC client messages.
 * @property messages The list of client messages.
 */
@Serializable
@JvmInline
public value class JsonRpcClientMessageBatch(
    public val messages: List<JsonRpcClientSingleMessage>,
) : JsonRpcClientMessage

/**
 * Represents a JSON-RPC client call. For the `execute`-`call` pattern.
 * @param R The type of the expected response.
 */
public sealed interface JsonRpcClientCall<R>

/**
 * Represents a JSON-RPC request.
 * @property id The request ID.
 * @property method The method name to be invoked.
 * @property params The parameters to be used for the method.
 * @property jsonrpc The JSON-RPC version string.
 */
@Serializable
public data class JsonRpcRequest(
    public val id: RequestId,
    public override val method: String,
    public override val params: JsonElement? = null,
    public override val jsonrpc: String = JsonRpc.VERSION,
) : JsonRpcClientSingleMessage, JsonRpcClientCall<JsonRpcSuccessResponse>

/**
 * Represents a JSON-RPC notification.
 * @property method The method name to be invoked.
 * @property params The parameters to be used for the method.
 * @property jsonrpc The JSON-RPC version string.
 */
@Serializable
public data class JsonRpcNotification(
    public override val method: String,
    public override val params: JsonElement? = null,
    public override val jsonrpc: String = JsonRpc.VERSION,
) : JsonRpcClientSingleMessage, JsonRpcClientCall<Unit>

/**
 * Represents a successful JSON-RPC response.
 * @property id The request ID.
 * @property jsonrpc The JSON-RPC version string.
 * @property result The result of the method invocation.
 */
@Serializable
public data class JsonRpcSuccessResponse(
    public override val id: RequestId,
    public override val jsonrpc: String = JsonRpc.VERSION,
    public val result: JsonElement,
) : JsonRpcServerSingleMessage

/**
 * Represents a failed JSON-RPC response.
 * @property id The request ID.
 * @property error The error object.
 * @property jsonrpc The JSON-RPC version string.
 */
@Serializable
public data class JsonRpcFailResponse(
    public override val id: RequestId,
    public val error: Error,
    public override val jsonrpc: String = JsonRpc.VERSION,
) : JsonRpcServerSingleMessage {
    /**
     * Represents a JSON-RPC error object.
     * @property code The error code.
     * @property message A human-readable description of the error.
     * @property data Additional data associated with the error.
     */
    @Serializable
    public data class Error(
        public val code: Code,
        public val message: String,
        public val data: JsonElement = JsonObject.Empty,
    ) {
        /**
         * Represents a JSON-RPC error code.
         * @property value The integer value of the error code.
         */
        @Serializable
        @JvmInline
        public value class Code(public val value: Int) {
            /** Companion object for [Code]. */
            public companion object
        }
    }
}

/**
 * Represents a JSON-RPC request ID.
 * It can be a string, a number, or null.
 */
@Serializable(with = RequestIdSerializer::class)
public sealed interface RequestId {
    /**
     * Represents a string request ID.
     * @property value The string value of the ID.
     */
    @Serializable
    @JvmInline
    public value class StringId(public val value: String) : RequestId

    /**
     * Represents a numeric request ID.
     * @property value The numeric value of the ID.
     */
    @Serializable
    @JvmInline
    public value class NumberId(public val value: Long) : RequestId

    /**
     * Represents a null request ID.
     */
    @Serializable(with = NullIdSerializer::class)
    public object NullId : RequestId
}

/** Standard JSON-RPC error code for when a connection is closed. */
public val JsonRpcFailResponse.Error.Code.Companion.ConnectionClosed: JsonRpcFailResponse.Error.Code by globalCached {
    JsonRpcFailResponse.Error.Code(-1)
}
/** Standard JSON-RPC error code for when a request times out. */
public val JsonRpcFailResponse.Error.Code.Companion.RequestTimeout: JsonRpcFailResponse.Error.Code by globalCached {
    JsonRpcFailResponse.Error.Code(-2)
}
/** Standard JSON-RPC error code for when a parse error occurs. */
public val JsonRpcFailResponse.Error.Code.Companion.ParseError: JsonRpcFailResponse.Error.Code by globalCached {
    JsonRpcFailResponse.Error.Code(-32700)
}
/** Standard JSON-RPC error code for when a request is invalid. */
public val JsonRpcFailResponse.Error.Code.Companion.InvalidRequest: JsonRpcFailResponse.Error.Code by globalCached {
    JsonRpcFailResponse.Error.Code(-32600)
}
/** Standard JSON-RPC error code for when a method is not found. */
public val JsonRpcFailResponse.Error.Code.Companion.MethodNotFound: JsonRpcFailResponse.Error.Code by globalCached {
    JsonRpcFailResponse.Error.Code(-32601)
}
/** Standard JSON-RPC error code for when parameters are invalid. */
public val JsonRpcFailResponse.Error.Code.Companion.InvalidParams: JsonRpcFailResponse.Error.Code by globalCached {
    JsonRpcFailResponse.Error.Code(-32602)
}
/** Standard JSON-RPC error code for when an internal error occurs. */
public val JsonRpcFailResponse.Error.Code.Companion.InternalError: JsonRpcFailResponse.Error.Code by globalCached {
    JsonRpcFailResponse.Error.Code(-32603)
}

/**
 * Creates a custom JSON-RPC error code.
 * @param code The integer value of the custom error code.
 * @return A [JsonRpcFailResponse.Error.Code] instance.
 */
@Suppress("FunctionName")
public fun JsonRpcFailResponse.Error.Code.Companion.Custom(code: Int): JsonRpcFailResponse.Error.Code =
    JsonRpcFailResponse.Error.Code(code)
