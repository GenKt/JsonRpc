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
 * This serializer allows for selecting specific serializers at runtime based on the instance being
 * serialized or the JSON structure being deserialized.
 *
 * @property serialName The `serialName` for [T], used to build the [descriptor].
 *
 * @property childSerializers A list of [KSerializer]s for all possible concrete subtypes of [T].
 * Their `serialName`s must be unique.
 * These serializers will only be used to build the [descriptor] for this serializer.
 *
 * @property selectSerializer Take an instance of [T] and
 * returns the appropriate [SerializationStrategy] (should be one of the [childSerializers]) for serializing that instance.
 *
 * @property selectDeserializer Take a [JsonElement] (the raw JSON being deserialized)
 * and returns the appropriate [DeserializationStrategy] (should be one of the [childSerializers])
 * for deserializing that JSON element into an instance of [T].
 *
 * @property annotations [SerialInfo] annotations, used to build the [descriptor].
 *
 * @throws IllegalStateException if [childSerializers] contain serializers with duplicate `serialName`s.
 */
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
     *
     * @throws ClassCastException if [selectSerializer] returns a [KSerializer] incompatible with [value].
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