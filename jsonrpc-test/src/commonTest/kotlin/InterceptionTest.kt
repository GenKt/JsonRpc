package io.github.genkt.jsonrpc.test

import io.genkt.jsonprc.client.JsonRpcClient
import io.genkt.jsonprc.client.interceptNotification
import io.genkt.jsonprc.client.interceptRequest
import io.genkt.jsonprc.client.sendRequest
import io.genkt.jsonrpc.*
import io.genkt.jsonrpc.server.JsonRpcServer
import io.github.genkt.jsonrpc.transport.memory.InMemoryTransport
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class InterceptionTest {
    @Test
    fun `should intercept server correctly`() = runTest {
        val (clientTransport, serverTransport) = InMemoryTransport<JsonElement>()
        val client = JsonRpcClient(
            clientTransport.asJsonRpcClientTransport(),
        )
        val server = JsonRpcServer {
            transport = serverTransport.asJsonRpcServerTransport()
            onRequest = { request ->
                JsonRpcSuccessResponse(
                    id = request.id,
                    result = JsonRpc.json.encodeToJsonElement(String.serializer(), "Hello"),
                )
            }
            onNotification = {}
            requestInterceptor += { oldHandler ->
                { request ->
                    JsonRpcSuccessResponse(
                        id = request.id,
                        result = JsonRpc.json.encodeToJsonElement(String.serializer(), "World"),
                    )
                }
            }
        }
        server.start()
        client.start()
        val response = client.sendRequest(
            id = RequestId.NumberId(1),
            method = "test",
            params = JsonNull,
        )
        assertEquals("World", response.result.jsonPrimitive.content)
        client.close()
        server.close()
    }


    @Test
    fun `should intercept client correctly`() = runTest {
        val (clientTransport, serverTransport) = InMemoryTransport<JsonElement>()
        val client = JsonRpcClient {
            transport = clientTransport.asJsonRpcClientTransport()
            additionalCoroutineContext += CoroutineName("Client")
            interceptRequest { handler ->
                { request ->
                    JsonRpcSuccessResponse(
                        id = request.id,
                        result = JsonPrimitive("World"),
                    )
                }
            }
            @Suppress("RedundantUnitExpression")
            interceptNotification { handler -> { Unit } }
        }
        val server = JsonRpcServer(
            serverTransport.asJsonRpcServerTransport(),
            { request ->
                JsonRpcSuccessResponse(
                    id = request.id,
                    result = JsonRpc.json.encodeToJsonElement(String.serializer(), "Hello"),
                )
            },
            {},
            {},
            CoroutineName("Server")
        )
        server.start()
        client.start()
        val response = client.sendRequest(
            id = RequestId.NumberId(1),
            method = "test",
            params = JsonNull,
        )
        assertEquals("World", response.result.jsonPrimitive.content)
        client.close()
        server.close()
    }
}