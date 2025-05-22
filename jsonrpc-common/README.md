# JSON-RPC Common Module

The `jsonrpc-common` module provides a set of common data structures, utilities, and abstractions for working with JSON-RPC 2.0 messages in Kotlin. It is designed to be flexible and extensible, allowing it to be used as a foundation for building JSON-RPC clients and servers.

## Key Features

- **DTOs for JSON-RPC Messages**: Defines Kotlin classes and interfaces for all standard JSON-RPC message types, including:
    - `JsonRpcRequest`: Represents a JSON-RPC request.
    - `JsonRpcNotification`: Represents a JSON-RPC notification.
    - `JsonRpcSuccessResponse`: Represents a successful JSON-RPC response.
    - `JsonRpcFailResponse`: Represents a failed JSON-RPC response.
    - `RequestId`: Represents a request ID (String, Number, or Null).
    - Batch messages (`JsonRpcClientMessageBatch`, `JsonRpcServerMessageBatch`).
- **Serialization/Deserialization**: Uses `kotlinx.serialization` for handling JSON conversion. A pre-configured `Json` instance (`JsonRpc.json`) is provided with sensible defaults for JSON-RPC communication.
- **Transport Abstraction**:
    - `Transport<Input, Output>`: A core interface representing a bidirectional communication channel for sending and receiving messages.
    - `SharedTransport<Input, Output>`: A sub-interface for transports that can be shared by multiple collectors (using `SharedFlow`).
    - Utility functions for creating, converting, and managing transports (e.g., `asJsonRpcClientTransport`, `asJsonTransport`, `sharedIn`).
- **Interceptors**:
    - `Interceptor<T>`: A typealias for functions that can modify a value in a pipeline.
    - `TransportInterceptor`: Allows modification of the send and receive flows of a `Transport`.
    - Utility functions for creating common interceptors like `TimeOut`, `Catch`, `BeforeInvoke`, and `OnInvoke`.
- **Extensibility**: Designed with interfaces and generic types to allow for easy customization and extension.

## Design Principles

- **Type Safety**: Leverages Kotlin's type system to provide strong typing for JSON-RPC messages and operations.
- **Coroutines for Asynchronous Operations**: Built with Kotlin Coroutines to handle asynchronous communication efficiently.
- **Immutability**: DTOs are primarily data classes, promoting immutability where possible.
- **Modularity**: Core concerns like DTOs, transport, and serialization are separated for better maintainability.

## Usage Examples

### Defining JSON-RPC Messages

```kotlin
import io.genkt.jsonrpc.*
import kotlinx.serialization.json.*

// Create a JSON-RPC Request
val requestId = JsonRpc.NumberIdGenerator() // Or RequestId.StringId("my-request-1")
val request = JsonRpcRequest(
    id = requestId(),
    method = "add",
    params = buildJsonArray {
        add(1)
        add(2)
    }
)

// Create a JSON-RPC Notification
val notification = JsonRpcNotification(
    method = "user/updated",
    params = buildJsonObject {
        put("userId", JsonPrimitive(123))
        put("status", JsonPrimitive("active"))
    }
)

// Create a JSON-RPC Success Response
val successResponse = JsonRpcSuccessResponse(
    id = RequestId.NumberId(1), // Corresponds to the request ID
    result = JsonPrimitive(3)
)

// Create a JSON-RPC Error Response
val errorResponse = JsonRpcFailResponse(
    id = RequestId.NumberId(2),
    error = JsonRpcFailResponse.Error(
        code = JsonRpcFailResponse.Error.Code.MethodNotFound,
        message = "Method not found: subtract"
    )
)
```

### Using Transports

The `Transport` interface is the core of communication. Implementations of this interface would handle the actual sending and receiving of data (e.g., over WebSockets, HTTP, etc.). The `jsonrpc-common` module provides abstractions and utilities to work with these transports.

```kotlin
import io.genkt.jsonrpc.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.Channel // For a concrete SendChannel example

// Assume you have a StringTransport implementation
// For example, a hypothetical WebSocketStringTransport:
// class WebSocketStringTransport(scope: CoroutineScope) : StringTransport { ... }

// val stringTransport: StringTransport = WebSocketStringTransport(CoroutineScope(Dispatchers.Default))

// For demonstration, let's create a dummy StringTransport
val dummyScope = CoroutineScope(Dispatchers.Unconfined)
val dummySendChannel = Channel<SendAction<String>>()
val dummyReceiveFlow = flowOf<Result<String>>(Result.success("""{"jsonrpc":"2.0","id":1,"result":42}"""))

val stringTransport: StringTransport = Transport(
    sendChannel = dummySendChannel,
    receiveFlow = dummyReceiveFlow,
    coroutineScope = dummyScope
)


// Convert it to a JsonTransport
val jsonTransport: JsonTransport = stringTransport.asJsonTransport()

// Convert it to a JsonRpcClientTransport
val clientTransport: JsonRpcClientTransport = jsonTransport.asJsonRpcClientTransport()

// Now you can use clientTransport to send JsonRpcClientMessage and receive JsonRpcServerMessage
// Example (conceptual):
// GlobalScope.launch {
//     clientTransport.sendChannel.sendOrThrow(request) // Send the request defined earlier
//     clientTransport.receiveFlow.collect { result ->
//         result.fold(
//             onSuccess = { serverMessage ->
//                 when (serverMessage) {
//                     is JsonRpcSuccessResponse -> println("Received success: ${serverMessage.result}")
//                     is JsonRpcFailResponse -> println("Received error: ${serverMessage.error.message}")
//                     // Other server message types if applicable
//                 }
//             },
//             onFailure = { error ->
//                 println("Transport error: ${error.message}")
//             }
//         )
//     }
// }
```

### Using Interceptors

Interceptors can be used to modify the behavior of transports or other functions.

```kotlin
import io.genkt.jsonrpc.*
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.seconds

// Example: Add a timeout to a transport's receive flow (conceptual)
// This specific example would require more direct access to the flow processing
// but demonstrates the interceptor concept.

// Create a timeout interceptor for a generic suspend function
// val timeoutInterceptor = GenericInterceptorScope.TimeOut<JsonRpcClientMessage, JsonRpcServerMessage>(5.seconds)

// Apply interceptors to a Transport using TransportInterceptor
// val interceptedClientTransport = clientTransport.intercept {
//     // Example: Log sent messages
//     sendChannelInterceptor += { flow ->
//         flow.map { sendAction ->
//             println("Sending: ${sendAction.value}")
//             sendAction
//         }
//     }

//     // Example: Modify received messages (e.g. for debugging)
//     receiveFlowInterceptor += { flow ->
//         flow.map { result ->
//             result.map { serverMessage ->
//                 println("Received raw: $serverMessage")
//                 // You could transform the serverMessage here if needed
//                 serverMessage
//             }
//         }
//     }
// }
```

## Getting Started

This module provides the common building blocks. To use it, you would typically:
1. Depend on this `jsonrpc-common` module.
2. Implement a `Transport` for your specific communication protocol (e.g., WebSockets, HTTP).
3. Use the DTOs to construct and parse JSON-RPC messages.
4. Utilize the `JsonRpc.json` object for serialization/deserialization or configure your own.
5. Optionally, apply interceptors to customize transport behavior or other logic.

This module itself does not provide concrete client or server implementations but offers the necessary tools to build them.
