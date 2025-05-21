package io.genkt.mcp.common.dto

import io.genkt.mcp.common.McpMethods
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy

public sealed interface McpCall<out R> {
    public val method: String

    public companion object
}

public sealed interface McpProgressRequest<out Result, out Request> : McpCall<Result> {
    public val token: McpProgress.Token
    public val request: Request
}

@Serializable(with = McpServerCallSerializer::class)
public sealed interface McpServerCall<out R> : McpCall<R> {
    public companion object {
        public fun serializer(): SerializationStrategy<McpServerCall<*>> = McpServerCallSerializer
        public fun deserializer(method: String): DeserializationStrategy<McpServerCall<*>>? =
            if (method.contains("notification")) McpServerNotification.deserializer(method)
            else McpServerRequest.deserializer(method)
    }
}

@Serializable(with = McpClientCallSerializer::class)
public sealed interface McpClientCall<out R> : McpCall<R> {
    public companion object {
        public fun serializer(): SerializationStrategy<McpClientCall<*>> = McpClientCallSerializer
        public fun deserializer(method: String): DeserializationStrategy<McpClientCall<*>> =
            if (method.contains("notification")) McpClientNotification.deserializer(method)
            else McpClientRequest.deserializer(method)
    }
}

@Serializable(with = McpClientRequestSerializer::class)
public sealed interface McpClientRequest<out R> : McpClientCall<R> {
    public val resultDeserializer: DeserializationStrategy<R>

    public companion object {
        public fun serializer(): SerializationStrategy<McpClientRequest<*>> = McpClientRequestSerializer
        public fun deserializer(method: String): DeserializationStrategy<McpClientRequest<*>> =
            when (method) {
                McpMethods.Tools.Call -> McpTool.CallRequest.serializer()
                McpMethods.Prompts.Get -> McpPrompt.GetRequest.serializer()
                McpMethods.Initialize -> McpInit.InitializeRequest.serializer()
                McpMethods.Prompts.List -> McpPrompt.ListRequest.serializer()
                McpMethods.Resources.List -> McpResource.ListRequest.serializer()
                McpMethods.Tools.List -> McpTool.ListRequest.serializer()
                McpMethods.Resources.Templates.List -> McpResource.ListTemplateRequest.serializer()
                McpMethods.Ping -> McpUtilities.Ping.serializer()
                McpMethods.Resources.Read -> McpResource.ReadRequest.serializer()
                McpMethods.Completion.Complete -> McpCompletion.Request.serializer()
                McpMethods.Logging.SetLevel -> McpLogging.SetLevelRequest.serializer()
                McpMethods.Resources.Subscribe -> McpResource.SubscribeRequest.serializer()
                McpMethods.Resources.Unsubscribe -> McpResource.UnsubscribeRequest.serializer()
                else -> throw IllegalArgumentException("Unknown McpClientRequest method: $method")
            }
    }
}

@Serializable(with = McpClientNotificationSerializer::class)
public sealed interface McpClientNotification : McpClientCall<Unit> {
    public companion object {
        public fun deserializer(method: String): DeserializationStrategy<McpClientNotification> =
            when (method) {
                McpMethods.Notifications.Cancelled -> McpUtilities.Cancellation.serializer()
                McpMethods.Notifications.Initialized -> McpInit.InitializedNotification.serializer()
                McpMethods.Notifications.Roots.ListChanged -> McpRoot.ListChangedNotification.serializer()
                McpMethods.Notifications.Progress -> McpProgress.Notification.serializer()
                else -> throw IllegalArgumentException("Unknown McpClientNotification method: $method")
            }
    }
}

@Serializable(with = McpServerRequestSerializer::class)
public sealed interface McpServerRequest<out R> : McpServerCall<R> {
    public val resultDeserializer: DeserializationStrategy<R>

    public companion object {
        public fun serializer(): SerializationStrategy<McpServerRequest<*>> = McpServerRequestSerializer
        public fun deserializer(method: String): DeserializationStrategy<McpServerRequest<*>>? =
            when (method) {
                McpMethods.Roots.List -> McpRoot.ListRequest.serializer()
                McpMethods.Sampling.CreateMessage -> McpSampling.CreateMessageRequest.serializer()
                McpMethods.Ping -> McpUtilities.Ping.serializer()
                else -> null
            }
    }
}

@Serializable(with = McpServerNotificationSerializer::class)
public sealed interface McpServerNotification : McpServerCall<Unit> {
    public companion object {
        public fun deserializer(method: String): DeserializationStrategy<McpServerNotification>? =
            when (method) {
                McpMethods.Notifications.Cancelled -> McpUtilities.Cancellation.serializer()
                McpMethods.Notifications.Prompts.ListChanged -> McpPrompt.ListChangedNotification.serializer()
                McpMethods.Notifications.Resources.ListChanged -> McpPrompt.ListChangedNotification.serializer()
                McpMethods.Notifications.Tools.ListChanged -> McpTool.ListChangedNotification.serializer()
                McpMethods.Notifications.Message -> McpLogging.LogMessage.serializer()
                McpMethods.Notifications.Progress -> McpProgress.Notification.serializer()
                McpMethods.Notifications.Resources.Updated -> McpResource.UpdatedNotification.serializer()
                else -> null
            }
    }
}