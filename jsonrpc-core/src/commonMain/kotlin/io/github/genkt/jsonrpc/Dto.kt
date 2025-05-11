package io.github.genkt.jsonrpc

import io.github.stream29.streamlin.globalCached
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.jvm.JvmInline

@Serializable(with = JsonRpcMessageSerializer::class)
public sealed interface JsonRpcMessage

@Serializable(with = JsonRpcClientMessageSerializer::class)
public sealed interface JsonRpcClientMessage : JsonRpcMessage

@Serializable(with = JsonRpcServerMessageSerializer::class)
public sealed interface JsonRpcServerMessage : JsonRpcMessage

@Serializable(with = JsonRpcClientSingleMessageSerializer::class)
public sealed interface JsonRpcClientSingleMessage : JsonRpcClientMessage

@Serializable(with = JsonRpcServerSingleMessageSerializer::class)
public sealed interface JsonRpcServerSingleMessage : JsonRpcServerMessage

@Serializable
@JvmInline
public value class JsonRpcServerMessageBatch(
    public val messages: List<JsonRpcServerSingleMessage>,
) : JsonRpcServerMessage

@Serializable
@JvmInline
public value class JsonRpcClientMessageBatch(
    public val messages: List<JsonRpcClientSingleMessage>,
) : JsonRpcClientMessage

@Serializable
public data class JsonRpcRequest(
    public val id: RequestId,
    public val method: String,
    public val params: JsonElement = JsonObject.Empty,
    public val jsonrpc: String = JsonRpc.VERSION,
) : JsonRpcClientSingleMessage

@Serializable
public data class JsonRpcNotification(
    public val method: String,
    public val params: JsonElement = JsonObject.Empty,
    public val jsonrpc: String = JsonRpc.VERSION,
) : JsonRpcClientSingleMessage

@Serializable
public class JsonRpcSuccessResponse(
    public val id: RequestId,
    public val jsonrpc: String = JsonRpc.VERSION,
    public val result: JsonElement,
) : JsonRpcServerSingleMessage

@Serializable
public class JsonRpcFailResponse(
    public val id: RequestId,
    public val error: Error,
    public val jsonrpc: String = JsonRpc.VERSION,
) : JsonRpcServerSingleMessage {
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
public fun JsonRpcFailResponse.Error.Code.Companion.Custom(code: Int): JsonRpcFailResponse.Error.Code =
    JsonRpcFailResponse.Error.Code(code)
