package io.genkt.mcp.client

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "It is not safe to use this API directly. Please consider carefully and understand the consequences before using it."
)
@Retention(AnnotationRetention.BINARY)
public annotation class InternalMcpClientApi