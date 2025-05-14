package io.github.genkt.jsonrpc.test

import io.genkt.jsonprc.client.JsonRpcClient
import io.genkt.jsonprc.client.interceptRequest
import io.genkt.jsonprc.client.intercepted
import io.genkt.jsonprc.client.sendRequest
import io.genkt.jsonrpc.JsonRpc
import io.genkt.jsonrpc.JsonRpcSuccessResponse
import io.genkt.jsonrpc.RequestId
import io.genkt.jsonrpc.asJsonRpcClientTransport
import io.genkt.jsonrpc.asJsonRpcServerTransport
import io.genkt.jsonrpc.asJsonTransport
import io.genkt.jsonrpc.server.JsonRpcServer
import io.genkt.jsonrpc.server.interceptRequestHandler
import io.genkt.jsonrpc.server.intercepted
import io.github.genkt.jsonrpc.transport.memory.InMemoryTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals

class InterceptionTest {
    @Test
    fun `should intercept server correctly`() {
        CoroutineScope(EmptyCoroutineContext).launch {
            val (clientTransport, serverTransport) = InMemoryTransport<String>().run {
                (first.asJsonTransport().asJsonRpcClientTransport()) to (second.asJsonTransport()
                    .asJsonRpcServerTransport())
            }
            val client = JsonRpcClient(
                clientTransport,
            )
            val server = JsonRpcServer(
                serverTransport,
                { request ->
                    JsonRpcSuccessResponse(
                        id = request.id,
                        result = JsonRpc.json.encodeToJsonElement(String.serializer(), "Hello"),
                    )
                },
                {}
            ).intercepted {
                interceptRequestHandler {
                    return@interceptRequestHandler { request ->
                        JsonRpcSuccessResponse(
                            id = request.id,
                            result = JsonRpc.json.encodeToJsonElement(String.serializer(), "World"),
                        )
                    }
                }
            }
            server.start()
            val response = client.sendRequest(
                id = RequestId.NumberId(1),
                method = "test",
                params = JsonNull,
            )
            assertEquals("World", response.result.jsonPrimitive.content)
        }
    }

    @Test
    fun `should intercept client correctly`() {
        CoroutineScope(EmptyCoroutineContext).launch {
            val (clientTransport, serverTransport) = InMemoryTransport<String>().run {
                (first.asJsonTransport().asJsonRpcClientTransport()) to (second.asJsonTransport()
                    .asJsonRpcServerTransport())
            }
            JsonRpcServer(
                serverTransport,
                { request ->
                    JsonRpcSuccessResponse(
                        id = request.id,
                        result = JsonRpc.json.encodeToJsonElement(String.serializer(), "Hello"),
                    )
                },
                {}
            ).start()
            val client = JsonRpcClient(
                clientTransport,
            ).intercepted {
                interceptRequest { sendFunction ->
                    { request ->
                        val response = sendFunction(request)
                        JsonRpcSuccessResponse(
                            id = response.id,
                            result = JsonRpc.json.encodeToJsonElement(String.serializer(), "World"),
                        )
                    }
                }
            }
            val response = client.sendRequest(
                id = RequestId.NumberId(1),
                method = "test",
                params = JsonNull,
            )
            assertEquals("World", response.result.jsonPrimitive.content)
        }
    }
}