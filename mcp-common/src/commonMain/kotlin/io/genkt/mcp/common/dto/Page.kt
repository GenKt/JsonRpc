package io.genkt.mcp.common.dto

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

public sealed interface McpPaginated {
    @Serializable
    @JvmInline
    public value class Cursor(public val value: String)
    public interface Request {
        public val cursor: Cursor?
    }

    public interface Result {
        public val nextCursor: Cursor?
    }
}