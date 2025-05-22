package io.genkt.jsonprc.client

import io.genkt.jsonrpc.JsonRpcFailResponse
import io.genkt.jsonrpc.JsonRpcRequest
import io.genkt.jsonrpc.JsonRpcServerSingleMessage
import kotlin.time.Duration

/**
 * Exception thrown when the JSON-RPC server responds with [JsonRpcFailResponse].
 *
 * @property error The [JsonRpcFailResponse.Error] object received from the server.
 */
public class JsonRpcResponseException(public val error: JsonRpcFailResponse.Error) :
    RuntimeException("Server response with error:(${error.code}) ${error.message}")

/**
 * Exception thrown when a request ID is not found for a received server message.
 * This typically indicates a mismatch or an unexpected server message.
 *
 * @property response The [JsonRpcServerSingleMessage] that caused the exception.
 */
public class JsonRpcRequestIdNotFoundException(
    public val response: JsonRpcServerSingleMessage,
) : RuntimeException("Request id not found: $response")