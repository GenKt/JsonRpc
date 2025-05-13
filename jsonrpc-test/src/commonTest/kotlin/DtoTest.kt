package io.github.genkt.jsonrpc.test

import io.genkt.jsonrpc.InvalidRequest
import io.genkt.jsonrpc.JsonRpc
import io.genkt.jsonrpc.JsonRpcClientMessageBatch
import io.genkt.jsonrpc.JsonRpcFailResponse
import io.genkt.jsonrpc.JsonRpcNotification
import io.genkt.jsonrpc.JsonRpcRequest
import io.genkt.jsonrpc.JsonRpcServerMessageBatch
import io.genkt.jsonrpc.JsonRpcSuccessResponse
import io.genkt.jsonrpc.MethodNotFound
import io.genkt.jsonrpc.RequestId
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

class DtoTest {
    @Test
    fun `RequestId serialization roundtrip`() {
        val ids = listOf(
            RequestId.StringId("abc"),
            RequestId.NumberId(42),
            RequestId.NullId
        )
        ids.forEach { id ->
            val element = JsonRpc.json.encodeToJsonElement(RequestId.serializer(), id)
            val decoded = JsonRpc.json.decodeFromJsonElement(RequestId.serializer(), element)
            assertEquals(id, decoded)
        }
    }

    @Test
    fun `JsonRpcRequest serialization roundtrip`() {
        val req = JsonRpcRequest(
            id = RequestId.NumberId(1),
            method = "m",
            params = buildJsonObject { put("p", 123) }
        )
        val element = JsonRpc.json.encodeToJsonElement(JsonRpcRequest.serializer(), req)
        val decoded = JsonRpc.json.decodeFromJsonElement(JsonRpcRequest.serializer(), element)
        assertEquals(req, decoded)
    }

    @Test
    fun `JsonRpcNotification serialization roundtrip`() {
        val notif = JsonRpcNotification(
            method = "n",
            params = buildJsonObject { put("x", true) }
        )
        val element = JsonRpc.json.encodeToJsonElement(JsonRpcNotification.serializer(), notif)
        val decoded = JsonRpc.json.decodeFromJsonElement(JsonRpcNotification.serializer(), element)
        assertEquals(notif, decoded)
    }

    @Test
    fun `JsonRpcSuccessResponse serialization roundtrip`() {
        val resp = JsonRpcSuccessResponse(
            id = RequestId.StringId("ok"),
            result = buildJsonObject { put("r", "v") }
        )
        val element = JsonRpc.json.encodeToJsonElement(JsonRpcSuccessResponse.serializer(), resp)
        val decoded = JsonRpc.json.decodeFromJsonElement(JsonRpcSuccessResponse.serializer(), element)
        assertEquals(resp, decoded)
    }

    @Test
    fun `JsonRpcFailResponse serialization roundtrip`() {
        val error = JsonRpcFailResponse.Error(
            code = JsonRpcFailResponse.Error.Code.MethodNotFound,
            message = "err",
            data = buildJsonObject { put("d", 1) }
        )
        val resp = JsonRpcFailResponse(
            id = RequestId.NumberId(2),
            error = error
        )
        val element = JsonRpc.json.encodeToJsonElement(JsonRpcFailResponse.serializer(), resp)
        val decoded = JsonRpc.json.decodeFromJsonElement(JsonRpcFailResponse.serializer(), element)
        assertEquals(resp, decoded)
    }

    @Test
    fun `JsonRpcClientMessageBatch serialization roundtrip`() {
        val req = JsonRpcRequest(RequestId.NumberId(3), "a")
        val notif = JsonRpcNotification("b")
        val batch = JsonRpcClientMessageBatch(listOf(req, notif))
        val element = JsonRpc.json.encodeToJsonElement(JsonRpcClientMessageBatch.serializer(), batch)
        val decoded = JsonRpc.json.decodeFromJsonElement(JsonRpcClientMessageBatch.serializer(), element)
        assertEquals(batch, decoded)
    }

    @Test
    fun `JsonRpcServerMessageBatch serialization roundtrip`() {
        val suc = JsonRpcSuccessResponse(RequestId.NumberId(4), result = JsonPrimitive("ok"))
        val err = JsonRpcFailResponse(
            RequestId.NumberId(5),
            JsonRpcFailResponse.Error(JsonRpcFailResponse.Error.Code.InvalidRequest, "x")
        )
        val batch = JsonRpcServerMessageBatch(listOf(suc, err))
        val element = JsonRpc.json.encodeToJsonElement(JsonRpcServerMessageBatch.serializer(), batch)
        val decoded = JsonRpc.json.decodeFromJsonElement(JsonRpcServerMessageBatch.serializer(), element)
        assertEquals(batch, decoded)
    }
}
