package io.genkt.mcp.common

import io.genkt.jsonrpc.JsonRpcClientSingleMessage
import io.genkt.jsonrpc.JsonRpcFailResponse
import io.genkt.mcp.common.dto.McpServerRawNotification

@Suppress("CanBeParameter")
public class McpMethodNotFoundException(
    public val jsonRpcMessage: JsonRpcClientSingleMessage
) : RuntimeException("Method ${jsonRpcMessage.method} not found in: $jsonRpcMessage")

public class McpParamParseException(
    public val jsonRpcMessage: JsonRpcClientSingleMessage,
    cause: Throwable
) : RuntimeException(cause)

/**
 * This exception can be thrown when the client or server want to response with [JsonRpcFailResponse].
 * The framework will catch this exception and send a [JsonRpcFailResponse] to where the request comes from.
 * Then the sender will receive a [JsonRpcFailResponse] and throw this exception again.
 *
 * @param error The [JsonRpcFailResponse.Error] in the [JsonRpcFailResponse].
 */
public class McpErrorResponseException(
    public val error: JsonRpcFailResponse.Error,
) : RuntimeException("Get response with error:(${error.code}) ${error.message}")

public class McpServerNotificationException(
    public val notification: McpServerRawNotification,
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)