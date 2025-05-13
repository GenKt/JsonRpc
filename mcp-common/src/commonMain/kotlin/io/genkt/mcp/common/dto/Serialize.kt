@file:OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)

package io.genkt.mcp.common.dto

import io.genkt.serialization.json.JsonPolymorphicSerializer
import io.github.stream29.streamlin.DelegatingSerializer
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.json.*

internal object McpSamplingContentSerializer :
    KSerializer<McpSampling.Content> by JsonPolymorphicSerializer(
        buildSerialDescriptor("io.github.genkt.mcp.common.dto.McpSampling.Content", PolymorphicKind.SEALED),
        {
            when (it) {
                is McpSampling.Content.Text -> McpSamplingTextContentSerializer
                is McpSampling.Content.Image -> McpSamplingImageContentSerializer
                is McpSampling.Content.Audio -> McpSamplingAudioContentSerializer
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
    KSerializer<McpSampling.Content.Text> by DelegatingSerializer(
        delegate = McpTextContentDelegate.serializer(),
        fromDelegate = { McpSampling.Content.Text(it.text) },
        toDelegate = { McpTextContentDelegate(text = it.text) },
    )

internal object McpSamplingImageContentSerializer :
    KSerializer<McpSampling.Content.Image> by DelegatingSerializer(
        delegate = McpImageContentDelegate.serializer(),
        fromDelegate = { McpSampling.Content.Image(it.data, it.mimeType) },
        toDelegate = { McpImageContentDelegate(data = it.data, mimeType = it.mimeType) },
    )

internal object McpSamplingAudioContentSerializer :
    KSerializer<McpSampling.Content.Audio> by DelegatingSerializer(
        delegate = McpAudioContentDelegate.serializer(),
        fromDelegate = { McpSampling.Content.Audio(it.data, it.mimeType) },
        toDelegate = { McpAudioContentDelegate(data = it.data, mimeType = it.mimeType) },
    )

internal object McpPromptContentSerializer :
    KSerializer<McpPrompt.Content> by JsonPolymorphicSerializer(
        buildSerialDescriptor("io.github.genkt.mcp.common.dto.McpPrompt.Content", PolymorphicKind.SEALED),
        {
            when (it) {
                is McpPrompt.Content.Text -> McpPromptTextContentSerializer
                is McpPrompt.Content.Image -> McpPromptImageContentSerializer
                is McpPrompt.Content.Audio -> McpPromptAudioContentSerializer
                is McpPrompt.Content.Resource -> McpPromptResourceContentSerializer
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

internal object McpPromptTextContentSerializer :
    KSerializer<McpPrompt.Content.Text> by DelegatingSerializer(
        delegate = McpTextContentDelegate.serializer(),
        fromDelegate = { McpPrompt.Content.Text(it.text) },
        toDelegate = { McpTextContentDelegate(text = it.text) },
    )

internal object McpPromptImageContentSerializer :
    KSerializer<McpPrompt.Content.Image> by DelegatingSerializer(
        delegate = McpImageContentDelegate.serializer(),
        fromDelegate = { McpPrompt.Content.Image(it.data, it.mimeType) },
        toDelegate = { McpImageContentDelegate(data = it.data, mimeType = it.mimeType) },
    )

internal object McpPromptAudioContentSerializer :
    KSerializer<McpPrompt.Content.Audio> by DelegatingSerializer(
        delegate = McpAudioContentDelegate.serializer(),
        fromDelegate = { McpPrompt.Content.Audio(it.data, it.mimeType) },
        toDelegate = { McpAudioContentDelegate(data = it.data, mimeType = it.mimeType) },
    )

internal object McpPromptResourceContentSerializer :
    KSerializer<McpPrompt.Content.Resource> by DelegatingSerializer(
        delegate = McpResourceContentDelegate.serializer(),
        fromDelegate = { McpPrompt.Content.Resource(it.resource.uri, it.resource.mimeType, it.resource.text) },
        toDelegate = { McpResourceContentDelegate(it.uri, it.mimeType, it.text) },
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