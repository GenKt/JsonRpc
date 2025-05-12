package io.github.genkt.jsonrpc.test

import io.github.genkt.jsonprc.client.JsonRpcClient
import io.github.genkt.jsonrpc.*
import io.github.genkt.jsonrpc.server.JsonRpcServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import kotlin.test.*

class JsonRpcTest {
    val transportPair = InMemoryTransport()

    val client = JsonRpcClient(
        transportPair.first.asJsonTransport().asJsonRpcClientTransport(),
    )

    val notifications = mutableListOf<JsonRpcNotification>()

    val server = JsonRpcServer(
        transportPair.second.asJsonTransport().asJsonRpcServerTransport(),
        {
            return@JsonRpcServer when (it.method) {
                "echo" -> JsonRpcSuccessResponse(
                    id = it.id,
                    result = JsonRpc.json.encodeToJsonElement(
                        "Hello, ${it.params.jsonObject["name"]?.jsonPrimitive?.content ?: "Mr. Unknown"}!"
                    )
                )

                else -> JsonRpcFailResponse(
                    id = it.id,
                    error = JsonRpcFailResponse.Error(
                        code = JsonRpcFailResponse.Error.Code.MethodNotFound,
                        message = "Method not found",
                    ),
                )
            }
        },
        {
            notifications.add(it)
        }
    )

    @Test
    fun `should call echo method with correct params`() = runTest {
        withContext(Dispatchers.Default) {
            val response = client.sendRequest(
                RequestId.NumberId(1),
                "echo",
                buildJsonObject {
                    put("name", "World")
                }
            )
            assertEquals("Hello, World!", response.result.jsonPrimitive.content)
        }
    }

    @Test
    fun `should call echo method with incorrect params`() = runTest {
        withContext(Dispatchers.Default) {
            val response = client.sendRequest(
                RequestId.NumberId(1),
                "echo",
                buildJsonObject {}
            )
            assertEquals("Hello, Mr. Unknown!", response.result.jsonPrimitive.content)
        }
    }

    @Test
    fun `should receive notification`() = runTest {
        withContext(Dispatchers.Default) {
            client.sendNotification(
                method = "notify"
            )
            delay(100)
            assertEquals("notify", notifications.first().method)
        }
    }
}