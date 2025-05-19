package io.genkt.mcp.common.dto

import io.genkt.serialization.json.JsonPolymorphicSerializer
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.putJsonObject


internal object McpResourceContentSerializer
    : KSerializer<McpContent.Resource> by JsonPolymorphicSerializer(
    serialName = "io.genkt.mcp.common.dto.McpContent.Resource",
    childSerializers = listOf(
        McpContent.Resource.Text.serializer(),
        McpContent.Resource.Blob.serializer(),
    ),
    selectSerializer = { resourceContent ->
        when (resourceContent) {
            is McpContent.Resource.Blob -> McpContent.Resource.Blob.serializer()
            is McpContent.Resource.Text -> McpContent.Resource.Text.serializer()
        }
    },
    selectDeserializer = { jsonElement ->
        jsonElement as? JsonObject ?: throw IllegalArgumentException("Invalid McpContent.Resource: $jsonElement")
        when {
            jsonElement.contains("text") -> McpContent.Resource.Text.serializer()
            jsonElement.contains("blob") -> McpContent.Resource.Blob.serializer()
            else -> throw IllegalArgumentException("Unknown McpContent.Resource: $jsonElement")
        }
    }
)

internal object McpPromptContentSerializer
    : KSerializer<McpContent.Prompt> by JsonPolymorphicSerializer(
    serialName = "io.genkt.mcp.common.dto.McpContent.Prompt",
    childSerializers = listOf(
        McpContent.Text.serializer(),
        McpContent.Image.serializer(),
        McpContent.Audio.serializer(),
    ),
    selectSerializer = { promptContent ->
        when (promptContent) {
            is McpContent.Text -> McpContent.Text.serializer()
            is McpContent.Image -> McpContent.Image.serializer()
            is McpContent.Audio -> McpContent.Audio.serializer()
            is McpContent.EmbeddedResource -> McpContent.EmbeddedResource.serializer()
        }
    },
    selectDeserializer = { jsonElement ->
        jsonElement as? JsonObject ?: throw IllegalArgumentException("Invalid McpContent.Prompt: $jsonElement")
        when (jsonElement["type"]?.jsonPrimitive?.content) {
            "text" -> McpContent.Text.serializer()
            "image" -> McpContent.Image.serializer()
            "audio" -> McpContent.Audio.serializer()
            "resource" -> McpContent.EmbeddedResource.serializer()
            else -> throw IllegalArgumentException("Unknown McpContent.Prompt: $jsonElement")
        }
    }
)

internal object McpSamplingContentSerializer
    : KSerializer<McpContent.Sampling> by JsonPolymorphicSerializer(
    serialName = "io.genkt.mcp.common.dto.McpContent.Sampling",
    childSerializers = listOf(
        McpContent.Text.serializer(),
        McpContent.Image.serializer(),
        McpContent.Audio.serializer(),
    ),
    selectSerializer = { samplingContent ->
        when (samplingContent) {
            is McpContent.Text -> McpContent.Text.serializer()
            is McpContent.Image -> McpContent.Image.serializer()
            is McpContent.Audio -> McpContent.Audio.serializer()
        }
    },
    selectDeserializer = { jsonElement ->
        jsonElement as? JsonObject ?: throw IllegalArgumentException("Invalid McpContent.Sampling: $jsonElement")
        when (jsonElement["type"]?.jsonPrimitive?.content) {
            "text" -> McpContent.Text.serializer()
            "image" -> McpContent.Image.serializer()
            "audio" -> McpContent.Audio.serializer()
            else -> throw IllegalArgumentException("Unknown McpContent.Sampling: $jsonElement")
        }
    }
)

internal object McpCompletionReferenceSerializer
    : KSerializer<McpCompletion.Reference> by JsonPolymorphicSerializer(
    serialName = "io.genkt.mcp.common.dto.McpCompletion.Reference",
    childSerializers = listOf(
        McpCompletion.Reference.Resource.serializer(),
        McpCompletion.Reference.Prompt.serializer(),
    ),
    selectSerializer = { completionReference ->
        when (completionReference) {
            is McpCompletion.Reference.Resource -> McpCompletion.Reference.Resource.serializer()
            is McpCompletion.Reference.Prompt -> McpCompletion.Reference.Prompt.serializer()
        }
    },
    selectDeserializer = { jsonElement ->
        jsonElement as? JsonObject ?: throw IllegalArgumentException("Invalid McpCompletion.Reference: $jsonElement")
        when (jsonElement["type"]?.jsonPrimitive?.content) {
            "ref/resource" -> McpCompletion.Reference.Resource.serializer()
            "ref/prompt" -> McpCompletion.Reference.Prompt.serializer()
            else -> throw IllegalArgumentException("Unknown McpCompletion.Reference: $jsonElement")
        }
    }
)

internal class McpClientProgressRequestSerializer<Result, Request : McpClientRequest<Result>>(
    resultSerializer: KSerializer<Result>,
    val requestSerializer: KSerializer<Request>,
) : KSerializer<McpProgress.ClientRequest<Result, Request>> {
    val rawRequestSerializer = McpProgress.RawClientRequest.serializer(resultSerializer, requestSerializer)
    override val descriptor: SerialDescriptor by requestSerializer::descriptor

    override fun serialize(
        encoder: Encoder,
        value: McpProgress.ClientRequest<Result, Request>
    ) {
        encoder.encodeSerializableValue(rawRequestSerializer, value.rawRequest)
    }

    override fun deserialize(decoder: Decoder): McpProgress.ClientRequest<Result, Request> {
        throw UnsupportedOperationException("You shouldn't deserialize McpProgress.ClientRequest. Deserialize McpProgress.RawClientRequest instead.")
    }
}

internal class McpServerProgressRequestSerializer<Result, Request : McpServerRequest<Result>>(
    resultSerializer: KSerializer<Result>,
    val requestSerializer: KSerializer<Request>,
) : KSerializer<McpProgress.ServerRequest<Result, Request>> {
    val rawRequestSerializer = McpProgress.RawServerRequest.serializer(resultSerializer, requestSerializer)
    override val descriptor: SerialDescriptor by requestSerializer::descriptor
    override fun serialize(encoder: Encoder, value: McpProgress.ServerRequest<Result, Request>) {
        encoder.encodeSerializableValue(rawRequestSerializer, value.rawRequest)
    }

    override fun deserialize(decoder: Decoder): McpProgress.ServerRequest<Result, Request> {
        throw UnsupportedOperationException("You shouldn't deserialize McpProgress.ServerRequest. Deserialize McpProgress.RawServerRequest instead.")
    }
}

internal class RawMcpClientProgressRequestSerializer<Result, Request : McpClientRequest<Result>>(
    resultSerializer: KSerializer<Result>,
    val requestSerializer: KSerializer<Request>,
) : KSerializer<McpProgress.RawClientRequest<Result, Request>> {
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor(
        "io.genkt.mcp.common.dto.McpProgress.RawClientRequest",
        SerialKind.CONTEXTUAL
    )

    override fun serialize(encoder: Encoder, value: McpProgress.RawClientRequest<Result, Request>) {
        encoder as? JsonEncoder
            ?: throw IllegalArgumentException("This serializer can be used only with Json format")
        val requestJson = encoder.json.encodeToJsonElement(requestSerializer, value.request).jsonObject
        val requestJsonWithProgressToken = buildJsonObject {
            putJsonObject("_meta") {
                put(
                    "progressToken",
                    encoder.json.encodeToJsonElement(
                        McpProgress.Token.serializer(),
                        value.token
                    )
                )
            }
            requestJson.forEach { (key, value) -> put(key, value) }
        }
        encoder.encodeJsonElement(requestJsonWithProgressToken)
    }

    override fun deserialize(decoder: Decoder): McpProgress.RawClientRequest<Result, Request> {
        decoder as? JsonDecoder
            ?: throw IllegalArgumentException("This serializer can be used only with Json format")
        val requestJson = decoder.decodeJsonElement().jsonObject
        val progressTokenJson = requestJson["_meta"]!!.jsonObject["progressToken"]!!
        val token = decoder.json.decodeFromJsonElement(McpProgress.Token.serializer(), progressTokenJson)
        val request = decoder.json.decodeFromJsonElement(requestSerializer, requestJson)
        return McpProgress.RawClientRequest(request, token)
    }
}