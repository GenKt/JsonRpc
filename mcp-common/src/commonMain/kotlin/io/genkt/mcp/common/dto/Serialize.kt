package io.genkt.mcp.common.dto

import io.genkt.mcp.common.McpMethods
import io.genkt.serialization.json.JsonPolymorphicSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

public fun <Request, Result> McpClientRawRequest<Request, Result>.serializer(): KSerializer<McpClientRawRequest<Request, Result>>
        where Request : McpClientBasicRequest<Result> {
    return ComposedSerializer(
        component1Serializer = request.serializer(),
        component2Serializer = MetaContainer.serializer(McpClientRawRequest.Meta.serializer()),
        descriptor = request.serializer().descriptor,
        compose = { request, meta -> McpClientRawRequest(request, meta.meta) },
        decompose = { (request, meta) -> request to MetaContainer(meta) }
    )
}

public fun McpClientRawRequest.Companion.serializerOf(method: String): KSerializer<McpClientRawRequest<*, *>>? {
    val requestSerializer = McpClientBasicRequest.serializerOf(method)
    return ComposedSerializer(
        component1Serializer = (requestSerializer ?: return null) as KSerializer<McpClientBasicRequest<*>>,
        component2Serializer = MetaContainer.serializer(McpClientRawRequest.Meta.serializer()),
        descriptor = requestSerializer.descriptor,
        compose = { request, meta -> McpClientRawRequest(request, meta.meta) },
        decompose = { (request, meta) -> request to MetaContainer(meta) }
    )
}

public fun McpClientRawNotification.serializer(): KSerializer<McpClientRawNotification> {
    return ComposedSerializer(
        component1Serializer = notification.serializer(),
        component2Serializer = MetaContainer.serializer(McpClientRawNotification.Meta.serializer()),
        descriptor = notification.serializer().descriptor,
        compose = { notification, meta -> McpClientRawNotification(notification, meta.meta) },
        decompose = { (notification, meta) -> notification to MetaContainer(meta) }
    )
}

public fun McpClientRawNotification.Companion.serializerOf(method: String): KSerializer<McpClientRawNotification>? {
    val notificationSerializer = McpClientBasicNotification.serializerOf(method)
    return ComposedSerializer(
        component1Serializer = (notificationSerializer ?: return null) as KSerializer<McpClientBasicNotification>,
        component2Serializer = MetaContainer.serializer(McpClientRawNotification.Meta.serializer()),
        descriptor = notificationSerializer.descriptor,
        compose = { notification, meta -> McpClientRawNotification(notification, meta.meta) },
        decompose = { (notification, meta) -> notification to MetaContainer(meta) }
    )
}

public fun <Request, Result> McpServerRawRequest<Request, Result>.serializer(): KSerializer<McpServerRawRequest<Request, Result>>
        where Request : McpServerBasicRequest<Result> {
    return ComposedSerializer(
        component1Serializer = request.serializer(),
        component2Serializer = MetaContainer.serializer(McpServerRawRequest.Meta.serializer()),
        descriptor = request.serializer().descriptor,
        compose = { request, meta -> McpServerRawRequest(request, meta.meta) },
        decompose = { (request, meta) -> request to MetaContainer(meta) }
    )
}

public fun McpServerRawRequest.Companion.serializerOf(method: String): KSerializer<McpServerRawRequest<*, *>>? {
    val requestSerializer = McpServerBasicRequest.serializerOf(method)
    return ComposedSerializer(
        component1Serializer = (requestSerializer ?: return null) as KSerializer<McpServerBasicRequest<*>>,
        component2Serializer = MetaContainer.serializer(McpServerRawRequest.Meta.serializer()),
        descriptor = requestSerializer.descriptor,
        compose = { request, meta -> McpServerRawRequest(request, meta.meta) },
        decompose = { (request, meta) -> request to MetaContainer(meta) }
    )
}

public fun McpServerRawNotification.serializer(): KSerializer<McpServerRawNotification> {
    return ComposedSerializer(
        component1Serializer = notification.serializer(),
        component2Serializer = MetaContainer.serializer(McpServerRawNotification.Meta.serializer()),
        descriptor = notification.serializer().descriptor,
        compose = { notification, meta -> McpServerRawNotification(notification, meta.meta) },
        decompose = { (notification, meta) -> notification to MetaContainer(meta) }
    )
}

public fun McpServerRawNotification.Companion.serializerOf(method: String): KSerializer<McpServerRawNotification>? {
    val notificationSerializer = McpServerBasicNotification.serializerOf(method)
    return ComposedSerializer(
        component1Serializer = (notificationSerializer ?: return null) as KSerializer<McpServerBasicNotification>,
        component2Serializer = MetaContainer.serializer(McpServerRawNotification.Meta.serializer()),
        descriptor = notificationSerializer.descriptor,
        compose = { notification, meta -> McpServerRawNotification(notification, meta.meta) },
        decompose = { (notification, meta) -> notification to MetaContainer(meta) }
    )
}

@Serializable
internal class MetaContainer<T>(
    @SerialName("_meta")
    val meta: T?,
)


internal class ComposedSerializer<T, T1, T2>(
    private val component1Serializer: KSerializer<T1>,
    private val component2Serializer: KSerializer<T2>,
    override val descriptor: SerialDescriptor,
    private val compose: (T1, T2) -> T,
    private val decompose: (T) -> Pair<T1, T2>,
) : KSerializer<T> {
    override fun serialize(encoder: Encoder, value: T) {
        val (component1Value, component2Value) = decompose(value)
        encoder.encodeStructure(descriptor) {
            val compositeDecoder = this
            val mockEncoder = MockEncoder(encoder, compositeDecoder)
            component1Serializer.serialize(mockEncoder, component1Value)
            component2Serializer.serialize(mockEncoder, component2Value)
        }
    }

    override fun deserialize(decoder: Decoder): T {
        return decoder.decodeStructure(descriptor) {
            val compositeDecoder = this
            val mockDecoder = MockDecoder(decoder, compositeDecoder)
            val component1Value = component1Serializer.deserialize(mockDecoder)
            val component2Value = component2Serializer.deserialize(mockDecoder)
            compose(component1Value, component2Value)
        }
    }

    internal class MockDecoder(
        val decoder: Decoder,
        val compositeDecoder: CompositeDecoder
    ) : Decoder by decoder {
        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            MockCompositeDecoder(compositeDecoder)
    }

    internal class MockEncoder(
        val encoder: Encoder,
        val compositeEncoder: CompositeEncoder
    ) : Encoder by encoder {
        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
            MockCompositeEncoder(compositeEncoder)
    }

    internal class MockCompositeEncoder(
        val compositeEncoder: CompositeEncoder
    ) : CompositeEncoder by compositeEncoder {
        override fun endStructure(descriptor: SerialDescriptor) {}
    }

    internal class MockCompositeDecoder(
        val compositeDecoder: CompositeDecoder
    ) : CompositeDecoder by compositeDecoder {
        override fun endStructure(descriptor: SerialDescriptor) {}
    }
}

@Suppress("unchecked_cast")
public fun <T : McpClientBasicRequest<*>> T.serializer(): KSerializer<T> =
    when (this) {
        is McpTool.CallRequest -> McpTool.CallRequest.serializer()
        is McpPrompt.GetRequest -> McpPrompt.GetRequest.serializer()
        is McpInit.InitializeRequest -> McpInit.InitializeRequest.serializer()
        is McpPrompt.ListRequest -> McpPrompt.ListRequest.serializer()
        is McpResource.ListRequest -> McpResource.ListRequest.serializer()
        is McpTool.ListRequest -> McpTool.ListRequest.serializer()
        is McpResource.ListTemplateRequest -> McpResource.ListTemplateRequest.serializer()
        is McpUtilities.Ping -> McpUtilities.Ping.serializer()
        is McpResource.ReadRequest -> McpResource.ReadRequest.serializer()
        is McpCompletion.Request -> McpCompletion.Request.serializer()
        is McpLogging.SetLevelRequest -> McpLogging.SetLevelRequest.serializer()
        is McpResource.SubscribeRequest -> McpResource.SubscribeRequest.serializer()
        is McpResource.UnsubscribeRequest -> McpResource.UnsubscribeRequest.serializer()
    } as KSerializer<T>

public fun McpClientBasicRequest.Companion.serializerOf(method: String): KSerializer<out McpClientBasicRequest<*>>? =
    when (method) {
        McpMethods.Tools.Call -> McpTool.CallRequest.serializer()
        McpMethods.Prompts.Get -> McpPrompt.GetRequest.serializer()
        McpMethods.Initialize -> McpInit.InitializeRequest.serializer()
        McpMethods.Prompts.List -> McpPrompt.ListRequest.serializer()
        McpMethods.Resources.List -> McpResource.ListRequest.serializer()
        McpMethods.Tools.List -> McpTool.ListRequest.serializer()
        McpMethods.Resources.Templates.List -> McpResource.ListTemplateRequest.serializer()
        McpMethods.Ping -> McpUtilities.Ping.serializer()
        McpMethods.Resources.Read -> McpResource.ReadRequest.serializer()
        McpMethods.Completion.Complete -> McpCompletion.Request.serializer()
        McpMethods.Logging.SetLevel -> McpLogging.SetLevelRequest.serializer()
        McpMethods.Resources.Subscribe -> McpResource.SubscribeRequest.serializer()
        McpMethods.Resources.Unsubscribe -> McpResource.UnsubscribeRequest.serializer()
        else -> null
    }

@Suppress("unchecked_cast")
public fun <T : McpClientBasicNotification> T.serializer(): KSerializer<T> =
    when (this) {
        is McpUtilities.Cancellation -> McpUtilities.Cancellation.serializer()
        is McpInit.InitializedNotification -> McpInit.InitializedNotification.serializer()
        is McpRoot.ListChangedNotification -> McpRoot.ListChangedNotification.serializer()
        is McpProgress.Notification -> McpProgress.Notification.serializer()
    } as KSerializer<T>

public fun McpClientBasicNotification.Companion.serializerOf(method: String): KSerializer<out McpClientBasicNotification>? =
    when (method) {
        McpMethods.Notifications.Cancelled -> McpUtilities.Cancellation.serializer()
        McpMethods.Notifications.Initialized -> McpInit.InitializedNotification.serializer()
        McpMethods.Notifications.Roots.ListChanged -> McpRoot.ListChangedNotification.serializer()
        McpMethods.Notifications.Progress -> McpProgress.Notification.serializer()
        else -> null
    }

@Suppress("unchecked_cast")
public fun <T : McpServerBasicRequest<*>> T.serializer(): KSerializer<T> =
    when (this) {
        is McpRoot.ListRequest -> McpRoot.ListRequest.serializer()
        is McpSampling.CreateMessageRequest -> McpSampling.CreateMessageRequest.serializer()
        is McpUtilities.Ping -> McpUtilities.Ping.serializer()
    } as KSerializer<T>

public fun McpServerBasicRequest.Companion.serializerOf(method: String): KSerializer<out McpServerBasicRequest<*>>? =
    when (method) {
        McpMethods.Roots.List -> McpRoot.ListRequest.serializer()
        McpMethods.Sampling.CreateMessage -> McpSampling.CreateMessageRequest.serializer()
        McpMethods.Ping -> McpUtilities.Ping.serializer()
        else -> null
    }

@Suppress("unchecked_cast")
public fun <T : McpServerBasicNotification> T.serializer(): KSerializer<T> =
    when (this) {
        is McpUtilities.Cancellation -> McpUtilities.Cancellation.serializer()
        is McpPrompt.ListChangedNotification -> McpPrompt.ListChangedNotification.serializer()
        is McpResource.ListChangedNotification -> McpResource.ListChangedNotification.serializer()
        is McpTool.ListChangedNotification -> McpTool.ListChangedNotification.serializer()
        is McpLogging.LogMessage -> McpLogging.LogMessage.serializer()
        is McpProgress.Notification -> McpProgress.Notification.serializer()
        is McpResource.UpdatedNotification -> McpResource.UpdatedNotification.serializer()
    } as KSerializer<T>

public fun McpServerBasicNotification.Companion.serializerOf(method: String): KSerializer<out McpServerBasicNotification>? =
    when (method) {
        McpMethods.Notifications.Cancelled -> McpUtilities.Cancellation.serializer()
        McpMethods.Notifications.Prompts.ListChanged -> McpPrompt.ListChangedNotification.serializer()
        McpMethods.Notifications.Resources.ListChanged -> McpResource.ListChangedNotification.serializer()
        McpMethods.Notifications.Tools.ListChanged -> McpTool.ListChangedNotification.serializer()
        McpMethods.Notifications.Message -> McpLogging.LogMessage.serializer()
        McpMethods.Notifications.Progress -> McpProgress.Notification.serializer()
        McpMethods.Notifications.Resources.Updated -> McpResource.UpdatedNotification.serializer()
        else -> null
    }


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
        jsonElement.checkJsonObjectOrThrow { "Invalid McpContent.Resource: $jsonElement" }
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
        jsonElement.checkJsonObjectOrThrow { "Invalid McpContent.Prompt: $jsonElement" }
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
        jsonElement.checkJsonObjectOrThrow { "Invalid McpContent.Sampling: $jsonElement" }
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
        jsonElement.checkJsonObjectOrThrow { "Invalid McpCompletion.Reference: $jsonElement" }
        when (jsonElement["type"]?.jsonPrimitive?.content) {
            "ref/resource" -> McpCompletion.Reference.Resource.serializer()
            "ref/prompt" -> McpCompletion.Reference.Prompt.serializer()
            else -> throw IllegalArgumentException("Unknown McpCompletion.Reference: $jsonElement")
        }
    }
)

internal object McpProgressTokenSerializer
    : KSerializer<McpProgress.Token> by JsonPolymorphicSerializer(
    serialName = "io.genkt.mcp.common.dto.McpProgress.Token",
    childSerializers = listOf(
        McpProgress.Token.StringToken.serializer(),
        McpProgress.Token.IntegerToken.serializer(),
    ),
    selectSerializer = { token ->
        when (token) {
            is McpProgress.Token.StringToken -> McpProgress.Token.StringToken.serializer()
            is McpProgress.Token.IntegerToken -> McpProgress.Token.IntegerToken.serializer()
        }
    },
    selectDeserializer = { jsonElement ->
        jsonElement.checkJsonPrimitiveOrThrow { "Invalid McpProgress.Token: $jsonElement" }
        when {
            jsonElement.isString -> McpProgress.Token.StringToken.serializer()
            jsonElement.longOrNull != null -> McpProgress.Token.IntegerToken.serializer()
            else -> throw IllegalArgumentException("Unknown McpProgress.Token: $jsonElement")
        }
    }
)

@OptIn(ExperimentalContracts::class)
private fun JsonElement.checkJsonObjectOrThrow(message: JsonElement.() -> String = { "Invalid call: $this" }) {
    contract { returns() implies (this@checkJsonObjectOrThrow is JsonObject) }
    this as? JsonObject ?: throw IllegalArgumentException(message())
}

@OptIn(ExperimentalContracts::class)
private fun JsonElement.checkJsonPrimitiveOrThrow(message: JsonElement.() -> String = { "Invalid call: $this" }) {
    contract { returns() implies (this@checkJsonPrimitiveOrThrow is JsonPrimitive) }
    this as? JsonPrimitive ?: throw IllegalArgumentException(message())
}