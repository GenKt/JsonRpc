package io.genkt.mcp.common.dto

import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

public sealed interface McpClientCall<R> {
    public val method: String
}

public data class McpClientRequest<Result, Request : McpClientBasicRequest<Result>>(
    val basicRequest: Request,
    val meta: Meta? = null,
) : McpClientCall<Result> by basicRequest {
    @Serializable
    public data class Meta(
        val progressChannel: SendChannel<McpProgress>? = null,
    )
}

public data class McpClientNotification<Notification : McpClientBasicNotification>(
    val basicNotification: Notification,
    val meta: Meta? = null,
) : McpClientCall<Unit> by basicNotification {
    public data object Meta
}

public data class McpClientRawRequest<Result, Request : McpClientBasicRequest<Result>>(
    public val request: Request,
    public val meta: Meta? = null,
) {
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

public data class McpServerRequest<Result, Request : McpServerBasicRequest<Result>>(
    val basicRequest: Request,
    val meta: Meta? = null,
) : McpServerCall<Result> by basicRequest {
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

public data class McpServerRawRequest<Result, Request : McpServerBasicRequest<Result>>(
    public val request: Request,
    public val meta: Meta? = null,
) {
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