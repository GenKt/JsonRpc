@file:OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)

package io.genkt.jsonrpc

import io.genkt.serialization.json.JsonPolymorphicSerializer
import io.github.stream29.streamlin.DelegatingSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object JsonRpcMessageSerializer :
    KSerializer<JsonRpcMessage> by JsonPolymorphicSerializer(
        serialName = "io.github.genkt.jsonrpc.JsonRpcMessage",
        childSerializers = listOf(JsonRpcClientMessageSerializer, JsonRpcServerMessageSerializer),
        selectSerializer = {
            when (it) {
                is JsonRpcClientMessage -> JsonRpcClientMessageSerializer
                is JsonRpcServerMessage -> JsonRpcServerMessageSerializer
            }
        },
        selectDeserializer = { element ->
            if (element is JsonArray) {
                if (element.isEmpty())
                    return@JsonPolymorphicSerializer JsonRpcClientMessageBatch.serializer()
                return@JsonPolymorphicSerializer if (element.first().jsonObject.contains("method")) JsonRpcClientMessageBatch.serializer()
                else JsonRpcServerMessageBatch.serializer()
            }
            val jsonObject = element.jsonObject
            when {
                jsonObject.contains("method") -> JsonRpcClientSingleMessageSerializer
                else -> JsonRpcServerSingleMessageSerializer
            }
        }
    )

internal object JsonRpcClientMessageSerializer :
    KSerializer<JsonRpcClientMessage> by JsonPolymorphicSerializer(
        serialName = "io.github.genkt.jsonrpc.JsonRpcClientMessage",
        childSerializers = listOf(JsonRpcClientSingleMessageSerializer, JsonRpcClientMessageBatch.serializer()),
        selectSerializer = {
            when (it) {
                is JsonRpcClientSingleMessage -> JsonRpcClientSingleMessageSerializer
                is JsonRpcClientMessageBatch -> JsonRpcClientMessageBatch.serializer()
            }
        },
        selectDeserializer = { element ->
            if (element is JsonArray) JsonRpcClientMessageBatch.serializer()
            else JsonRpcClientSingleMessageSerializer
        }
    )

internal object JsonRpcClientSingleMessageSerializer :
    KSerializer<JsonRpcClientSingleMessage> by JsonPolymorphicSerializer(
        serialName = "io.github.genkt.jsonrpc.JsonRpcClientSingleMessage",
        childSerializers = listOf(JsonRpcRequest.serializer(), JsonRpcNotification.serializer()),
        selectSerializer = {
            when (it) {
                is JsonRpcRequest -> JsonRpcRequest.serializer()
                is JsonRpcNotification -> JsonRpcNotification.serializer()
            }
        },
        selectDeserializer = { element ->
            when {
                element.jsonObject.contains("id") -> JsonRpcRequest.serializer()
                else -> JsonRpcNotification.serializer()
            }
        }
    )

internal object JsonRpcServerMessageSerializer :
    KSerializer<JsonRpcServerMessage> by JsonPolymorphicSerializer(
        serialName = "io.github.genkt.jsonrpc.JsonRpcServerMessage",
        childSerializers = listOf(JsonRpcServerSingleMessageSerializer, JsonRpcServerMessageBatch.serializer()),
        selectSerializer = {
            when (it) {
                is JsonRpcServerSingleMessage -> JsonRpcServerSingleMessageSerializer
                is JsonRpcServerMessageBatch -> JsonRpcServerMessageBatch.serializer()
            }
        },
        selectDeserializer = { element ->
            if (element is JsonArray) JsonRpcServerMessageBatch.serializer()
            else JsonRpcServerSingleMessageSerializer
        }
    )

internal object JsonRpcServerSingleMessageSerializer :
    KSerializer<JsonRpcServerSingleMessage> by JsonPolymorphicSerializer(
        serialName = "io.github.genkt.jsonrpc.JsonRpcServerSingleMessage",
        childSerializers = listOf(JsonRpcSuccessResponse.serializer(), JsonRpcFailResponse.serializer()),
        selectSerializer = {
            when (it) {
                is JsonRpcSuccessResponse -> JsonRpcSuccessResponse.serializer()
                is JsonRpcFailResponse -> JsonRpcFailResponse.serializer()
            }
        },
        selectDeserializer = { element ->
            val jsonObject = element.jsonObject
            when {
                jsonObject.contains("result") -> JsonRpcSuccessResponse.serializer()
                jsonObject.contains("error") -> JsonRpcFailResponse.serializer()
                else -> throw IllegalArgumentException("Invalid JSON-RPC message: $jsonObject")
            }
        }
    )

internal object RequestIdSerializer :
    KSerializer<RequestId> by JsonPolymorphicSerializer(
        serialName = "io.github.genkt.jsonrpc.RequestId",
        childSerializers = listOf(RequestId.NumberId.serializer(), RequestId.StringId.serializer(), NullIdSerializer),
        selectSerializer = {
            when (it) {
                is RequestId.NumberId -> RequestId.NumberId.serializer()
                is RequestId.StringId -> RequestId.StringId.serializer()
                is RequestId.NullId -> NullIdSerializer
            }
        },
        selectDeserializer = { element ->
            val jsonPrimitive = element.jsonPrimitive
            when {
                jsonPrimitive.isString -> RequestId.StringId.serializer()
                jsonPrimitive is JsonNull -> NullIdSerializer
                else -> RequestId.NumberId.serializer()
            }
        }
    )

internal object NullIdSerializer :
    KSerializer<RequestId.NullId> by
    DelegatingSerializer(
        JsonNull.serializer(),
        { RequestId.NullId },
        { JsonNull }
    )