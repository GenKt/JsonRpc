package io.genkt.mcp.common.dto

import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

public sealed interface McpClientCall<R> {
    public val method: String
}

public data class McpClientRequest<Request, Result>(
    val basicRequest: Request,
    val meta: Meta? = null,
) : McpClientCall<Result> by basicRequest
        where Request : McpClientBasicRequest<Result> {
    @Serializable
    public data class Meta(
        /**
         * A non-null value of this property means that the request sender is interested in receiving progress updates.
         * The party who receives this channel should update the progress by sending [McpProgress] instances to the channel.
         *
         * This channel will be closed when the request is completed, for both parties.
         */
        val progressChannel: SendChannel<McpProgress>? = null,
    )
}

public data class McpClientNotification<Notification : McpClientBasicNotification>(
    val basicNotification: Notification,
    val meta: Meta? = null,
) : McpClientCall<Unit> by basicNotification {
    public data object Meta
}

public data class McpClientRawRequest<Request, Result>(
    public val request: Request,
    public val meta: Meta? = null,
) where Request : McpClientBasicRequest<Result> {
    @Serializable
    public data class Meta(
        public val progressToken: McpProgress.Token? = null,
    )

    public companion object
}

public data class McpClientRawNotification(
    public val notification: McpClientBasicNotification,
    public val meta: Meta? = null,
) {
    @Serializable
    public data object Meta
    public companion object
}

public sealed interface McpClientBasicRequest<R> : McpClientCall<R> {
    public val resultSerializer: KSerializer<R>

    public companion object
}

public sealed interface McpClientBasicNotification : McpClientCall<Unit> {
    public companion object
}

public sealed interface McpServerCall<R> {
    public val method: String
}

public data class McpServerRequest<Request, Result>(
    val basicRequest: Request,
    val meta: Meta? = null,
) : McpServerCall<Result> by basicRequest
        where Request : McpServerBasicRequest<Result> {
    public data class Meta(
        val progressChannel: SendChannel<McpProgress>? = null,
    )
}

public data class McpServerNotification<Notification : McpServerBasicNotification>(
    val basicNotification: Notification,
    val meta: Meta? = null,
) : McpServerCall<Unit> by basicNotification {
    public data object Meta
}

public data class McpServerRawRequest<Request, Result>(
    public val request: Request,
    public val meta: Meta? = null,
) where Request : McpServerBasicRequest<Result> {
    @Serializable
    public data class Meta(
        public val progressToken: McpProgress.Token? = null,
    )

    public companion object
}

public data class McpServerRawNotification(
    public val notification: McpServerBasicNotification,
    public val meta: Meta? = null,
) {
    @Serializable
    public data object Meta
    public companion object
}

public sealed interface McpServerBasicRequest<R> : McpServerCall<R> {
    public val resultSerializer: KSerializer<R>

    public companion object
}

public sealed interface McpServerBasicNotification : McpServerCall<Unit> {
    public companion object
}