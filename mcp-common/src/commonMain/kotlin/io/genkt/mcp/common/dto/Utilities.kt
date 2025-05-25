package io.genkt.mcp.common.dto

import io.genkt.jsonrpc.RequestId
import io.genkt.mcp.common.McpMethods
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

public sealed interface McpUtilities {
    @Serializable
    public data object Ping : McpClientRequest<Pong>, McpServerRequest<Pong> {
        override val method: String get() = McpMethods.Ping
        override val resultSerializer: KSerializer<Pong> get() = Pong.serializer()
    }

    @Serializable
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
    /**
     * This request wraps a [RawClientRequest] to enable sending [Notification]s.
     * The server developer can pass a [progressChannel] to receive [Notification]s.
     * The client developer can send [Notification]s to the server through [progressChannel].
     */
    @Serializable(with = McpClientProgressRequestSerializer::class)
    public data class ClientRequest<Result, Request : McpClientRequest<Result>>(
        public val rawRequest: RawClientRequest<Result, Request>,
        public val progressChannel: SendChannel<Notification>,
    ) : McpProgressRequest<Result, McpClientRequest<Result>> by rawRequest, McpClientCall<Result>

    /**
     * This request wraps a [RawServerRequest] to enable sending [Notification]s.
     * The server developer can pass a [progressChannel] to receive [Notification]s.
     * The client developer can send [Notification]s to the server through [progressChannel].
     */
    @Serializable(with = McpServerProgressRequestSerializer::class)
    public data class ServerRequest<Result, Request : McpServerRequest<Result>>(
        public val rawRequest: RawServerRequest<Result, Request>,
        public val progressChannel: SendChannel<Notification>,
    ) : McpProgressRequest<Result, McpServerRequest<Result>> by rawRequest, McpServerCall<Result>

    /**
     * This is the raw request content transferred to the server.
     * The server should wrap it as [ServerRequest] to enable sending [Notification]s.
     */
    @Serializable(with = RawMcpClientProgressRequestSerializer::class)
    public data class RawClientRequest<Result, Request : McpClientRequest<Result>>(
        public override val request: Request,
        public override val token: Token,
    ) : McpProgressRequest<Result, McpClientRequest<Result>>, McpClientCall<Result> by request

    /**
     * This is the raw request content transferred to the client.
     * The client should wrap it as [ClientRequest] to enable sending [Notification]s.
     */
    @Serializable(with = RawMcpServerProgressRequestSerializer::class)
    public data class RawServerRequest<Result, Request : McpServerRequest<Result>>(
        public override val request: Request,
        public override val token: Token,
    ) : McpProgressRequest<Result, McpServerRequest<Result>>, McpServerCall<Result> by request

    @Serializable
    public data class Notification(
        public val progressToken: Token,
        public val progress: Double,
        public val total: Double? = null,
        public val message: String? = null,
    ) : McpClientNotification, McpServerNotification {
        override val method: String get() = McpMethods.Notifications.Progress
    }

    // TODO: Serialization
    @Serializable(with = McpProgressTokenSerializer::class)
    public sealed interface Token {
        @Serializable
        @JvmInline
        public value class StringToken(public val value: String) : Token

        @Serializable
        @JvmInline
        public value class IntegerToken(public val value: Long) : Token
    }

    public companion object {
        @OptIn(ExperimentalTime::class)
        public val defaultStringTokenGenerator: () -> Token.StringToken = {
            val timestamp = Clock.System.now().toEpochMilliseconds()
            val random1 = Random.nextInt(0, 0xFFFF).toString(16).padStart(4, '0')
            val random2 = Random.nextInt(0, 0xFFFF).toString(16).padStart(4, '0')
            val random3 = Random.nextInt(0, 0xFFFF).toString(16).padStart(4, '0')
            val random4 = Random.nextInt(0, 0xFFFF).toString(16).padStart(4, '0')
            val random5 = Random.nextInt(0, 0xFFFF).toString(16).padStart(4, '0')
            val timestampHex = timestamp.toString(16)

            Token.StringToken("prog-$timestampHex-$random1-$random2-$random3-$random4-$random5")
        }
    }
}