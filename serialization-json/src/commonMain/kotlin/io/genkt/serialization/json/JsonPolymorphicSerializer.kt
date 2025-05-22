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

/**
 * A custom [KSerializer] for handling polymorphic serialization and deserialization in JSON.
 * This serializer allows for selecting specific serializers at runtime based on the object being
 * serialized or the JSON structure being deserialized.
 *
 * It is particularly useful when the type of an object cannot be determined statically or when
 * JSON payloads do not adhere to kotlinx.serialization's default polymorphic format (e.g., missing type discriminators
 * or using external criteria to determine the type).
 *
 * @param T The base type of the objects this serializer can handle.
 * @property serialName The unique name for this serializer, used in its [SerialDescriptor].
 * @property childSerializers A list of [KSerializer]s for all possible concrete subtypes of [T].
 *                            Their `serialName`s must be unique.
 * @property selectSerializer A lambda function that takes an instance of [T] and returns the appropriate
 *                            [SerializationStrategy] (usually one of the `childSerializers`) for serializing that instance.
 * @property selectDeserializer A lambda function that takes a [JsonElement] (the raw JSON being deserialized)
 *                              and returns the appropriate [DeserializationStrategy] (usually one of the `childSerializers`)
 *                              for deserializing that JSON element into an instance of [T].
 * @property annotations A list of annotations to be included in the [SerialDescriptor] of this serializer.
 *                       Defaults to an empty list.
 *
 * @throws IllegalArgumentException if used with a non-JSON format during deserialization.
 * @throws IllegalStateException if `childSerializers` contain serializers with duplicate `serialName`s.
 */
@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
public class JsonPolymorphicSerializer<T>(
    public val serialName: String,
    public val childSerializers: List<KSerializer<out T>>,
    public val selectSerializer: (T) -> SerializationStrategy<*>,
    public val selectDeserializer: (JsonElement) -> DeserializationStrategy<T>,
    public val annotations: List<Annotation> = emptyList(),
) : KSerializer<T> {
    /**
     * Describes the structure of the data handled by this serializer.
     * It's a `PolymorphicKind.SEALED` descriptor, reflecting that it can handle various subtypes.
     * The descriptor includes elements "type" (conceptually, though not strictly enforced in this custom serializer's output)
     * and "value", where "value" can be one of the `childSerializers`' descriptors.
     */
    override val descriptor: SerialDescriptor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        check(childSerializers.asSequence().map { it.descriptor.serialName }.distinct().count() == childSerializers.size) {
            "Serializers for polymorphic class '$serialName' have duplicate names: ${childSerializers.map { it.descriptor.serialName }}"
        }
        buildSerialDescriptor(serialName, PolymorphicKind.SEALED) {
            element("type", String.serializer().descriptor) // Conceptually represents the type discriminator
            val elementDescriptor =
                buildSerialDescriptor("kotlinx.serialization.Sealed<$serialName>", SerialKind.CONTEXTUAL) {
                    childSerializers.forEach {
                        // Element name is the serial name of the child serializer's descriptor
                        element(it.descriptor.serialName, it.descriptor)
                    }
                }
            element("value", elementDescriptor) // Represents the actual polymorphic value
            annotations = this@JsonPolymorphicSerializer.annotations
        }
    }

    /**
     * Serializes the given [value] using the [SerializationStrategy] selected by [selectSerializer].
     *
     * @param encoder The [Encoder] to write the serialized data to.
     * @param value The instance of [T] to serialize.
     */
    @Suppress("unchecked_cast")
    override fun serialize(encoder: Encoder, value: T) {
        val serializer = selectSerializer(value) as SerializationStrategy<T>
        serializer.serialize(encoder, value)
    }

    /**
     * Deserializes a [JsonElement] into an instance of [T] using the [DeserializationStrategy]
     * selected by [selectDeserializer].
     *
     * This method requires the [decoder] to be a [JsonDecoder].
     *
     * @param decoder The [Decoder] to read the serialized data from. Must be a [JsonDecoder].
     * @return The deserialized instance of [T].
     * @throws IllegalArgumentException if the [decoder] is not a [JsonDecoder].
     */
    override fun deserialize(decoder: Decoder): T {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw IllegalArgumentException("This serializer can be used only with Json format")
        val element = jsonDecoder.decodeJsonElement() // Decode the entire JSON structure for type selection
        val deserializer = selectDeserializer(element)
        // Delegate the actual deserialization to the selected child serializer
        return jsonDecoder.json.decodeFromJsonElement(deserializer, element)
    }
}