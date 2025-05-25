package io.genkt.mcp.common

import io.genkt.jsonrpc.JsonRpcClientSingleMessage
import io.genkt.jsonrpc.JsonRpcFailResponse

public class McpMethodNotFoundException(
    public val originalCall: JsonRpcClientSingleMessage
) : RuntimeException("Method ${originalCall.method} not found in: $originalCall")

public class McpParamParseException(
    public val originalCall: JsonRpcClientSingleMessage,
    public override val message: String,
) : RuntimeException("Fail to parse JsonRpc param: $message in: $originalCall")

/**
 * This exception can be thrown when the client or server want to response with [JsonRpcFailResponse].
 * The framework will catch this exception and send a [JsonRpcFailResponse] to where the request comes from.
 * Then the sender will receive a [JsonRpcFailResponse] and throw this exception again.
 *
 * @param error The [JsonRpcFailResponse.Error] in the [JsonRpcFailResponse].
 */
public class McpErrorResponseException(
    public val error: JsonRpcFailResponse.Error,
): RuntimeException("Get response with error:(${error.code}) ${error.message}")