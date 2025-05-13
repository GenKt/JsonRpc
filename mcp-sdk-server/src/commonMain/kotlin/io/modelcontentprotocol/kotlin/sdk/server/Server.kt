package io.modelcontentprotocol.kotlin.sdk.server

import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.shared.Protocol
import io.modelcontextprotocol.kotlin.sdk.shared.ProtocolOptions
import io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.JsonObject
import kotlin.collections.contains


/**
 * Configuration options for the MCP server.
 *
 * @property capabilities The capabilities this server supports.
 * @property enforceStrictCapabilities Whether to strictly enforce capabilities when interacting with clients.
 */
public class ServerOptions(
    public val capabilities: ServerCapabilities,
    enforceStrictCapabilities: Boolean = true,
) : ProtocolOptions(enforceStrictCapabilities = enforceStrictCapabilities)

/**
 * An MCP server on top of a pluggable transport.
 *
 * This server automatically responds to the initialization flow as initiated by the client.
 * You can register tools, prompts, and resources using [addTool], [addPrompt], and [addResource].
 * The server will then automatically handle listing and retrieval requests from the client.
 *
 * @param serverInfo Information about this server implementation (name, version).
 * @param options Configuration options for the server.
 */
public open class Server(
    private val serverInfo: Implementation,
    options: ServerOptions,
) : Protocol(options) {
    private var _onInitialized: (() -> Unit) = {}
    private var _onClose: () -> Unit = {}

    /**
     * The client's reported capabilities after initialization.
     */
    public var clientCapabilities: ClientCapabilities? = null
        private set

    /**
     * The client's version information after initialization.
     */
    public var clientVersion: Implementation? = null
        private set

    private val capabilities: ServerCapabilities = options.capabilities

    private val tools = mutableMapOf<String, RegisteredTool>()
    private val prompts = mutableMapOf<String, RegisteredPrompt>()
    private val resources = mutableMapOf<String, RegisteredResource>()

    init {
        // Core protocol handlers
        setRequestHandler<InitializeRequest>(Method.Defined.Initialize) { request, _ ->
            handleInitialize(request)
        }
        setNotificationHandler<InitializedNotification>(Method.Defined.NotificationsInitialized) {
            _onInitialized()
            CompletableDeferred(Unit)
        }

        // Internal handlers for tools
        if (capabilities.tools != null) {
            setRequestHandler<ListToolsRequest>(Method.Defined.ToolsList) { _, _ ->
                handleListTools()
            }
            setRequestHandler<CallToolRequest>(Method.Defined.ToolsCall) { request, _ ->
                handleCallTool(request)
            }
        }

        // Internal handlers for prompts
        if (capabilities.prompts != null) {
            setRequestHandler<ListPromptsRequest>(Method.Defined.PromptsList) { _, _ ->
                handleListPrompts()
            }
            setRequestHandler<GetPromptRequest>(Method.Defined.PromptsGet) { request, _ ->
                handleGetPrompt(request)
            }
        }

        // Internal handlers for resources
        if (capabilities.resources != null) {
            setRequestHandler<ListResourcesRequest>(Method.Defined.ResourcesList) { _, _ ->
                handleListResources()
            }
            setRequestHandler<ReadResourceRequest>(Method.Defined.ResourcesRead) { request, _ ->
                handleReadResource(request)
            }
            setRequestHandler<ListResourceTemplatesRequest>(Method.Defined.ResourcesTemplatesList) { _, _ ->
                handleListResourceTemplates()
            }
        }
    }

    /**
     * Registers a callback to be invoked when the server has completed initialization.
     */
    public fun onInitalized(block: () -> Unit) {
        val old = _onInitialized
        _onInitialized = {
            old()
            block()
        }
    }

    /**
     * Registers a callback to be invoked when the server connection is closing.
     */
    public fun onClose(block: () -> Unit) {
        val old = _onClose
        _onClose = {
            old()
            block()
        }
    }

    /**
     * Called when the server connection is closing.
     */
    override fun onClose() {
        _onClose()
    }

    /**
     * Registers a single tool. This tool can then be called by the client.
     *
     * @param name The name of the tool.
     * @param description A human-readable description of what the tool does.
     * @param inputSchema The expected input schema for the tool.
     * @param handler A suspend function that handles executing the tool when called by the client.
     * @throws IllegalStateException If the server does not support tools.
     */
    public fun addTool(
        name: String,
        description: String,
        inputSchema: Tool.Input = Tool.Input(),
        handler: suspend (CallToolRequest) -> CallToolResult
    ) {
        if (capabilities.tools == null) {
            throw IllegalStateException("Server does not support tools capability. Enable it in ServerOptions.")
        }
        tools[name] = RegisteredTool(Tool(name, description, inputSchema), handler)
    }

    /**
     * Registers multiple tools at once.
     *
     * @param toolsToAdd A list of [RegisteredTool] objects representing the tools to register.
     * @throws IllegalStateException If the server does not support tools.
     */
    public fun addTools(toolsToAdd: List<RegisteredTool>) {
        if (capabilities.tools == null) {
            throw IllegalStateException("Server does not support tools capability.")
        }
        for (rt in toolsToAdd) {
            tools[rt.tool.name] = rt
        }
    }

    /**
     * Registers a single prompt. The prompt can then be retrieved by the client.
     *
     * @param prompt A [sdk.Prompt] object describing the prompt.
     * @param promptProvider A suspend function that returns the prompt content when requested by the client.
     * @throws IllegalStateException If the server does not support prompts.
     */
    public fun addPrompt(prompt: Prompt, promptProvider: suspend (GetPromptRequest) -> GetPromptResult) {
        if (capabilities.prompts == null) {
            throw IllegalStateException("Server does not support prompts capability.")
        }
        prompts[prompt.name] = RegisteredPrompt(prompt, promptProvider)
    }

    /**
     * Registers a single prompt by constructing a [sdk.Prompt] from given parameters.
     *
     * @param name The name of the prompt.
     * @param description An optional human-readable description of the prompt.
     * @param arguments An optional list of [sdk.PromptArgument] that the prompt accepts.
     * @param promptProvider A suspend function that returns the prompt content when requested.
     * @throws IllegalStateException If the server does not support prompts.
     */
    public fun addPrompt(
        name: String,
        description: String? = null,
        arguments: List<PromptArgument>? = null,
        promptProvider: suspend (GetPromptRequest) -> GetPromptResult
    ) {
        val prompt = Prompt(name = name, description = description, arguments = arguments)
        addPrompt(prompt, promptProvider)
    }

    /**
     * Registers multiple prompts at once.
     *
     * @param promptsToAdd A list of [RegisteredPrompt] objects representing the prompts to register.
     * @throws IllegalStateException If the server does not support prompts.
     */
    public fun addPrompts(promptsToAdd: List<RegisteredPrompt>) {
        if (capabilities.prompts == null) {
            throw IllegalStateException("Server does not support prompts capability.")
        }
        for (rp in promptsToAdd) {
            prompts[rp.prompt.name] = rp
        }
    }

    /**
     * Registers a single resource. The resource content can then be read by the client.
     *
     * @param uri The URI of the resource.
     * @param name A human-readable name for the resource.
     * @param description A description of the resource's content.
     * @param mimeType The MIME type of the resource content.
     * @param readHandler A suspend function that returns the resource content when read by the client.
     * @throws IllegalStateException If the server does not support resources.
     */
    public fun addResource(
        uri: String,
        name: String,
        description: String,
        mimeType: String = "text/html",
        readHandler: suspend (ReadResourceRequest) -> ReadResourceResult
    ) {
        if (capabilities.resources == null) {
            throw IllegalStateException("Server does not support resources capability.")
        }
        resources[uri] = RegisteredResource(Resource(uri, name, description, mimeType), readHandler)
    }

    /**
     * Registers multiple resources at once.
     *
     * @param resourcesToAdd A list of [RegisteredResource] objects representing the resources to register.
     * @throws IllegalStateException If the server does not support resources.
     */
    public fun addResources(resourcesToAdd: List<RegisteredResource>) {
        if (capabilities.resources == null) {
            throw IllegalStateException("Server does not support resources capability.")
        }
        for (r in resourcesToAdd) {
            resources[r.resource.uri] = r
        }
    }

    /**
     * Sends a ping request to the client to check connectivity.
     *
     * @return The result of the ping request.
     * @throws IllegalStateException If for some reason the method is not supported or the connection is closed.
     */
    public suspend fun ping(): EmptyRequestResult {
        return request<EmptyRequestResult>(PingRequest())
    }

    /**
     * Creates a message using the server's sampling capability.
     *
     * @param params The parameters for creating a message.
     * @param options Optional request options.
     * @return The created message result.
     * @throws IllegalStateException If the server does not support sampling or if the request fails.
     */
    public suspend fun createMessage(
        params: CreateMessageRequest,
        options: RequestOptions? = null
    ): CreateMessageResult {
        return request<CreateMessageResult>(params, options)
    }

    /**
     * Lists the available "roots" from the client's perspective (if supported).
     *
     * @param params JSON parameters for the request, usually empty.
     * @param options Optional request options.
     * @return The list of roots.
     * @throws IllegalStateException If the server or client does not support roots.
     */
    public suspend fun listRoots(
        params: JsonObject = EmptyJsonObject,
        options: RequestOptions? = null
    ): ListRootsResult {
        return request<ListRootsResult>(ListRootsRequest(params), options)
    }

    /**
     * Sends a logging message notification to the client.
     *
     * @param params The logging message notification parameters.
     */
    public suspend fun sendLoggingMessage(params: LoggingMessageNotification) {
        notification(params)
    }

    /**
     * Sends a resource-updated notification to the client, indicating that a specific resource has changed.
     *
     * @param params Details of the updated resource.
     */
    public suspend fun sendResourceUpdated(params: ResourceUpdatedNotification) {
        notification(params)
    }

    /**
     * Sends a notification to the client indicating that the list of resources has changed.
     */
    public suspend fun sendResourceListChanged() {
        notification(ResourceListChangedNotification())
    }

    /**
     * Sends a notification to the client indicating that the list of tools has changed.
     */
    public suspend fun sendToolListChanged() {
        notification(ToolListChangedNotification())
    }

    /**
     * Sends a notification to the client indicating that the list of prompts has changed.
     */
    public suspend fun sendPromptListChanged() {
        notification(PromptListChangedNotification())
    }

    // --- Internal Handlers ---

    private suspend fun handleInitialize(request: InitializeRequest): InitializeResult {
        clientCapabilities = request.capabilities
        clientVersion = request.clientInfo

        val requestedVersion = request.protocolVersion
        val protocolVersion = if (SUPPORTED_PROTOCOL_VERSIONS.contains(requestedVersion)) {
            requestedVersion
        } else {
            LATEST_PROTOCOL_VERSION
        }

        return InitializeResult(
            protocolVersion = protocolVersion,
            capabilities = capabilities,
            serverInfo = serverInfo
        )
    }

    private suspend fun handleListTools(): ListToolsResult {
        val toolList = tools.values.map { it.tool }
        return ListToolsResult(tools = toolList, nextCursor = null)
    }

    private suspend fun handleCallTool(request: CallToolRequest): CallToolResult {
        val tool = tools[request.name]
            ?: run {
                throw IllegalArgumentException("Tool not found: ${request.name}")
            }
        return tool.handler(request)
    }

    private suspend fun handleListPrompts(): ListPromptsResult {
        return ListPromptsResult(prompts = prompts.values.map { it.prompt })
    }

    private suspend fun handleGetPrompt(request: GetPromptRequest): GetPromptResult {
        val prompt = prompts[request.name]
            ?: run {
                throw IllegalArgumentException("Prompt not found: ${request.name}")
            }
        return prompt.messageProvider(request)
    }

    private suspend fun handleListResources(): ListResourcesResult {
        return ListResourcesResult(resources = resources.values.map { it.resource })
    }

    private suspend fun handleReadResource(request: ReadResourceRequest): ReadResourceResult {
        val resource = resources[request.uri]
            ?: run {
                throw IllegalArgumentException("Resource not found: ${request.uri}")
            }
        return resource.readHandler(request)
    }

    private suspend fun handleListResourceTemplates(): ListResourceTemplatesResult {
        // If you have resource templates, return them here. For now, return empty.
        return ListResourceTemplatesResult(listOf())
    }

    /**
     * Asserts that the client supports the capability required for the given [method].
     *
     * This method is automatically called by the [sdk.shared.Protocol] framework before handling requests.
     * Throws [IllegalStateException] if the capability is not supported.
     *
     * @param method The method for which we are asserting capability.
     */
    override fun assertCapabilityForMethod(method: Method) {
        when (method.value) {
            "sampling/createMessage" -> {
                if (clientCapabilities?.sampling == null) {
                    throw IllegalStateException("Client does not support sampling (required for ${method.value})")
                }
            }

            "roots/list" -> {
                if (clientCapabilities?.roots == null) {
                    throw IllegalStateException("Client does not support listing roots (required for ${method.value})")
                }
            }

            "ping" -> {
                // No specific capability required
            }
        }
    }

    /**
     * Asserts that the server can handle the specified notification method.
     *
     * Throws [IllegalStateException] if the server does not have the capabilities required to handle this notification.
     *
     * @param method The notification method.
     */
    override fun assertNotificationCapability(method: Method) {
        when (method.value) {
            "notifications/message" -> {
                if (capabilities.logging == null) {
                    throw IllegalStateException("Server does not support logging (required for ${method.value})")
                }
            }

            "notifications/resources/updated",
            "notifications/resources/list_changed" -> {
                if (capabilities.resources == null) {
                    throw IllegalStateException("Server does not support notifying about resources (required for ${method.value})")
                }
            }

            "notifications/tools/list_changed" -> {
                if (capabilities.tools == null) {
                    throw IllegalStateException("Server does not support notifying of tool list changes (required for ${method.value})")
                }
            }

            "notifications/prompts/list_changed" -> {
                if (capabilities.prompts == null) {
                    throw IllegalStateException("Server does not support notifying of prompt list changes (required for ${method.value})")
                }
            }

            "notifications/cancelled",
            "notifications/progress" -> {
                // Always allowed
            }
        }
    }

    /**
     * Asserts that the server can handle the specified request method.
     *
     * Throws [IllegalStateException] if the server does not have the capabilities required to handle this request.
     *
     * @param method The request method.
     */
    override fun assertRequestHandlerCapability(method: Method) {
        when (method.value) {
            "sampling/createMessage" -> {
                if (capabilities.sampling == null) {
                    throw IllegalStateException("Server does not support sampling (required for $method)")
                }
            }

            "logging/setLevel" -> {
                if (capabilities.logging == null) {
                    throw IllegalStateException("Server does not support logging (required for $method)")
                }
            }

            "prompts/get",
            "prompts/list" -> {
                if (capabilities.prompts == null) {
                    throw IllegalStateException("Server does not support prompts (required for $method)")
                }
            }

            "resources/list",
            "resources/templates/list",
            "resources/read" -> {
                if (capabilities.resources == null) {
                    throw IllegalStateException("Server does not support resources (required for $method)")
                }
            }

            "tools/call",
            "tools/list" -> {
                if (capabilities.tools == null) {
                    throw IllegalStateException("Server does not support tools (required for $method)")
                }
            }

            "ping", "initialize" -> {
                // No capability required
            }
        }
    }
}

/**
 * A wrapper class representing a registered tool on the server.
 *
 * @property tool The tool definition.
 * @property handler A suspend function to handle the tool call requests.
 */
public data class RegisteredTool(
    val tool: Tool,
    val handler: suspend (CallToolRequest) -> CallToolResult
)

/**
 * A wrapper class representing a registered prompt on the server.
 *
 * @property prompt The prompt definition.
 * @property messageProvider A suspend function that returns the prompt content when requested by the client.
 */
public data class RegisteredPrompt(
    val prompt: Prompt,
    val messageProvider: suspend (GetPromptRequest) -> GetPromptResult
)

/**
 * A wrapper class representing a registered resource on the server.
 *
 * @property resource The resource definition.
 * @property readHandler A suspend function to handle read requests for this resource.
 */
public data class RegisteredResource(
    val resource: Resource,
    val readHandler: suspend (ReadResourceRequest) -> ReadResourceResult
)
