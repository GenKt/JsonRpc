package io.github.genkt.jsonrpc.test

import io.github.genkt.jsonrpc.*
import io.github.genkt.jsonrpc.transport.memory.InMemoryTransport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

class DtoTest {
    @Test
    fun `test RequestId serialization and deserialization`() {
        // Test StringId
        val stringId = RequestId.StringId("test-id")
        val stringIdJson = JsonRpc.json.encodeToJsonElement(RequestId.serializer(), stringId)
        assertEquals(JsonPrimitive("test-id"), stringIdJson)
        val decodedStringId = JsonRpc.json.decodeFromJsonElement(RequestId.serializer(), stringIdJson)
        assertEquals(stringId, decodedStringId)

        // Test NumberId
        val numberId = RequestId.NumberId(123)
        val numberIdJson = JsonRpc.json.encodeToJsonElement(RequestId.serializer(), numberId)
        assertEquals(JsonPrimitive(123), numberIdJson)
        val decodedNumberId = JsonRpc.json.decodeFromJsonElement(RequestId.serializer(), numberIdJson)
        assertEquals(numberId, decodedNumberId)

        // Test NullId
        val nullId = RequestId.NullId
        val nullIdJson = JsonRpc.json.encodeToJsonElement(RequestId.serializer(), nullId)
        assertEquals(JsonNull, nullIdJson)
        val decodedNullId = JsonRpc.json.decodeFromJsonElement(RequestId.serializer(), nullIdJson)
        assertEquals(nullId, decodedNullId)
    }

    @Test
    fun `test JsonRpcRequest through transport`() = runTest {
        // Create a pair of transports
        val transportPair = InMemoryTransport()
        val jsonRpcTransport1 = transportPair.first.asJsonTransport().asJsonRpcTransport()
        val jsonRpcTransport2 = transportPair.second.asJsonTransport().asJsonRpcTransport()

        // Create a request
        val request = JsonRpcRequest(
            id = RequestId.NumberId(1),
            method = "test",
            params = buildJsonObject {
                put("param", "value")
            }
        )

        // Send the request through the first transport
        jsonRpcTransport1.sendChannel.send(request)

        // Receive the request from the second transport
        val received = jsonRpcTransport2.receiveFlow.first()

        // Verify the received request
        assertTrue(received is JsonRpcRequest)
        assertEquals(request.id, (received as JsonRpcRequest).id)
        assertEquals(request.method, received.method)
        assertEquals(request.params, received.params)
    }

    @Test
    fun `test JsonRpcNotification through transport`() = runTest {
        // Create a pair of transports
        val transportPair = InMemoryTransport()
        val jsonRpcTransport1 = transportPair.first.asJsonTransport().asJsonRpcTransport()
        val jsonRpcTransport2 = transportPair.second.asJsonTransport().asJsonRpcTransport()

        // Create a notification
        val notification = JsonRpcNotification(
            method = "notify",
            params = buildJsonObject {
                put("event", "something-happened")
            }
        )

        // Send the notification through the first transport
        jsonRpcTransport1.sendChannel.send(notification)

        // Receive the notification from the second transport
        val received = jsonRpcTransport2.receiveFlow.first()

        // Verify the received notification
        assertTrue(received is JsonRpcNotification)
        assertEquals(notification.method, (received as JsonRpcNotification).method)
        assertEquals(notification.params, received.params)
    }

    @Test
    fun `test JsonRpcSuccessResponse through transport`() = runTest {
        // Create a pair of transports
        val transportPair = InMemoryTransport()
        val jsonRpcTransport1 = transportPair.first.asJsonTransport().asJsonRpcTransport()
        val jsonRpcTransport2 = transportPair.second.asJsonTransport().asJsonRpcTransport()

        // Create a success response
        val response = JsonRpcSuccessResponse(
            id = RequestId.StringId("test-id"),
            result = buildJsonObject {
                put("result", "success")
            }
        )

        // Send the response through the first transport
        jsonRpcTransport1.sendChannel.send(response)

        // Receive the response from the second transport
        val received = jsonRpcTransport2.receiveFlow.first()

        // Verify the received response
        assertTrue(received is JsonRpcSuccessResponse)
        assertEquals(response.id, (received as JsonRpcSuccessResponse).id)
        assertEquals(response.result, received.result)
    }

    @Test
    fun `test JsonRpcFailResponse through transport`() = runTest {
        // Create a pair of transports
        val transportPair = InMemoryTransport()
        val jsonRpcTransport1 = transportPair.first.asJsonTransport().asJsonRpcTransport()
        val jsonRpcTransport2 = transportPair.second.asJsonTransport().asJsonRpcTransport()

        // Create a fail response
        val response = JsonRpcFailResponse(
            id = RequestId.NumberId(1),
            error = JsonRpcFailResponse.Error(
                code = JsonRpcFailResponse.Error.Code.MethodNotFound,
                message = "Method not found"
            )
        )

        // Send the response through the first transport
        jsonRpcTransport1.sendChannel.send(response)

        // Receive the response from the second transport
        val received = jsonRpcTransport2.receiveFlow.first()

        // Verify the received response
        assertTrue(received is JsonRpcFailResponse)
        assertEquals(response.id, (received as JsonRpcFailResponse).id)
        assertEquals(response.error.code, received.error.code)
        assertEquals(response.error.message, received.error.message)
    }

    @Test
    fun `test JsonRpcClientMessageBatch through transport`() = runTest {
        // Create a pair of transports
        val transportPair = InMemoryTransport()
        val jsonRpcTransport1 = transportPair.first.asJsonTransport().asJsonRpcTransport()
        val jsonRpcTransport2 = transportPair.second.asJsonTransport().asJsonRpcTransport()

        // Create a request and a notification
        val request = JsonRpcRequest(
            id = RequestId.NumberId(1),
            method = "test1"
        )

        val notification = JsonRpcNotification(
            method = "test2"
        )

        // Create a batch
        val batch = JsonRpcClientMessageBatch(
            messages = listOf(request, notification)
        )

        // Send the batch through the first transport
        jsonRpcTransport1.sendChannel.send(batch)

        // Receive the batch from the second transport
        val received = jsonRpcTransport2.receiveFlow.first()

        // Verify the received batch
        assertTrue(received is JsonRpcClientMessageBatch)
        assertEquals(2, (received as JsonRpcClientMessageBatch).messages.size)
        assertTrue(received.messages[0] is JsonRpcRequest)
        assertEquals(request.id, (received.messages[0] as JsonRpcRequest).id)
        assertEquals(request.method, (received.messages[0] as JsonRpcRequest).method)
        assertTrue(received.messages[1] is JsonRpcNotification)
        assertEquals(notification.method, (received.messages[1] as JsonRpcNotification).method)
    }

    @Test
    fun `test JsonRpcServerMessageBatch through transport`() = runTest {
        // Create a pair of transports
        val transportPair = InMemoryTransport()
        val jsonRpcTransport1 = transportPair.first.asJsonTransport().asJsonRpcTransport()
        val jsonRpcTransport2 = transportPair.second.asJsonTransport().asJsonRpcTransport()

        // Create a success response and a fail response
        val successResponse = JsonRpcSuccessResponse(
            id = RequestId.NumberId(1),
            result = JsonPrimitive("success")
        )

        val failResponse = JsonRpcFailResponse(
            id = RequestId.NumberId(2),
            error = JsonRpcFailResponse.Error(
                code = JsonRpcFailResponse.Error.Code.MethodNotFound,
                message = "Method not found"
            )
        )

        // Create a batch
        val batch = JsonRpcServerMessageBatch(
            messages = listOf(successResponse, failResponse)
        )

        // Send the batch through the first transport
        jsonRpcTransport1.sendChannel.send(batch)

        // Receive the batch from the second transport
        val received = jsonRpcTransport2.receiveFlow.first()

        // Verify the received batch
        assertTrue(received is JsonRpcServerMessageBatch)
        assertEquals(2, (received as JsonRpcServerMessageBatch).messages.size)
        assertTrue(received.messages[0] is JsonRpcSuccessResponse)
        assertEquals(successResponse.id, (received.messages[0] as JsonRpcSuccessResponse).id)
        assertEquals(successResponse.result, (received.messages[0] as JsonRpcSuccessResponse).result)
        assertTrue(received.messages[1] is JsonRpcFailResponse)
        assertEquals(failResponse.id, (received.messages[1] as JsonRpcFailResponse).id)
        assertEquals(failResponse.error.code, (received.messages[1] as JsonRpcFailResponse).error.code)
        assertEquals(failResponse.error.message, (received.messages[1] as JsonRpcFailResponse).error.message)
    }

    @Test
    fun `test error codes`() {
        // Test predefined error codes
        assertEquals(-1, JsonRpcFailResponse.Error.Code.ConnectionClosed.value)
        assertEquals(-2, JsonRpcFailResponse.Error.Code.RequestTimeout.value)
        assertEquals(-32700, JsonRpcFailResponse.Error.Code.ParseError.value)
        assertEquals(-32600, JsonRpcFailResponse.Error.Code.InvalidRequest.value)
        assertEquals(-32601, JsonRpcFailResponse.Error.Code.MethodNotFound.value)
        assertEquals(-32602, JsonRpcFailResponse.Error.Code.InvalidParams.value)
        assertEquals(-32603, JsonRpcFailResponse.Error.Code.InternalError.value)

        // Test custom error code
        val customCode = JsonRpcFailResponse.Error.Code.Custom(123)
        assertEquals(123, customCode.value)
    }
}
