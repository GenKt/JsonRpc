package io.github.genkt.jsonrpc.test

import io.github.genkt.jsonprc.client.JsonRpcClient
import io.github.genkt.jsonprc.client.JsonRpcTimeoutException
import io.github.genkt.jsonprc.client.sendNotification
import io.github.genkt.jsonprc.client.sendRequest
import io.github.genkt.jsonrpc.*
import io.github.genkt.jsonrpc.server.JsonRpcServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

suspend fun testJsonTransport(
    transportPair: Pair<JsonTransport, JsonTransport>
) {
    val clientTransport = transportPair.first.asJsonRpcClientTransport()
    val serverTransport = transportPair.second.asJsonRpcServerTransport()
    val receivedNotificationChannel = Channel<JsonRpcNotification>()
    val serverErrorChannel = Channel<Throwable>()
    val server = JsonRpcServer(
        transport = serverTransport,
        onRequest = { request ->
            if (request.params == null)
                throw IllegalArgumentException("Params cannot be null")
            if (request.method == "timeout") {
                delay(1.seconds)
            }
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
    val client = JsonRpcClient(
        transport = clientTransport,
        timeOut = 100.milliseconds,
        coroutineContext = SupervisorJob() + Dispatchers.Default
    )

    val response = client.sendRequest(
        id = RequestId.NumberId(1),
        method = "test",
        params = JsonPrimitive("test")
    )
    assertEquals(response.id, RequestId.NumberId(1))
    assertEquals("Method: test", response.result.jsonPrimitive.content)

    val notification = buildJsonObject {
        put("type", "event")
    }
    client.sendNotification(
        method = "notify",
        params = notification
    )
    assertEquals(receivedNotificationChannel.receive().params, notification)

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

    client.sendNotification(
        method = "notify",
        params = null
    )
    assertEquals(
        serverErrorChannel.receive().message,
        "Params cannot be null"
    )


    // Send a request from the client and expect a timeout
    val exception = assertFails {
        client.sendRequest(
            id = RequestId.NumberId(3),
            method = "timeout",
            params = JsonObject.Empty
        )
    }

    // Verify the exception is a JsonRpcTimeoutException
    assertIs<JsonRpcTimeoutException>(exception)


    server.close()
    client.close()
    assertFails {
        client.sendRequest(
            id = RequestId.NumberId(4),
            method = "test",
            params = JsonPrimitive("test")
        )
    }
}
