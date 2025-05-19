package io.genkt.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
public class JsonPolymorphicSerializer<T>(
    public val serialName: String,
    public val childSerializers: List<KSerializer<out T>>,
    public val selectSerializer: (T) -> SerializationStrategy<*>,
    public val selectDeserializer: (JsonElement) -> DeserializationStrategy<T>,
    public val annotations: List<Annotation> = emptyList(),
) : KSerializer<T> {
    override val descriptor: SerialDescriptor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        check(childSerializers.asSequence().map { it.descriptor.serialName }.distinct().count() == childSerializers.size) {
            "Serializers for polymorphic class '$serialName' have duplicate names: ${childSerializers.map { it.descriptor.serialName }}"
        }
        buildSerialDescriptor(serialName, PolymorphicKind.SEALED) {
            element("type", String.serializer().descriptor)
            val elementDescriptor =
                buildSerialDescriptor("kotlinx.serialization.Sealed<$serialName>", SerialKind.CONTEXTUAL) {
                    childSerializers.forEach {
                        element(it.descriptor.serialName, it.descriptor)
                    }
                }
            element("value", elementDescriptor)
            annotations = this@JsonPolymorphicSerializer.annotations
        }
    }

    @Suppress("unchecked_cast")
    override fun serialize(encoder: Encoder, value: T) {
        val serializer = selectSerializer(value) as SerializationStrategy<T>
        serializer.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): T {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw IllegalArgumentException("This serializer can be used only with Json format")
        val element = jsonDecoder.decodeJsonElement()
        val deserializer = selectDeserializer(element)
        return jsonDecoder.json.decodeFromJsonElement(deserializer, element)
    }
}