package io.github.genkt.jsonrpc.test

import io.genkt.jsonprc.client.JsonRpcClient
import io.genkt.jsonprc.client.intercept
import io.genkt.jsonprc.client.sendNotification
import io.genkt.jsonprc.client.sendRequest
import io.genkt.jsonrpc.*
import io.genkt.jsonrpc.server.JsonRpcServer
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

suspend fun testJsonTransport(
    transportPair: Pair<JsonTransport, JsonTransport>
) {
    println("testJsonTransport")
    val clientTransport = transportPair.first.asJsonRpcClientTransport()
    val serverTransport = transportPair.second.asJsonRpcServerTransport()
    val receivedNotificationChannel = Channel<JsonRpcNotification>()
    val serverErrorChannel = Channel<Throwable>()
    val server = JsonRpcServer(
        transport = serverTransport,
        onRequest = { request ->
            if (request.params == null)
                throw IllegalArgumentException("Params cannot be null")
            if (request.method == "timeout")
                delay(10.seconds)
            JsonRpcSuccessResponse(
                id = request.id,
                result = JsonPrimitive("Method: ${request.method}")
            )
        },
        onNotification = {
            if (it.params == null)
                throw IllegalArgumentException("Params cannot be null")
            receivedNotificationChannel.send(it)
        },
        errorHandler = { serverErrorChannel.send(it) }
    )
    server.start()
    val client = JsonRpcClient(
        transport = clientTransport,
    ).intercept {
        requestInterceptor = TimeOut(1.seconds)
    }
    client.start()
    println("Client and Server started")

    val response = client.sendRequest(
        id = RequestId.NumberId(1),
        method = "test",
        params = JsonPrimitive("test")
    )
    assertEquals(response.id, RequestId.NumberId(1))
    assertEquals("Method: test", response.result.jsonPrimitive.content)
    println("Response test received")
    val notification = buildJsonObject {
        put("type", "event")
    }
    client.sendNotification(
        method = "notify",
        params = notification
    )
    assertEquals(receivedNotificationChannel.receive().params, notification)
    println("Notification received")
    assertFails {
        client.sendRequest(
            id = RequestId.NumberId(2),
            method = "test",
            params = null
        )
    }
    assertEquals(
        serverErrorChannel.receive().message,
        "Params cannot be null"
    )
    println("Null Error received")
    client.sendNotification(
        method = "notify",
        params = null
    )
    assertEquals(
        serverErrorChannel.receive().message,
        "Params cannot be null"
    )
    println("Null Error of notification received")
    // Send a request from the client and expect a timeout
    val exception = assertFails {
        client.sendRequest(
            id = RequestId.NumberId(3),
            method = "timeout",
            params = JsonObject.Empty
        )
    }

    // Verify the exception is a JsonRpcTimeoutException
    assertIs<TimeoutCancellationException>(exception)
    println("Timeout exception received")

    server.close()
    client.close()

    println("Client and Server closed")
    assertFails {
        client.sendRequest(
            id = RequestId.NumberId(4),
            method = "test",
            params = JsonPrimitive("test")
        )
    }
    println("Client closed, request failed as expected")
}
