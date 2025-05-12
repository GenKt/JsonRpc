package io.github.genkt.jsonrpc.test

import io.github.genkt.jsonprc.client.JsonRpcClient
import io.github.genkt.jsonrpc.*
import io.github.genkt.jsonrpc.server.JsonRpcServer
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*
import kotlin.time.Duration.Companion.minutes

class JsonRpcTest {
    val transportPair = InMemoryTransport()

    val client = JsonRpcClient(
        transportPair.first.asJsonTransport().asJsonRpcClientTransport(),
        timeOut = 1.minutes,
    )

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
            null
        }
    )

    @Test
    fun `should call echo method with correct params`() = runTest {
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