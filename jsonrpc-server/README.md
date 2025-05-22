# JSON-RPC Server Module

A Kotlin-first, coroutine-based framework for building JSON-RPC servers.

## Design Principles

- Structured concurrency: Every server has its own `CoroutineScope`, making it safe and easy to stop the server.
- Extensibility: You can intercept the request/notification handling flow and add your own logic
- DSL style: Easy to configure and instantiate the server.

## How to use

``` kotlin
val server = JsonRpcServer {
    // DSL-styled server builder
    transport = serverTransport.asJsonRpcServerTransport()
    onRequest = { request ->
        // process your request and make a response
    }
    onNotification = { notification ->
        // process your notification
    }
    requestInterceptor += Catch { e ->
        // process your exception here
    }
    additionalCoroutineContext += CoroutineName("Server")
}
server.start()
server.close() // JsonRpcServer implements AutoClosable
```