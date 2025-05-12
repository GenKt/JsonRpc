package io.github.genkt.jsonrpc.test

import io.github.genkt.jsonrpc.*
import io.github.genkt.jsonrpc.server.JsonRpcServer
import io.github.genkt.jsonrpc.transport.memory.InMemoryTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import kotlin.test.*

class JsonRpcServerTest {
    @Test
    fun `test server handles request and sends response`() = runTest {
        withContext(Dispatchers.Default) {
            // Create a pair of transports
            val transportPair = InMemoryTransport()
            
            // Create client and server transports
            val clientTransport = transportPair.first.asJsonTransport().asJsonRpcClientTransport()
            val serverTransport = transportPair.second.asJsonTransport().asJsonRpcServerTransport()
            
            // Create a server
            val server = JsonRpcServer(
                transport = serverTransport,
                onRequest = { request ->
                    // Echo the method name in the result
                    JsonRpcSuccessResponse(
                        id = request.id,
                        result = JsonPrimitive("Method: ${request.method}")
                    )
                },
                onNotification = { }
            )
            
            // Send a request from the client side
            val request = JsonRpcRequest(
                id = RequestId.NumberId(1),
                method = "test"
            )
            clientTransport.sendChannel.send(request)
            
            // Receive the response
            val response = clientTransport.receiveFlow.first()
            
            // Verify the response
            assertTrue(response is JsonRpcSuccessResponse)
            assertEquals(request.id, (response as JsonRpcSuccessResponse).id)
            assertEquals("Method: test", response.result.jsonPrimitive.content)
            
            // Clean up
            server.close()
        }
    }
    
    @Test
    fun `test server handles notification`() = runTest {
        withContext(Dispatchers.Default) {
            // Create a pair of transports
            val transportPair = InMemoryTransport()
            
            // Create client and server transports
            val clientTransport = transportPair.first.asJsonTransport().asJsonRpcClientTransport()
            val serverTransport = transportPair.second.asJsonTransport().asJsonRpcServerTransport()
            
            // Track received notifications
            val receivedNotifications = mutableListOf<JsonRpcNotification>()
            
            // Create a server
            val server = JsonRpcServer(
                transport = serverTransport,
                onRequest = { request ->
                    JsonRpcSuccessResponse(
                        id = request.id,
                        result = JsonNull
                    )
                },
                onNotification = { notification ->
                    receivedNotifications.add(notification)
                }
            )
            
            // Send a notification from the client side
            val notification = JsonRpcNotification(
                method = "notify",
                params = buildJsonObject {
                    put("type", "event")
                }
            )
            clientTransport.sendChannel.send(notification)
            
            // Wait for the server to process the notification
            delay(100)
            
            // Verify the notification was received
            assertEquals(1, receivedNotifications.size)
            assertEquals("notify", receivedNotifications[0].method)
            assertEquals("event", receivedNotifications[0].params.jsonObject["type"]?.jsonPrimitive?.content)
            
            // Clean up
            server.close()
        }
    }
    
    @Test
    fun `test server handles batch request`() = runTest {
        withContext(Dispatchers.Default) {
            // Create a pair of transports
            val transportPair = InMemoryTransport()
            
            // Create client and server transports
            val clientTransport = transportPair.first.asJsonTransport().asJsonRpcClientTransport()
            val serverTransport = transportPair.second.asJsonTransport().asJsonRpcServerTransport()
            
            // Track received notifications
            val receivedNotifications = mutableListOf<JsonRpcNotification>()
            
            // Create a server
            val server = JsonRpcServer(
                transport = serverTransport,
                onRequest = { request ->
                    // Echo the method name in the result
                    JsonRpcSuccessResponse(
                        id = request.id,
                        result = JsonPrimitive("Method: ${request.method}")
                    )
                },
                onNotification = { notification ->
                    receivedNotifications.add(notification)
                }
            )
            
            // Create a batch with a request and a notification
            val request = JsonRpcRequest(
                id = RequestId.NumberId(1),
                method = "test"
            )
            
            val notification = JsonRpcNotification(
                method = "notify"
            )
            
            val batch = JsonRpcClientMessageBatch(
                messages = listOf(request, notification)
            )
            
            // Send the batch from the client side
            clientTransport.sendChannel.send(batch)
            
            // Receive the response
            val response = clientTransport.receiveFlow.first()
            
            // Verify the response
            assertTrue(response is JsonRpcSuccessResponse)
            assertEquals(request.id, (response as JsonRpcSuccessResponse).id)
            assertEquals("Method: test", response.result.jsonPrimitive.content)
            
            // Wait for the server to process the notification
            delay(100)
            
            // Verify the notification was received
            assertEquals(1, receivedNotifications.size)
            assertEquals("notify", receivedNotifications[0].method)
            
            // Clean up
            server.close()
        }
    }
}