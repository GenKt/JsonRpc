package io.github.genkt.jsonrpc.test

import io.github.genkt.jsonrpc.*
import io.github.genkt.jsonrpc.transport.memory.InMemoryTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import kotlin.test.*

class TransportTest {
    @Test
    fun `test StringTransport to JsonTransport conversion`() = runTest {
        withContext(Dispatchers.Default) {
            // Create a pair of string transports
            val transportPair = InMemoryTransport()
            
            // Convert to JsonTransport
            val jsonTransport1 = transportPair.first.asJsonTransport()
            val jsonTransport2 = transportPair.second.asJsonTransport()
            
            // Send a JSON element through the first transport
            val jsonElement = buildJsonObject {
                put("key", "value")
            }
            jsonTransport1.sendChannel.send(jsonElement)
            
            // Receive the JSON element from the second transport
            val received = jsonTransport2.receiveFlow.first()
            
            // Verify the received JSON element
            assertEquals(jsonElement, received)
        }
    }
    
    @Test
    fun `test JsonTransport to JsonRpcTransport conversion`() = runTest {
        withContext(Dispatchers.Default) {
            // Create a pair of string transports
            val transportPair = InMemoryTransport()
            
            // Convert to JsonTransport and then to JsonRpcTransport
            val jsonRpcTransport1 = transportPair.first.asJsonTransport().asJsonRpcTransport()
            val jsonRpcTransport2 = transportPair.second.asJsonTransport().asJsonRpcTransport()
            
            // Create a JSON-RPC request
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
    }
    
    @Test
    fun `test JsonTransport to JsonRpcClientTransport conversion`() = runTest {
        withContext(Dispatchers.Default) {
            // Create a pair of string transports
            val transportPair = InMemoryTransport()
            
            // Convert to JsonTransport and then to JsonRpcClientTransport and JsonRpcServerTransport
            val clientTransport = transportPair.first.asJsonTransport().asJsonRpcClientTransport()
            val serverTransport = transportPair.second.asJsonTransport().asJsonRpcServerTransport()
            
            // Create a JSON-RPC request
            val request = JsonRpcRequest(
                id = RequestId.NumberId(1),
                method = "test",
                params = buildJsonObject {
                    put("param", "value")
                }
            )
            
            // Send the request through the client transport
            clientTransport.sendChannel.send(request)
            
            // Receive the request from the server transport
            val received = serverTransport.receiveFlow.first()
            
            // Verify the received request
            assertTrue(received is JsonRpcRequest)
            assertEquals(request.id, (received as JsonRpcRequest).id)
            assertEquals(request.method, received.method)
            assertEquals(request.params, received.params)
            
            // Create a JSON-RPC response
            val response = JsonRpcSuccessResponse(
                id = RequestId.NumberId(1),
                result = buildJsonObject {
                    put("result", "success")
                }
            )
            
            // Send the response through the server transport
            serverTransport.sendChannel.send(response)
            
            // Receive the response from the client transport
            val receivedResponse = clientTransport.receiveFlow.first()
            
            // Verify the received response
            assertTrue(receivedResponse is JsonRpcSuccessResponse)
            assertEquals(response.id, (receivedResponse as JsonRpcSuccessResponse).id)
            assertEquals(response.result, receivedResponse.result)
        }
    }
}