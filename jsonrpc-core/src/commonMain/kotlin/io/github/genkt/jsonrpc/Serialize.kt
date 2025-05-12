package io.github.genkt.jsonrpc

import io.github.stream29.streamlin.DelegatingSerializer
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*

internal object JsonRpcMessageSerializer :
    JsonContentPolymorphicSerializer<JsonRpcMessage>(JsonRpcMessage::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<JsonRpcMessage> {
        if (element is JsonArray) {
            if (element.isEmpty())
                return JsonRpcClientMessageBatch.serializer()
            return if (element.first().jsonObject.contains("method")) JsonRpcClientMessageBatch.serializer()
            else JsonRpcServerMessageBatch.serializer()
        }
        val jsonObject = element.jsonObject
        return when {
            jsonObject.contains("method") -> JsonRpcClientSingleMessageSerializer
            else -> JsonRpcServerSingleMessageSerializer
        }
    }
}

internal object JsonRpcClientMessageSerializer :
    JsonContentPolymorphicSerializer<JsonRpcClientMessage>(JsonRpcClientMessage::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<JsonRpcClientMessage> {
        return if (element is JsonArray) JsonRpcClientMessageBatch.serializer()
        else JsonRpcClientSingleMessageSerializer
    }
}

internal object JsonRpcClientSingleMessageSerializer :
    JsonContentPolymorphicSerializer<JsonRpcClientSingleMessage>(JsonRpcClientSingleMessage::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<JsonRpcClientSingleMessage> {
        val jsonObject = element.jsonObject
        return when {
            jsonObject.contains("id") -> JsonRpcRequest.serializer()
            else -> JsonRpcNotification.serializer()
        }
    }
}

internal object JsonRpcServerMessageSerializer :
    JsonContentPolymorphicSerializer<JsonRpcServerMessage>(JsonRpcServerMessage::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<JsonRpcServerMessage> {
        return if (element is JsonArray) JsonRpcServerMessageBatch.serializer()
        else JsonRpcServerSingleMessageSerializer
    }
}

internal object JsonRpcServerSingleMessageSerializer :
    JsonContentPolymorphicSerializer<JsonRpcServerSingleMessage>(JsonRpcServerSingleMessage::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<JsonRpcServerSingleMessage> {
        val jsonObject = element.jsonObject
        return when {
            jsonObject.contains("result") -> JsonRpcSuccessResponse.serializer()
            jsonObject.contains("error") -> JsonRpcFailResponse.serializer()
            else -> throw IllegalArgumentException("Invalid JSON-RPC message: $jsonObject")
        }
    }
}

internal object RequestIdSerializer :
    JsonContentPolymorphicSerializer<RequestId>(RequestId::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<RequestId> {
        val jsonPrimitive = element.jsonPrimitive
        return when {
            jsonPrimitive.isString -> RequestId.StringId.serializer()
            jsonPrimitive is JsonNull -> NullIdSerializer
            else -> RequestId.NumberId.serializer()
        }
    }
}

internal object NullIdSerializer :
    KSerializer<RequestId.NullId> by
    DelegatingSerializer(
        JsonNull.serializer(),
        { RequestId.NullId },
        { JsonNull }
    )