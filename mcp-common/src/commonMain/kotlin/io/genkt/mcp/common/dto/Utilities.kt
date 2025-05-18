package io.genkt.mcp.common.dto

import io.genkt.jsonrpc.RequestId
import io.genkt.mcp.common.McpMethods
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.DeserializationStrategy
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
    /**
     * This request wraps a [RawClientRequest] to enable sending [Notification]s.
     * The server developer can pass a [progressChannel] to receive [Notification]s.
     * The client developer can send [Notification]s to the server through [progressChannel].
     */
    public data class ClientRequest<Result, Request : McpClientRequest<Result>>(
        public val rawRequest: RawClientRequest<Result, Request>,
        public val progressChannel: SendChannel<Notification>,
    ) : McpProgressRequest<Result, McpClientRequest<Result>> by rawRequest, McpClientRequest<Result> {
        public override val resultDeserializer: DeserializationStrategy<Result> by rawRequest::resultDeserializer
    }

    /**
     * This request wraps a [RawServerRequest] to enable sending [Notification]s.
     * The server developer can pass a [progressChannel] to receive [Notification]s.
     * The client developer can send [Notification]s to the server through [progressChannel].
     */
    public data class ServerRequest<Result, Request : McpServerRequest<Result>>(
        public val rawRequest: RawServerRequest<Result, Request>,
        public val progressChannel: SendChannel<Notification>,
    ) : McpProgressRequest<Result, McpServerRequest<Result>> by rawRequest, McpServerRequest<Result> {
        public override val resultDeserializer: DeserializationStrategy<Result> by rawRequest::resultDeserializer
    }

    /**
     * This is the raw request content transferred to the server.
     * The server should wrap it as [ServerRequest] to enable sending [Notification]s.
     */
    @Serializable
    public data class RawClientRequest<Result, Request : McpClientRequest<Result>>(
        public override val request: Request,
        public override val token: Token,
    ) : McpProgressRequest<Result, McpClientRequest<Result>>, McpClientRequest<Result> by request

    /**
     * This is the raw request content transferred to the client.
     * The client should wrap it as [ClientRequest] to enable sending [Notification]s.
     */
    @Serializable
    public data class RawServerRequest<Result, Request : McpServerRequest<Result>>(
        public override val request: Request,
        public override val token: Token,
    ) : McpProgressRequest<Result, McpServerRequest<Result>>, McpServerRequest<Result> by request

    @Serializable
    public data class Notification(
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