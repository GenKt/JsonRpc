package io.genkt.jsonprc.client

import io.genkt.jsonrpc.JsonRpcFailResponse
import io.genkt.jsonrpc.JsonRpcRequest
import io.genkt.jsonrpc.JsonRpcServerSingleMessage
import kotlin.time.Duration

public class JsonRpcResponseException(public val error: JsonRpcFailResponse.Error) :
    RuntimeException("Server response with error:(${error.code}) ${error.message}")

public class JsonRpcRequestIdNotFoundException(
    public val response: JsonRpcServerSingleMessage,
) : RuntimeException("Request id not found: $response")