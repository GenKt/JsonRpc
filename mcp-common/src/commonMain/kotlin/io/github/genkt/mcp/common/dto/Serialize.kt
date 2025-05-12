@file:OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)

package io.github.genkt.mcp.common.dto

import io.github.genkt.serialization.json.JsonPolymorphicSerializer
import io.github.stream29.streamlin.DelegatingSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.json.jsonObject

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
        fromDelegate = { McpSampling.Content.Text(text) },
        toDelegate = { McpTextContentDelegate(text = text) },
    )

internal object McpSamplingImageContentSerializer :
    KSerializer<McpSampling.Content.Image> by DelegatingSerializer(
        delegate = McpImageContentDelegate.serializer(),
        fromDelegate = { McpSampling.Content.Image(data, mimeType) },
        toDelegate = { McpImageContentDelegate(data = data, mimeType = mimeType) },
    )

internal object McpSamplingAudioContentSerializer :
    KSerializer<McpSampling.Content.Audio> by DelegatingSerializer(
        delegate = McpAudioContentDelegate.serializer(),
        fromDelegate = { McpSampling.Content.Audio(data, mimeType) },
        toDelegate = { McpAudioContentDelegate(data = data, mimeType = mimeType) },
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