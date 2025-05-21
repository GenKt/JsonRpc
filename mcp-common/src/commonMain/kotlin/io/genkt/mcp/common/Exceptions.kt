package io.genkt.mcp.common

import io.genkt.jsonrpc.JsonRpcClientSingleMessage

public class McpMethodNotFoundException(
    public val originalCall: JsonRpcClientSingleMessage
) : RuntimeException("Method ${originalCall.method} not found in: $originalCall")

public class McpParamParseException(
    public val originalCall: JsonRpcClientSingleMessage,
    public override val message: String,
) : RuntimeException("Fail to parse JsonRpc param: $message in: $originalCall")