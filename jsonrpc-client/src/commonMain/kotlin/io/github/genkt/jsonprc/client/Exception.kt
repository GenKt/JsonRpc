package io.github.genkt.jsonprc.client

import io.modelcontextprotocol.kotlin.sdk.JsonRpcFailResponse
import io.modelcontextprotocol.kotlin.sdk.JsonRpcRequest
import io.modelcontextprotocol.kotlin.sdk.RequestId
import kotlin.time.Duration

public class JsonRpcResponseException(public val error: JsonRpcFailResponse.Error):
    RuntimeException("Server response with error:(${error.code}) ${error.message}")

public class JsonRpcTimeoutException(
    public val originalRequest: JsonRpcRequest,
    public val timeout: Duration,
): RuntimeException("Server response timeout exceeded: ($timeout), with request: $originalRequest")

public class JsonRpcRequestIdNotFoundException(
    public val requestId: RequestId,
): RuntimeException("Request id not found: $requestId")