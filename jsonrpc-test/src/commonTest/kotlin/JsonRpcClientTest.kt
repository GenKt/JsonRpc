package io.github.genkt.jsonrpc.test

import io.github.genkt.jsonprc.client.JsonRpcClient
import io.github.genkt.jsonprc.client.JsonRpcTimeoutException
import io.github.genkt.jsonprc.client.sendNotification
import io.github.genkt.jsonprc.client.sendRequest
import io.github.genkt.jsonrpc.*
import io.github.genkt.jsonrpc.transport.memory.InMemoryTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

class JsonRpcClientTest {
    @Test
    fun `test client can send request and receive response`() = runTest {
        withContext(Dispatchers.Default) {
            // Create a pair of transports
            val transportPair = InMemoryTransport()

            // Create client and server transports
            val clientTransport = transportPair.first.asJsonTransport().asJsonRpcClientTransport()
            val serverTransport = transportPair.second.asJsonTransport().asJsonRpcServerTransport()

            // Create a client
            val client = JsonRpcClient(clientTransport)

            // Start a coroutine to handle server-side messages
            val serverJob = launch {
                // Listen for requests on the server transport
                val request = serverTransport.receiveFlow.first()
                assertTrue(request is JsonRpcRequest)
                assertEquals("test", request.method)
                assertEquals("value", request.params.jsonObject["param"]?.jsonPrimitive?.content)

                // Send a response
                val response = JsonRpcSuccessResponse(
                    id = request.id,
                    result = buildJsonObject {
                        put("result", "success")
                    }
                )
                serverTransport.sendChannel.send(response)
            }

            // Send a request from the client
            val response = client.sendRequest(
                id = RequestId.NumberId(1),
                method = "test",
                params = buildJsonObject {
                    put("param", "value")
                }
            )

            // Verify the response
            assertEquals("success", response.result.jsonObject["result"]?.jsonPrimitive?.content)

            // Clean up
            client.close()
        }
    }

    @Test
    fun `test client can send notification`() = runTest {
        withContext(Dispatchers.Default) {
            // Create a pair of transports
            val transportPair = InMemoryTransport()

            // Create client and server transports
            val clientTransport = transportPair.first.asJsonTransport().asJsonRpcClientTransport()
            val serverTransport = transportPair.second.asJsonTransport().asJsonRpcServerTransport()

            // Create a client
            val client = JsonRpcClient(clientTransport)

            // Start a coroutine to handle server-side messages
            val serverJob = launch {
                // Listen for notifications on the server transport
                val notification = serverTransport.receiveFlow.first()
                assertTrue(notification is JsonRpcNotification)
                assertEquals("notify", (notification as JsonRpcNotification).method)
                assertEquals("event", notification.params.jsonObject["type"]?.jsonPrimitive?.content)
            }

            // Send a notification from the client
            client.sendNotification(
                method = "notify",
                params = buildJsonObject {
                    put("type", "event")
                }
            )

            // Wait for the server to process the notification
            delay(100)

            // Clean up
            client.close()
        }
    }

    @Test
    fun `test client timeout`() = runTest {
        withContext(Dispatchers.Default) {
            // Create a pair of transports
            val transportPair = InMemoryTransport()

            // Create client transport
            val clientTransport = transportPair.first.asJsonTransport().asJsonRpcClientTransport()

            // Create a client with a short timeout
            val client = JsonRpcClient(
                transport = clientTransport,
                timeOut = 100.milliseconds
            )

            // Send a request from the client and expect a timeout
            val exception = assertFails {
                client.sendRequest(
                    id = RequestId.NumberId(1),
                    method = "test"
                )
            }

            // Verify the exception is a JsonRpcTimeoutException
            assertTrue(exception is JsonRpcTimeoutException)

            // Clean up
            client.close()
        }
    }

    @Test
    fun `test client can send request with direct send method`() = runTest {
        withContext(Dispatchers.Default) {
            // Create a pair of transports
            val transportPair = InMemoryTransport()

            // Create client and server transports
            val clientTransport = transportPair.first.asJsonTransport().asJsonRpcClientTransport()
            val serverTransport = transportPair.second.asJsonTransport().asJsonRpcServerTransport()

            // Create a client
            val client = JsonRpcClient(clientTransport)

            // Start a coroutine to handle server-side messages
            val serverJob = launch {
                // Listen for requests on the server transport
                val request = serverTransport.receiveFlow.first()
                assertTrue(request is JsonRpcRequest)
                assertEquals("test", (request as JsonRpcRequest).method)

                // Send a response
                val response = JsonRpcSuccessResponse(
                    id = request.id,
                    result = JsonPrimitive("success")
                )
                serverTransport.sendChannel.send(response)
            }

            // Create a request
            val request = JsonRpcRequest(
                id = RequestId.NumberId(1),
                method = "test"
            )

            // Send the request directly
            val response = client.send(request)

            // Verify the response
            assertEquals("success", response.result.jsonPrimitive.content)

            // Clean up
            client.close()
        }
    }

    @Test
    fun `test client can send notification with direct send method`() = runTest {
        withContext(Dispatchers.Default) {
            // Create a pair of transports
            val transportPair = InMemoryTransport()

            // Create client and server transports
            val clientTransport = transportPair.first.asJsonTransport().asJsonRpcClientTransport()
            val serverTransport = transportPair.second.asJsonTransport().asJsonRpcServerTransport()

            // Create a client
            val client = JsonRpcClient(clientTransport)

            // Start a coroutine to handle server-side messages
            val serverJob = launch {
                // Listen for notifications on the server transport
                val notification = serverTransport.receiveFlow.first()
                assertTrue(notification is JsonRpcNotification)
                assertEquals("notify", (notification as JsonRpcNotification).method)
            }

            // Create a notification
            val notification = JsonRpcNotification(
                method = "notify"
            )

            // Send the notification directly
            client.send(notification)

            // Wait for the server to process the notification
            delay(100)

            // Clean up
            client.close()
        }
    }
}
