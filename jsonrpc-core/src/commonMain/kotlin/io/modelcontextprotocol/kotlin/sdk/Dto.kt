package io.modelcontextprotocol.kotlin.sdk

import io.github.stream29.streamlin.globalCached
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.jvm.JvmInline

@Serializable(with = JsonRpcMessageSerializer::class)
public sealed interface JsonRpcMessage

@Serializable(with = JsonRpcSendMessageSerializer::class)
public sealed interface JsonRpcSendMessage : JsonRpcMessage {
    public val jsonrpc: String
    public val method: String
}

@Serializable(with = JsonRpcReceiveMessageSerializer::class)
public sealed interface JsonRpcReceiveMessage : JsonRpcMessage {
    public val jsonrpc: String
    public val id: RequestId
    public val success: Boolean
}

@Serializable
public data class JsonRpcRequest(
    public val id: RequestId,
    public override val method: String,
    public val params: JsonElement = JsonObject.Empty,
    public override val jsonrpc: String = JsonRpc.VERSION,
) : JsonRpcSendMessage

@Serializable
public data class JsonRpcNotification(
    public override val method: String,
    public val params: JsonElement = JsonObject.Empty,
    public override val jsonrpc: String = JsonRpc.VERSION,
) : JsonRpcSendMessage

@Serializable
public class JsonRpcSuccessResponse(
    public override val id: RequestId,
    public override val jsonrpc: String = JsonRpc.VERSION,
    public val result: JsonElement,
) : JsonRpcReceiveMessage {
    override val success: Boolean
        get() = true
}

@Serializable
public class JsonRpcFailResponse(
    public override val id: RequestId,
    public val error: Error,
    public override val jsonrpc: String = JsonRpc.VERSION,
) : JsonRpcReceiveMessage {
    override val success: Boolean
        get() = false
    @Serializable
    public data class Error(
        public val code: Code,
        public val message: String,
        public val data: JsonElement = JsonObject.Empty,
    ) {
        @Serializable
        @JvmInline
        public value class Code(public val value: Int) {
            public companion object
        }
    }
}

@Serializable(with = RequestIdSerializer::class)
public sealed interface RequestId {
    @Serializable
    @JvmInline
    public value class StringId(public val value: String) : RequestId

    @Serializable
    @JvmInline
    public value class NumberId(public val value: Long) : RequestId

    @Serializable(with = NullIdSerializer::class)
    public object NullId : RequestId
}

public val JsonRpcFailResponse.Error.Code.Companion.ConnectionClosed: JsonRpcFailResponse.Error.Code by globalCached {
    JsonRpcFailResponse.Error.Code(-1)
}
public val JsonRpcFailResponse.Error.Code.Companion.RequestTimeout: JsonRpcFailResponse.Error.Code by globalCached {
    JsonRpcFailResponse.Error.Code(-2)
}
public val JsonRpcFailResponse.Error.Code.Companion.ParseError: JsonRpcFailResponse.Error.Code by globalCached {
    JsonRpcFailResponse.Error.Code(-32700)
}
public val JsonRpcFailResponse.Error.Code.Companion.InvalidRequest: JsonRpcFailResponse.Error.Code by globalCached {
    JsonRpcFailResponse.Error.Code(-32600)
}
public val JsonRpcFailResponse.Error.Code.Companion.MethodNotFound: JsonRpcFailResponse.Error.Code by globalCached {
    JsonRpcFailResponse.Error.Code(-32601)
}
public val JsonRpcFailResponse.Error.Code.Companion.InvalidParams: JsonRpcFailResponse.Error.Code by globalCached {
    JsonRpcFailResponse.Error.Code(-32602)
}
public val JsonRpcFailResponse.Error.Code.Companion.InternalError: JsonRpcFailResponse.Error.Code by globalCached {
    JsonRpcFailResponse.Error.Code(-32603)
}

@Suppress("FunctionName")
public fun JsonRpcFailResponse.Error.Code.Companion.Custom(code: Int): JsonRpcFailResponse.Error.Code = JsonRpcFailResponse.Error.Code(code)
