# JSON-RPC Client Module

A Kotlin-first, coroutine-based framework for building JSON-RPC clients.

## Design Principles

- Structured concurrency: Every client has its own `CoroutineScope`, making it safe and easy to stop the client.
- Extensibility: You can intercept the request/notification sending process and add your own logic
- DSL style: Easy to configure and instantiate the client.

## How to use

```kotlin
val client = JsonRpcClient {
    transport = clientTransport.asJsonRpcClientTransport()
    additionalCoroutineContext += CoroutineName("Client")
    interceptRequest { handler -> { request ->
        // intercept request sending logic. e.g. log the request time
    } }
    interceptNotification { handler -> { notification ->
        // intercept notification sending logic. e.g. log the notification body
    } }
    requestTimeOut(1.seconds)
}
```
