# JSON-RPC Client Module

The `jsonrpc-client` module provides a Kotlin-first, coroutine-based client for interacting with JSON-RPC 2.0 servers. It builds upon the `jsonrpc-common` module, offering a high-level API to easily send requests and notifications.

## Key Features

- **Simple Client Creation**: Uses a DSL (`JsonRpcClient { ... }`) for straightforward configuration and instantiation.
- **Request/Notification Sending**: Provides convenient extension functions (`sendRequest`, `sendNotification`) on `JsonRpcClient` for making RPC calls.
- **Transport Agnostic**: Relies on the `JsonRpcClientTransport` abstraction from `jsonrpc-common`, allowing it to work with any underlying communication protocol (e.g., WebSockets, HTTP).
- **Coroutine-Based**: Leverages Kotlin Coroutines for asynchronous operations, ensuring non-blocking calls.
- **Interceptor Support**: Allows customization of call behavior through the `callInterceptor` property in the builder, utilizing the `Interceptor` mechanism from `jsonrpc-common`.
- **Error Handling**:
    - Handles server error responses by throwing `JsonRpcResponseException`.
    - Manages request ID mismatches with `JsonRpcRequestIdNotFoundException`.
    - Provides an `uncaughtErrorHandler` for handling exceptions within the client's coroutine scope.
- **Resource Management**: The `JsonRpcClient` interface extends `AutoCloseable` for proper resource cleanup.

## Design Principles

- **Ease of Use**: Aims to provide a simple and intuitive API for common JSON-RPC client tasks.
- **Asynchronous by Default**: Designed with coroutines to ensure that network operations do not block threads.
- **Extensibility**: While providing high-level abstractions, it allows for customization through transports and interceptors.
- **Leverages `jsonrpc-common`**: Reuses the DTOs, transport abstractions, and interceptor patterns from the common module for consistency.

## Usage Examples

### Dependencies

Ensure you have the `jsonrpc-common` module as a dependency, as `jsonrpc-client` relies on it.

```kotlin
// In your build.gradle.kts
// implementation(project(":jsonrpc-common")) // Or the appropriate coordinates
// implementation(project(":jsonrpc-client"))
```

### Creating a JSON-RPC Client

To create a client, you need to provide a `JsonRpcClientTransport`. The actual implementation of this transport will depend on your chosen communication method (e.g., Ktor client for WebSockets or HTTP).

```kotlin
import io.genkt.jsonprc.client.*
import io.genkt.jsonrpc.* // For DTOs and Transport from jsonrpc-common
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.seconds

// 1. Obtain or implement a JsonRpcClientTransport
//    This is a placeholder. In a real scenario, you'd use a concrete
//    transport implementation (e.g., based on Ktor, WebSockets, etc.).
val dummyScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
val dummySendChannel = Channel<SendAction<JsonRpcClientMessage>>() // For sending client messages
val dummyReceiveFlow = MutableSharedFlow<Result<JsonRpcServerMessage>>() // For receiving server responses

val myTransport: JsonRpcClientTransport = Transport(
    sendChannel = dummySendChannel,
    receiveFlow = dummyReceiveFlow,
    coroutineScope = dummyScope,
    onClose = {
        dummySendChannel.close()
        dummyScope.cancel()
    },
    onStart = { /* Optional: logic to execute on transport start */ }
)

// 2. Create the JsonRpcClient using the DSL
val client = JsonRpcClient {
    transport = myTransport
    uncaughtErrorHandler = { throwable ->
        println("Client uncaught error: ${throwable.message}")
    }
    additionalCoroutineContext = Dispatchers.IO // Example: specify dispatcher for client operations
    
    // Add a default timeout for requests
    requestTimeOut(10.seconds) 
    
    // You can add other custom interceptors if needed
    // interceptRequest { ... }
    // interceptNotification { ... }
}

// 3. Start the client (important!)
// GlobalScope.launch { // Or use a specific CoroutineScope
//     try {
//         client.start()
//         println("JSON-RPC Client started.")
//         // Now the client is ready to send messages and listen for responses.
//     } catch (e: Exception) {
//         println("Failed to start client: ${e.message}")
//     }
// }
```
**Note**: The `client.start()` call is crucial. It typically initiates the connection and starts the machinery for listening to incoming messages on the transport.

### Sending Requests

Requests expect a response from the server.

```kotlin
// Assume 'client' is an already created and started JsonRpcClient instance.
// GlobalScope.launch { // Or use a specific CoroutineScope
//     try {
//         val requestId = JsonRpc.NumberIdGenerator() // From jsonrpc-common
//         val response = client.sendRequest(
//             id = requestId(),
//             method = "calculator/add",
//             params = buildJsonArray {
//                 add(5)
//                 add(3)
//             }
//         )
//         println("Server responded to 'add' request with result: ${response.result}") // e.g., JsonPrimitive(8)
//     } catch (e: JsonRpcResponseException) {
//         println("Server error: (${e.error.code}) ${e.error.message} - Data: ${e.error.data}")
//     } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
//         println("Request timed out!")
//     } catch (e: Exception) {
//         println("An error occurred while sending request: ${e.message}")
//     }
// }

// Example of how you might simulate a server response for the dummy transport:
// GlobalScope.launch {
//    delay(100) // Simulate network latency
//    dummyReceiveFlow.emit(
//        Result.success(
//            JsonRpcSuccessResponse(
//                id = RequestId.NumberId(0L), // Match an expected ID from a request
//                result = JsonPrimitive(8)
//            )
//        )
//    )
// }
```

### Sending Notifications

Notifications are fire-and-forget; they do not elicit a response from the server.

```kotlin
// Assume 'client' is an already created and started JsonRpcClient instance.
// GlobalScope.launch { // Or use a specific CoroutineScope
//     try {
//         client.sendNotification(
//             method = "logging/logEvent",
//             params = buildJsonObject {
//                 put("level", JsonPrimitive("info"))
//                 put("message", JsonPrimitive("User logged in"))
//             }
//         )
//         println("Notification 'logEvent' sent.")
//     } catch (e: Exception) {
//         // Errors here are typically related to sending, not server processing
//         println("An error occurred while sending notification: ${e.message}")
//     }
// }
```

### Closing the Client

When the client is no longer needed, close it to release resources, including the underlying transport and coroutine scope.

```kotlin
// client.close()
// println("JSON-RPC Client closed.")
```

## Error Handling

- **`JsonRpcResponseException`**: Thrown by `sendRequest` if the server returns a JSON-RPC error object. Access `e.error` for details.
- **`JsonRpcRequestIdNotFoundException`**: Can occur if the client receives a response with an ID that doesn't match any pending requests. This is usually handled internally but might surface if there are transport-level issues or server misbehavior.
- **`kotlinx.coroutines.TimeoutCancellationException`**: Thrown if a request times out (e.g., if `requestTimeOut` is configured).
- **Transport-level exceptions**: Depending on the `JsonRpcClientTransport` implementation, other exceptions related to network issues, serialization, etc., might occur.
- **`uncaughtErrorHandler`**: Catches any other exceptions that might occur within the client's internal coroutines, preventing them from crashing the application if not handled elsewhere.

This `jsonrpc-client` module simplifies the process of building robust JSON-RPC clients in Kotlin by handling much of the boilerplate and providing a clean, coroutine-centric API.
