package io.genkt.mcp.common.dto

import io.genkt.jsonrpc.RequestId
import io.genkt.mcp.common.McpMethods
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

public sealed interface McpUtilities {
    @Serializable
    public data object Ping : McpClientBasicRequest<Pong>, McpServerBasicRequest<Pong> {
        override val method: String get() = McpMethods.Ping
        override val resultSerializer: KSerializer<Pong> get() = Pong.serializer()
    }

    @Serializable
    public data object Pong

    @Serializable
    public data class Cancellation(
        public val requestId: RequestId,
        public val reason: String? = null,
    ) : McpClientBasicNotification, McpServerBasicNotification {
        override val method: String get() = McpMethods.Notifications.Cancelled
    }
}

@Serializable
public data class McpProgress(
    public val progress: Double,
    public val total: Double? = null,
    public val message: String? = null,
) {
    @Serializable
    public data class Notification(
        public val progressToken: Token,
        public val progress: Double,
        public val total: Double? = null,
        public val message: String? = null,
    ) : McpClientBasicNotification, McpServerBasicNotification {
        override val method: String get() = McpMethods.Notifications.Progress
    }

    public fun withToken(token: Token): Notification =
        Notification(progressToken = token, progress = progress, total = total, message = message)

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