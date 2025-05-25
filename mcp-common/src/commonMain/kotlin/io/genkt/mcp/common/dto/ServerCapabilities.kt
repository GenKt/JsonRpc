package io.genkt.mcp.common.dto

import io.genkt.mcp.common.McpMethods
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
public data class McpPrompt(
    public val name: String,
    public val description: String? = null,
    public val arguments: List<Argument> = emptyList()
) {

    @Serializable
    public data class ListRequest(
        public override val cursor: McpPaginated.Cursor? = null
    ): McpClientRequest<ListResult>, McpPaginated.Request {
        override val method: String get() = McpMethods.Prompts.List
        override val resultSerializer: KSerializer<ListResult>
            get() = ListResult.serializer()
    }

    @Serializable
    public data class ListResult(
        public val prompts: List<McpPrompt>,
        public override val nextCursor: McpPaginated.Cursor? = null,
    ): McpPaginated.Result

    @Serializable
    public data class GetRequest(
        public val name: String,
        public val arguments: Map<String, String>
    ): McpClientRequest<GetResult> {
        override val method: String get() = McpMethods.Prompts.Get
        override val resultSerializer: KSerializer<GetResult>
            get() = GetResult.serializer()
    }

    @Serializable
    public data object ListChangedNotification: McpServerNotification {
        override val method: String get() = McpMethods.Notifications.Prompts.ListChanged
    }

    @Serializable
    public data class GetResult(
        public val description: String? = null,
        public val messages: List<Message>,
    )

    @Serializable
    public data class Argument(
        public val name: String,
        public val description: String? = null,
        public val required: Boolean = false
    )

    @Serializable
    public data class Message(
        public val role: String,
        public val content: McpContent.Prompt,
    )
}

@Serializable
public data class McpResource(
    public val uri: String,
    public val name: String,
    public val description: String? = null,
    public val mimeType: String? = null,
    public val size: Long? = null,
) {
    @Serializable
    public data class ListRequest(
        public override val cursor: McpPaginated.Cursor? = null
    ): McpClientRequest<ListResult>, McpPaginated.Request {
        override val method: String get() = McpMethods.Resources.List
        override val resultSerializer: KSerializer<ListResult>
            get() = ListResult.serializer()
    }

    @Serializable
    public data class ListResult(
        public val resources: List<McpResource>,
        public override val nextCursor: McpPaginated.Cursor? = null,
    ): McpPaginated.Result

    @Serializable
    public data class ListTemplateRequest(
        public override val cursor: McpPaginated.Cursor? = null
    ): McpClientRequest<ListTemplateResult>, McpPaginated.Request {
        override val method: String get() = McpMethods.Resources.Templates.List
        override val resultSerializer: KSerializer<ListTemplateResult>
            get() = ListTemplateResult.serializer()
    }

    @Serializable
    public data class ListTemplateResult(
        public val resourceTemplates: List<Template>,
    )

    @Serializable
    public data class ReadRequest(
        public val uri: String,
    ): McpClientRequest<ReadResult> {
        override val method: String get() = McpMethods.Resources.Read
        override val resultSerializer: KSerializer<ReadResult>
            get() = ReadResult.serializer()
    }

    @Serializable
    public data class ReadResult(
        public val contents: List<McpContent.Resource>,
    )

    @Serializable
    public data object ListChangedNotification: McpServerNotification {
        override val method: String get() = McpMethods.Notifications.Resources.ListChanged
    }

    @Serializable
    public data class Template(
        public val uriTemplate: String,
        public val name: String,
        public val description: String? = null,
        public val mimeType: String? = null,
        public val annotations: McpContent.Annotations? = null,
    )

    @Serializable
    public data class SubscribeRequest(
        public val uri: String,
    ): McpClientRequest<SubscribeResult> {
        override val method: String get() = McpMethods.Resources.Subscribe
        override val resultSerializer: KSerializer<SubscribeResult>
            get() = SubscribeResult.serializer()
    }

    @Serializable
    public data object SubscribeResult

    @Serializable
    public data class UnsubscribeRequest(
        public val uri: String,
    ): McpClientRequest<UnsubscribeResult> {
        override val method: String get() = McpMethods.Resources.Unsubscribe
        override val resultSerializer: KSerializer<UnsubscribeResult>
            get() = UnsubscribeResult.serializer()
    }

    @Serializable
    public data object UnsubscribeResult

    @Serializable
    public data class UpdatedNotification(
        public val uri: String,
    ): McpServerNotification {
        override val method: String get() = McpMethods.Notifications.Resources.Updated
    }
}

@Serializable
public data class McpTool(
    public val name: String,
    public val description: String,
    public val inputSchema: InputSchema,
    public val annotations: Annotations? = null,
) {
    @Serializable
    public data class InputSchema(
        public val type: String = "object",
        public val properties: JsonObject? = null,
        public val required: List<String>? = null
    )

    @Serializable
    public data class ListRequest(
        public override val cursor: McpPaginated.Cursor? = null
    ): McpClientRequest<ListResult>, McpPaginated.Request {
        override val method: String get() = McpMethods.Tools.List
        override val resultSerializer: KSerializer<ListResult>
            get() = ListResult.serializer()
    }

    @Serializable
    public data class ListResult(
        public val tools: List<McpTool>,
        public override val nextCursor: McpPaginated.Cursor? = null,
    ): McpPaginated.Result

    @Serializable
    public data class CallRequest(
        public val name: String,
        public val arguments: JsonObject
    ): McpClientRequest<CallResult> {
        override val method: String get() = McpMethods.Tools.Call
        override val resultSerializer: KSerializer<CallResult>
            get() = CallResult.serializer()
    }

    @Serializable
    public data class CallResult(
        public val content: List<McpContent.Prompt>,
        public val isError: Boolean = false,
    )

    @Serializable
    public data object ListChangedNotification: McpServerNotification {
        override val method: String get() = McpMethods.Notifications.Tools.ListChanged
    }

    @Serializable
    public data class Annotations(
        public val title: String? = null,
        public val readOnlyHint: Boolean? = null,
        public val destructiveHint: Boolean? = null,
        public val idempotentHint: Boolean? = null,
        public val openWorldHint: Boolean? = null,
    )
}