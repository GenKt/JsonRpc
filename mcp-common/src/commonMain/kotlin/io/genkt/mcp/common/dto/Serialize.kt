@file:OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)

package io.genkt.mcp.common.dto

import io.genkt.serialization.json.JsonPolymorphicSerializer
import io.github.stream29.streamlin.DelegatingSerializer
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.json.*

internal object McpSamplingContentSerializer :
    KSerializer<McpContent> by JsonPolymorphicSerializer(
        buildSerialDescriptor("io.github.genkt.mcp.common.dto.McpContent", PolymorphicKind.SEALED),
        {
            when (it) {
                is McpContent.Text -> McpSamplingTextContentSerializer
                is McpContent.Image -> McpSamplingImageContentSerializer
                is McpContent.Audio -> McpSamplingAudioContentSerializer
                is McpContent.Resource -> error("Resource not supported in McpSampling")
            }
        },
        {
            when (it.jsonObject["type"]?.jsonPrimitive?.content) {
                "text" -> McpSamplingTextContentSerializer
                "image" -> McpSamplingImageContentSerializer
                "audio" -> McpSamplingAudioContentSerializer
                else -> error("Unknown type: $it")
            }
        }
    )

internal object McpSamplingTextContentSerializer :
    KSerializer<McpContent.Text> by DelegatingSerializer(
        delegate = McpTextContentDelegate.serializer(),
        fromDelegate = { McpContent.Text(it.text) },
        toDelegate = { McpTextContentDelegate(text = it.text) },
    )

internal object McpSamplingImageContentSerializer :
    KSerializer<McpContent.Image> by DelegatingSerializer(
        delegate = McpImageContentDelegate.serializer(),
        fromDelegate = { McpContent.Image(it.data, it.mimeType) },
        toDelegate = { McpImageContentDelegate(data = it.data, mimeType = it.mimeType) },
    )

internal object McpSamplingAudioContentSerializer :
    KSerializer<McpContent.Audio> by DelegatingSerializer(
        delegate = McpAudioContentDelegate.serializer(),
        fromDelegate = { McpContent.Audio(it.data, it.mimeType) },
        toDelegate = { McpAudioContentDelegate(data = it.data, mimeType = it.mimeType) },
    )

internal object McpPromptContentSerializer :
    KSerializer<McpContent> by JsonPolymorphicSerializer(
        buildSerialDescriptor("io.github.genkt.mcp.common.dto.McpContent", PolymorphicKind.SEALED),
        {
            when (it) {
                is McpContent.Text -> McpPromptTextContentSerializer
                is McpContent.Image -> McpPromptImageContentSerializer
                is McpContent.Audio -> McpPromptAudioContentSerializer
                is McpContent.Resource -> McpPromptResourceContentSerializer
                else -> error("Unknown type: $it")
            }
        },
        {
            when (it.jsonObject["type"]?.jsonPrimitive?.content) {
                "text" -> McpPromptTextContentSerializer
                "image" -> McpPromptImageContentSerializer
                "audio" -> McpPromptAudioContentSerializer
                "resource" -> McpPromptResourceContentSerializer
                else -> error("Unknown type: $it")
            }
        }
    )

internal object McpPromptTextContentSerializer :
    KSerializer<McpContent.Text> by DelegatingSerializer(
        delegate = McpTextContentDelegate.serializer(),
        fromDelegate = { McpContent.Text(it.text) },
        toDelegate = { McpTextContentDelegate(text = it.text) },
    )

internal object McpPromptImageContentSerializer :
    KSerializer<McpContent.Image> by DelegatingSerializer(
        delegate = McpImageContentDelegate.serializer(),
        fromDelegate = { McpContent.Image(it.data, it.mimeType) },
        toDelegate = { McpImageContentDelegate(data = it.data, mimeType = it.mimeType) },
    )

internal object McpPromptAudioContentSerializer :
    KSerializer<McpContent.Audio> by DelegatingSerializer(
        delegate = McpAudioContentDelegate.serializer(),
        fromDelegate = { McpContent.Audio(it.data, it.mimeType) },
        toDelegate = { McpAudioContentDelegate(data = it.data, mimeType = it.mimeType) },
    )

internal object McpPromptResourceContentSerializer :
    KSerializer<McpContent.Resource> by DelegatingSerializer(
        delegate = McpResourceContentDelegate.serializer(),
        fromDelegate = { McpContent.Resource(it.resource.uri, it.resource.mimeType, it.resource.text) },
        toDelegate = { McpResourceContentDelegate(it.uri, it.mimeType, it.text) },
    )

internal object McpResourceContentSerializer :
    KSerializer<McpResource.Content> by JsonPolymorphicSerializer(
        buildSerialDescriptor("io.github.genkt.mcp.common.dto.McpResource.Content", PolymorphicKind.SEALED),
        {
            when (it) {
                is McpResource.Content.Text -> McpResource.Content.Text.serializer()
                is McpResource.Content.Binary -> McpResource.Content.Binary.serializer()
                else -> error("Unknown type: $it")
            }
        },
        {
            if (it.jsonObject["text"] != null) {
                McpResource.Content.Text.serializer()
            } else if (it.jsonObject["blob"] != null) {
                McpResource.Content.Binary.serializer()
            } else {
                error("Unknown type: $it")
            }
        }
    )

internal object McpToolContentSerializer :
    KSerializer<McpContent> by JsonPolymorphicSerializer(
        buildSerialDescriptor("io.github.genkt.mcp.common.dto.McpContent", PolymorphicKind.SEALED),
        {
            when (it) {
                is McpContent.Text -> McpToolTextContentSerializer
                is McpContent.Image -> McpToolImageContentSerializer
                is McpContent.Audio -> McpToolAudioContentSerializer
                is McpContent.Resource -> McpToolResourceContentSerializer
                else -> error("Unknown type: $it")
            }
        },
        {
            when (it.jsonObject["type"]?.jsonPrimitive?.content) {
                "text" -> McpToolTextContentSerializer
                "image" -> McpToolImageContentSerializer
                "audio" -> McpToolAudioContentSerializer
                "resource" -> McpToolResourceContentSerializer
                else -> error("Unknown type: $it")
            }
        }
    )

internal object McpToolTextContentSerializer :
    KSerializer<McpContent.Text> by DelegatingSerializer(
        delegate = McpTextContentDelegate.serializer(),
        fromDelegate = { McpContent.Text(it.text) },
        toDelegate = { McpTextContentDelegate(text = it.text) },
    )

internal object McpToolImageContentSerializer :
    KSerializer<McpContent.Image> by DelegatingSerializer(
        delegate = McpImageContentDelegate.serializer(),
        fromDelegate = { McpContent.Image(it.data, it.mimeType) },
        toDelegate = { McpImageContentDelegate(data = it.data, mimeType = it.mimeType) },
    )

internal object McpToolAudioContentSerializer :
    KSerializer<McpContent.Audio> by DelegatingSerializer(
        delegate = McpAudioContentDelegate.serializer(),
        fromDelegate = { McpContent.Audio(it.data, it.mimeType) },
        toDelegate = { McpAudioContentDelegate(data = it.data, mimeType = it.mimeType) },
    )

internal object McpToolResourceContentSerializer :
    KSerializer<McpContent.Resource> by DelegatingSerializer(
        delegate = McpResourceContentDelegate.serializer(),
        fromDelegate = { McpContent.Resource(it.resource.uri, it.resource.mimeType, it.resource.text) },
        toDelegate = { McpResourceContentDelegate(it.uri, it.mimeType, it.text) },
    )

internal object McpCompletionReferenceSerializer :
    KSerializer<McpCompletion.Reference> by JsonPolymorphicSerializer(
        buildSerialDescriptor("io.github.genkt.mcp.common.dto.McpCompletion.Reference", PolymorphicKind.SEALED),
        {
            when (it) {
                is McpCompletion.Reference.Prompt -> McpCompletionPromptReferenceSerializer
                is McpCompletion.Reference.Resource -> McpCompletionResourceReferenceSerializer
                else -> error("Unknown type: $it")
            }
        },
        {
            when (it.jsonObject["type"]?.jsonPrimitive?.content) {
                "ref/prompt" -> McpCompletionPromptReferenceSerializer
                "ref/resource" -> McpCompletionResourceReferenceSerializer
                else -> error("Unknown type: $it")
            }
        }
    )

internal object McpCompletionPromptReferenceSerializer :
    KSerializer<McpCompletion.Reference.Prompt> by DelegatingSerializer(
        delegate = McpPromptReferenceDelegate.serializer(),
        fromDelegate = { McpCompletion.Reference.Prompt(it.name) },
        toDelegate = { McpPromptReferenceDelegate(name = it.name) },
    )

internal object McpCompletionResourceReferenceSerializer :
    KSerializer<McpCompletion.Reference.Resource> by DelegatingSerializer(
        delegate = McpResourceReferenceDelegate.serializer(),
        fromDelegate = { McpCompletion.Reference.Resource(it.uri) },
        toDelegate = { McpResourceReferenceDelegate(uri = it.uri) },
    )

@Serializable
internal data class McpTextContentDelegate(
    val type: String = "text",
    val text: String,
)

@Serializable
internal data class McpImageContentDelegate(
    val type: String = "image",
    val data: String,
    val mimeType: String,
)

@Serializable
internal data class McpAudioContentDelegate(
    val type: String = "audio",
    val data: String,
    val mimeType: String,
)

@Serializable
internal data class McpResourceContentDelegate(
    val type: String = "resource",
    val resource: Resource,
) {
    constructor(
        uri: String,
        mimeType: String,
        text: String,
    ) : this(
        resource = Resource(uri, mimeType, text)
    )

    @Serializable
    internal data class Resource(
        val uri: String,
        val mimeType: String,
        val text: String,
    )
}

@Serializable
internal data class McpPromptReferenceDelegate(
    val type: String = "ref/prompt",
    val name: String,
)

@Serializable
internal data class McpResourceReferenceDelegate(
    val type: String = "ref/resource",
    val uri: String,
)