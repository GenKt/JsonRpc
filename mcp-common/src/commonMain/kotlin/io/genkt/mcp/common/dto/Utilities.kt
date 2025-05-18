package io.genkt.mcp.common.dto

import io.genkt.jsonrpc.RequestId
import io.genkt.mcp.common.McpMethods
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

public sealed interface McpUtilities {
    public data object Ping : McpClientRequest<Pong>, McpServerRequest<Pong> {
        override val method: String get() = McpMethods.Ping
    }

    public data object Pong
    @Serializable
    public data class Cancellation(
        public val requestId: RequestId,
        public val reason: String? = null,
    ) : McpClientNotification, McpServerNotification {
        override val method: String get() = McpMethods.Notifications.Cancelled
    }
}

public sealed interface McpProgress {
    // TODO: serializing
    @Serializable
    public data class ClientRequest<Result, Request : McpClientRequest<Result>>(
        public override val request: Request,
        public override val token: Token,
    ) : McpClientProgressRequest<Result> {
        override val method: String by request::method
    }

    @Serializable
    public data class ServerRequest<Result, Request : McpServerRequest<Result>>(
        public override val request: Request,
        public override val token: Token,
    ) : McpServerProgressRequest<Result> {
        override val method: String by request::method
    }

    @Serializable
    public data class ProgressNotification(
        public val progressToken: Token,
        public val progress: Double,
        public val total: Double? = null,
        public val message: String? = null,
    ) : McpClientNotification, McpServerNotification {
        override val method: String get() = McpMethods.Notifications.Progress
    }

    @Serializable
    public sealed interface Token {
        @Serializable
        @JvmInline
        public value class StringToken(public val value: String) : Token

        @Serializable
        @JvmInline
        public value class IntegerToken(public val value: Long) : Token
    }
}