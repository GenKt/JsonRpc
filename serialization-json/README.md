# Serialization JSON Module

The `serialization-json` module provides custom serialization utilities for working with `kotlinx.serialization` in JSON contexts. Its primary offering is the `JsonPolymorphicSerializer`, a flexible tool for handling polymorphic types when the default mechanisms are insufficient.

## Key Features

- **Custom Polymorphic Serialization**: Introduces `JsonPolymorphicSerializer<T>` for advanced control over how different subtypes of a base type `T` are serialized and deserialized.
- **Runtime Serializer Selection**:
    - **Serialization**: Allows specifying a lambda (`selectSerializer`) that chooses the appropriate `SerializationStrategy` (e.g., a specific subtype's serializer) based on the actual instance being serialized.
    - **Deserialization**: Allows specifying a lambda (`selectDeserializer`) that inspects the raw `JsonElement` and chooses the appropriate `DeserializationStrategy` to convert it into a specific subtype.
- **JSON-Specific**: Designed to work exclusively with the `Json` format from `kotlinx.serialization`.
- **Flexible Type Handling**: Useful in scenarios where:
    - JSON payloads lack explicit type discriminators.
    - Type determination relies on the presence of specific fields or structural differences in the JSON.
    - Interfacing with external systems that have non-standard polymorphic JSON representations.

## Design Principles

- **Extend `kotlinx.serialization`**: Built on top of the standard Kotlin serialization library, aiming to supplement its features rather than replace them.
- **Flexibility over Convention**: Prioritizes providing developers with the tools to define their own polymorphic logic when conventions don't fit.
- **Explicit Control**: Gives the developer explicit control over how types are chosen during both serialization and deserialization.

## `JsonPolymorphicSerializer<T>`

This is the core component of the module. It's a `KSerializer<T>` that delegates the actual serialization and deserialization to one of its `childSerializers` based on custom logic.

### When to Use

- When you have a sealed interface or class hierarchy.
- When the JSON representation of these types doesn't include a standard type discriminator field (e.g., `@JsonClassDiscriminator`).
- When you need to look at the content of the JSON (e.g., presence of a specific field) to decide which subtype to deserialize into.
- When serializing, if the choice of serializer depends on the runtime state of the object beyond just its class.

### Usage Example

Let's consider a common scenario: a sealed interface `Message` with different implementations.

```kotlin
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import io.genkt.serialization.json.JsonPolymorphicSerializer // Assuming this is the correct import path

// 1. Define your polymorphic type hierarchy
@Serializable(with = MessageSerializer::class)
sealed interface Message {
    val content: String
}

@Serializable
data class TextMessage(override val content: String, val sender: String) : Message

@Serializable
data class ImageMessage(override val content: String, val imageUrl: String) : Message

@Serializable
data class GenericMessage(override val content: String, val metadata: Map<String, String> = emptyMap()) : Message

// 2. Create a custom KSerializer using JsonPolymorphicSerializer
object MessageSerializer : JsonPolymorphicSerializer<Message>(
    serialName = "Message", // Unique name for this polymorphic type
    childSerializers = listOf(
        TextMessage.serializer(),
        ImageMessage.serializer(),
        GenericMessage.serializer()
        // Add other concrete message type serializers here
    ),
    selectSerializer = { messageInstance ->
        // Logic to select the serializer based on the instance type
        when (messageInstance) {
            is TextMessage -> TextMessage.serializer()
            is ImageMessage -> ImageMessage.serializer()
            is GenericMessage -> GenericMessage.serializer()
            // else -> throw IllegalArgumentException("Unknown message type: $messageInstance")
        }
    },
    selectDeserializer = { jsonElement ->
        // Logic to select the deserializer based on the JSON content
        val jsonObject = jsonElement.jsonObject
        when {
            "sender" in jsonObject -> TextMessage.serializer()
            "imageUrl" in jsonObject -> ImageMessage.serializer()
            // Add more sophisticated checks if needed, e.g., based on a 'type' field
            // "type" in jsonObject && jsonObject["type"]?.jsonPrimitive?.content == "text" -> TextMessage.serializer()
            else -> GenericMessage.serializer() // Fallback or default type
        }
    }
)

// 3. Use it with kotlinx.serialization.Json
fun main() {
    val json = Json { prettyPrint = true }

    val messages: List<Message> = listOf(
        TextMessage("Hello World!", "Alice"),
        ImageMessage("A beautiful cat.", "http://example.com/cat.jpg"),
        GenericMessage("Some generic event occurred.")
    )

    // Serialization
    val serializedMessages = json.encodeToString(serializer<List<Message>>(), messages)
    println("---- Serialized ----")
    println(serializedMessages)

    // Deserialization
    val deserializedMessages = json.decodeFromString(serializer<List<Message>>(), serializedMessages)
    println("\n---- Deserialized ----")
    deserializedMessages.forEach { msg ->
        when (msg) {
            is TextMessage -> println("TextMessage(content='${msg.content}', sender='${msg.sender}')")
            is ImageMessage -> println("ImageMessage(content='${msg.content}', imageUrl='${msg.imageUrl}')")
            is GenericMessage -> println("GenericMessage(content='${msg.content}', metadata=${msg.metadata})")
        }
    }
}

// Expected Output:
// ---- Serialized ----
// [
//     {
//         "content": "Hello World!",
//         "sender": "Alice"
//     },
//     {
//         "content": "A beautiful cat.",
//         "imageUrl": "http://example.com/cat.jpg"
//     },
//     {
//         "content": "Some generic event occurred.",
//         "metadata": {}
//     }
// ]
//
// ---- Deserialized ----
// TextMessage(content='Hello World!', sender='Alice')
// ImageMessage(content='A beautiful cat.', imageUrl='http://example.com/cat.jpg')
// GenericMessage(content='Some generic event occurred.', metadata={})
```

### Key Parameters of `JsonPolymorphicSerializer`

- `serialName: String`: A descriptive name for the polymorphic type being handled (e.g., "Message", "Event"). This is used in generating the `SerialDescriptor`.
- `childSerializers: List<KSerializer<out T>>`: A complete list of serializers for all possible concrete subtypes. The `serialName` of each child's descriptor must be unique.
- `selectSerializer: (T) -> SerializationStrategy<*>`: A lambda that receives an instance of the base type `T` and must return the correct `KSerializer` (cast to `SerializationStrategy<T>`) for that specific instance.
- `selectDeserializer: (JsonElement) -> DeserializationStrategy<T>`: A lambda that receives the raw `JsonElement` being parsed and must return the correct `KSerializer` (cast to `DeserializationStrategy<T>`) to deserialize it into the appropriate subtype.
- `annotations: List<Annotation>`: Optional annotations for the generated `SerialDescriptor`.

## Getting Started

To use this module:
1. Add it as a dependency to your project.
2. Define your polymorphic class/interface hierarchy.
3. Create an object that extends `JsonPolymorphicSerializer<YourBaseType>`, providing the necessary parameters, especially the `selectSerializer` and `selectDeserializer` lambdas.
4. Annotate your base type with `@Serializable(with = YourCustomSerializer::class)`.
5. Use `kotlinx.serialization.Json` as usual for encoding and decoding.

This module offers a powerful way to handle complex or non-standard polymorphic JSON structures when the built-in capabilities of `kotlinx.serialization` are not sufficient.
