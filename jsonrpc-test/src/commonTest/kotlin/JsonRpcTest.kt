package io.github.genkt.jsonrpc.test

import io.github.genkt.jsonprc.client.JsonRpcClient
import io.github.genkt.jsonrpc.*
import io.github.genkt.jsonrpc.server.JsonRpcServer
import io.github.genkt.jsonrpc.transport.stdio.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*

class JsonRpcTest {
    val transport = StdioTransport().asJsonTransport()

    val client = JsonRpcClient(
        transport.asJsonRpcClientTransport(),
    )

    val server = JsonRpcServer(
        transport.asJsonRpcServerTransport(),
        {
            JsonRpcSuccessResponse(
                id = it.id,
                result = JsonRpc.json.encodeToJsonElement(
                    String.serializer(),
                    "Hello, ${it.params.jsonObject["name"]?.jsonPrimitive?.content}!"
                )
            )
        },
        {
            null
        }
    )
}