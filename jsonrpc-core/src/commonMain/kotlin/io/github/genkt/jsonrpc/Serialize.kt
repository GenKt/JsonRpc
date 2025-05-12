@file:OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)

package io.github.genkt.jsonrpc

import io.github.genkt.serialization.json.JsonPolymorphicSerializer
import io.github.stream29.streamlin.DelegatingSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object JsonRpcMessageSerializer :
    KSerializer<JsonRpcMessage> by JsonPolymorphicSerializer(
        buildSerialDescriptor("io.github.genkt.jsonrpc.JsonRpcMessage", PolymorphicKind.SEALED),
        {
            when (it) {
                is JsonRpcClientMessage -> JsonRpcClientMessageSerializer
                is JsonRpcServerMessage -> JsonRpcServerMessageSerializer
            }
        },
        { element ->
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
        buildSerialDescriptor("io.github.genkt.jsonrpc.JsonRpcClientMessage", PolymorphicKind.SEALED),
        {
            when (it) {
                is JsonRpcClientSingleMessage -> JsonRpcClientSingleMessageSerializer
                is JsonRpcClientMessageBatch -> JsonRpcClientMessageBatch.serializer()
            }
        },
        { element ->
            if (element is JsonArray) JsonRpcClientMessageBatch.serializer()
            else JsonRpcClientSingleMessageSerializer
        }
    )

internal object JsonRpcClientSingleMessageSerializer :
    KSerializer<JsonRpcClientSingleMessage> by JsonPolymorphicSerializer(
        buildSerialDescriptor("io.github.genkt.jsonrpc.JsonRpcClientSingleMessage", PolymorphicKind.SEALED),
        {
            when (it) {
                is JsonRpcRequest -> JsonRpcRequest.serializer()
                is JsonRpcNotification -> JsonRpcNotification.serializer()
            }
        },
        { element ->
            when {
                element.jsonObject.contains("id") -> JsonRpcRequest.serializer()
                else -> JsonRpcNotification.serializer()
            }
        }
    )

internal object JsonRpcServerMessageSerializer :
    KSerializer<JsonRpcServerMessage> by JsonPolymorphicSerializer(
        buildSerialDescriptor("io.github.genkt.jsonrpc.JsonRpcServerMessage", PolymorphicKind.SEALED),
        {
            when (it) {
                is JsonRpcServerSingleMessage -> JsonRpcServerSingleMessageSerializer
                is JsonRpcServerMessageBatch -> JsonRpcServerMessageBatch.serializer()
            }
        },
        { element ->
            if (element is JsonArray) JsonRpcServerMessageBatch.serializer()
            else JsonRpcServerSingleMessageSerializer
        }
    )

internal object JsonRpcServerSingleMessageSerializer :
    KSerializer<JsonRpcServerSingleMessage> by JsonPolymorphicSerializer(
        buildSerialDescriptor("io.github.genkt.jsonrpc.JsonRpcServerSingleMessage", PolymorphicKind.SEALED),
        {
            when (it) {
                is JsonRpcSuccessResponse -> JsonRpcSuccessResponse.serializer()
                is JsonRpcFailResponse -> JsonRpcFailResponse.serializer()
            }
        },
        { element ->
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
        buildSerialDescriptor("io.github.genkt.jsonrpc.RequestId", PolymorphicKind.SEALED),
        {
            when (it) {
                is RequestId.NumberId -> RequestId.NumberId.serializer()
                is RequestId.StringId -> RequestId.StringId.serializer()
                is RequestId.NullId -> NullIdSerializer
            }
        },
        { element ->
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