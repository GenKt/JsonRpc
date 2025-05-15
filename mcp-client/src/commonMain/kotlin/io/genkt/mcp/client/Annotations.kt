package io.genkt.mcp.client

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This API is expected to be used only to intercept MCP client behaviours. It is not intended for any other use.",
)
@Retention(AnnotationRetention.BINARY)
public annotation class McpClientInterceptionApi