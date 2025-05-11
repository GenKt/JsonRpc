package io.modelcontextprotocol.kotlin.sdk

import io.github.stream29.streamlin.DelegatingSerializer
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*

internal object JsonRpcMessageSerializer :
    JsonContentPolymorphicSerializer<JsonRpcMessage>(JsonRpcMessage::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<JsonRpcMessage> {
        val jsonObject = if (element is JsonArray) {
            if (element.isEmpty())
                return JsonRpcClientMessageBatch.serializer()
            else
                element.first().jsonObject
        } else element.jsonObject
        return when {
            !jsonObject.contains("method") -> JsonRpcSuccessResponse.serializer()
            jsonObject.contains("id") -> JsonRpcRequest.serializer()
            else -> JsonRpcNotification.serializer()
        }
    }
}

internal object JsonRpcClientMessageSerializer :
    JsonContentPolymorphicSerializer<JsonRpcClientMessage>(JsonRpcClientMessage::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<JsonRpcClientMessage> {
        if (element is JsonArray)
            return JsonRpcClientMessageBatch.serializer()
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
        if (element is JsonArray)
            return JsonRpcServerMessageBatch.serializer()
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