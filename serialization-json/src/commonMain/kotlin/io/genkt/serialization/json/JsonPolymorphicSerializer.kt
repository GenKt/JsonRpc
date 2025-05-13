package io.genkt.serialization.json

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement

public class JsonPolymorphicSerializer<T>(
    public override val descriptor: SerialDescriptor,
    public val selectSerializer: (T) -> SerializationStrategy<*>,
    public val selectDeserializer: (JsonElement) -> DeserializationStrategy<T>,
) : KSerializer<T> {
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