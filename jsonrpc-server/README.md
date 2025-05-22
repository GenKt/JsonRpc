# JSON-RPC Server Module

The `jsonrpc-server` module provides a Kotlin-first, coroutine-based framework for building JSON-RPC 2.0 servers. It is designed to handle incoming requests and notifications, process them according to defined handlers, and send back appropriate responses. This module builds upon `jsonrpc-common`.

## Key Features

- **Server Creation DSL**: Utilizes a DSL (`JsonRpcServer { ... }`) for easy configuration and instantiation of the server.
- **Request and Notification Handling**:
    - Define custom logic for handling RPC requests via the `onRequest` lambda in the builder.
    - Process notifications using the `onNotification` lambda.
- **Transport Agnostic**: Leverages the `JsonRpcServerTransport` abstraction from `jsonrpc-common`, allowing it to operate with various communication protocols (e.g., WebSockets, HTTP).
- **Coroutine-Based**: Built with Kotlin Coroutines for asynchronous and non-blocking request processing.
- **Interceptor Support**: Allows customization of request and notification handling via `requestInterceptor` and `notificationInterceptor` in the builder, using the `Interceptor` mechanism from `jsonrpc-common`.
    - Includes convenience functions like `requestTimeout` to easily add common interceptors.
- **Error Handling**:
    - Provides an `uncaughtErrorHandler` for global exception handling within the server's coroutine scope.
    - Allows `onRequest` handlers to return `JsonRpcFailResponse` for specific RPC errors.
- **Resource Management**: The `JsonRpcServer` interface extends `AutoCloseable` for proper resource cleanup (e.g., stopping the transport and canceling coroutines).

## Design Principles

- **Simplicity and Clarity**: Aims for an intuitive API for defining server-side RPC logic.
- **Asynchronous Processing**: Employs coroutines to handle multiple client requests concurrently and efficiently.
- **Extensibility**: Facilitates customization through transports and interceptors.
- **Leverages `jsonrpc-common`**: Reuses DTOs, transport abstractions, and interceptor patterns from the common module for consistency and to avoid code duplication.

## Usage Examples

### Dependencies

Ensure you have the `jsonrpc-common` module as a dependency, as `jsonrpc-server` relies on it.

```kotlin
// In your build.gradle.kts
// implementation(project(":jsonrpc-common")) // Or the appropriate coordinates
// implementation(project(":jsonrpc-server"))
```

### Creating a JSON-RPC Server

To create a server, you need:
1. A `JsonRpcServerTransport` implementation (e.g., using Ktor server for WebSockets or HTTP).
2. Handlers for requests (`onRequest`) and notifications (`onNotification`).

```kotlin
import io.genkt.jsonrpc.server.*
import io.genkt.jsonrpc.* // For DTOs, Transport from jsonrpc-common
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.seconds

// 1. Obtain or implement a JsonRpcServerTransport
//    This is a placeholder. In a real scenario, you'd use a concrete
//    transport implementation (e.g., based on Ktor Server, WebSockets, etc.).
val serverCoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
val serverSendChannel = Channel<SendAction<JsonRpcServerMessage>>() // For sending responses/errors
val serverReceiveFlow = MutableSharedFlow<Result<JsonRpcClientMessage>>() // For receiving requests/notifications

val myServerTransport: JsonRpcServerTransport = Transport(
    sendChannel = serverSendChannel,
    receiveFlow = serverReceiveFlow,
    coroutineScope = serverCoroutineScope,
    onClose = {
        serverSendChannel.close()
        serverCoroutineScope.cancel()
    },
    onStart = { /* Optional: logic to execute on transport start */ }
)

// 2. Define request and notification handlers
val requestHandler: suspend (JsonRpcRequest) -> JsonRpcServerSingleMessage = { request ->
    when (request.method) {
        "echo" -> JsonRpcSuccessResponse(request.id, request.params ?: JsonNull)
        "add" -> {
            val paramsArray = request.params as? JsonArray
            if (paramsArray?.size == 2 && paramsArray[0] is JsonPrimitive && paramsArray[1] is JsonPrimitive) {
                try {
                    val num1 = (paramsArray[0] as JsonPrimitive).long
                    val num2 = (paramsArray[1] as JsonPrimitive).long
                    JsonRpcSuccessResponse(request.id, JsonPrimitive(num1 + num2))
                } catch (e: Exception) {
                    JsonRpcFailResponse(
                        request.id,
                        JsonRpcFailResponse.Error(JsonRpcFailResponse.Error.Code.InvalidParams, "Invalid parameters for 'add'")
                    )
                }
            } else {
                JsonRpcFailResponse(
                    request.id,
                    JsonRpcFailResponse.Error(JsonRpcFailResponse.Error.Code.InvalidParams, "Method 'add' requires two number parameters.")
                )
            }
        }
        else -> JsonRpcFailResponse(
            request.id,
            JsonRpcFailResponse.Error(JsonRpcFailResponse.Error.Code.MethodNotFound, "Method '${request.method}' not found.")
        )
    }
}

val notificationHandler: suspend (JsonRpcNotification) -> Unit = { notification ->
    println("Received notification '${notification.method}': ${notification.params}")
    // Process the notification (e.g., log it, update state)
}

// 3. Create the JsonRpcServer using the DSL
val server = JsonRpcServer {
    transport = myServerTransport
    onRequest = requestHandler
    onNotification = notificationHandler
    uncaughtErrorHandler = { throwable ->
        System.err.println("Server uncaught error: ${throwable.message}")
        throwable.printStackTrace()
    }
    additionalCoroutineContext = Dispatchers.Default // Example dispatcher for server operations
    
    // Add a default timeout for processing requests
    requestTimeout(15.seconds)
}

// 4. Start the server (important!)
// server.start()
// println("JSON-RPC Server started and listening...")

// To make the server do something in this dummy example, you could simulate a client message:
// GlobalScope.launch {
//     delay(1000) // Give server time to start
//     val sampleRequestId = JsonRpc.NumberIdGenerator()()
//     serverReceiveFlow.emit(
//         Result.success(
//             JsonRpcRequest(
//                 id = sampleRequestId,
//                 method = "echo",
//                 params = buildJsonObject { put("message", JsonPrimitive("Hello Server!")) }
//             )
//         )
//     )
//     // And check serverSendChannel for the response, or logs for notifications
// }
```
**Note**: The `server.start()` call is crucial. It initiates the transport, allowing the server to begin receiving and processing messages from clients.

### Handling Requests

The `onRequest` lambda is the core of your server's RPC logic. It receives a `JsonRpcRequest` and must return a `JsonRpcServerSingleMessage` (either `JsonRpcSuccessResponse` or `JsonRpcFailResponse`).

```kotlin
// Example onRequest handler (already shown above)
// suspend fun myRequestHandler(request: JsonRpcRequest): JsonRpcServerSingleMessage {
//     return when (request.method) {
//         "getUserData" -> {
//             val userId = (request.params as? JsonObject)?.get("userId")?.jsonPrimitive?.contentOrNull
//             if (userId != null) {
//                 // Simulate fetching user data
//                 JsonRpcSuccessResponse(
//                     request.id,
//                     buildJsonObject { put("name", JsonPrimitive("John Doe")); put("id", JsonPrimitive(userId)) }
//                 )
//             } else {
//                 JsonRpcFailResponse(request.id, JsonRpcFailResponse.Error.Code.InvalidParams, "Missing userId")
//             }
//         }
//         // ... other methods
//         else -> JsonRpcFailResponse(request.id, JsonRpcFailResponse.Error.Code.MethodNotFound, "Method not found")
//     }
// }
```

### Handling Notifications

The `onNotification` lambda processes incoming notifications. Since notifications don't require a response, this function has a `Unit` return type.

```kotlin
// Example onNotification handler (already shown above)
// suspend fun myNotificationHandler(notification: JsonRpcNotification) {
//     if (notification.method == "logClientEvent") {
//         println("Client event logged: ${notification.params}")
//     }
// }
```

### Using Interceptors

Interceptors can modify the request/notification handling flow. For example, adding a timeout:

```kotlin
// In the JsonRpcServer builder DSL:
// requestTimeout(30.seconds) // Apply a 30-second timeout to all requests

// For more complex interceptors:
// requestInterceptor += { nextHandler -> // 'plusAssign' to chain interceptors
//     { request ->
//         println("Request Interceptor: Received ${request.method}")
//         val response = nextHandler(request) // Call the next handler in the chain
//         println("Request Interceptor: Sending response for ${request.id}")
//         response
//     }
// }
```

### Closing the Server

When the server is no longer needed, close it to release resources, including the underlying transport and coroutine scope.

```kotlin
// server.close()
// println("JSON-RPC Server stopped.")
```

## Error Handling

- **Method Logic**: Your `onRequest` handler is responsible for returning `JsonRpcFailResponse` for application-specific errors (e.g., invalid parameters, method not found).
- **Timeouts**: If `requestTimeout` is used, a request taking too long will automatically result in an internal error response being sent to the client.
- **Transport Errors**: The underlying `JsonRpcServerTransport` should handle its own I/O errors. Failures in receiving messages might be logged or passed to `uncaughtErrorHandler` depending on the transport's behavior.
- **`uncaughtErrorHandler`**: This handler in the `JsonRpcServer.Builder` is a last resort for exceptions occurring in the server's processing scope that are not caught elsewhere (e.g., unexpected errors in your handlers or interceptors). It can be used for logging critical failures.

The `jsonrpc-server` module provides the essential framework for building robust and efficient JSON-RPC servers in Kotlin, emphasizing asynchronous processing and clear separation of concerns.
