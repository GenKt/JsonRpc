package io.genkt.jsonrpc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

/**
 * @property coroutineScope will be closed when the transport is closed.
 */
public interface Transport<in Input, out Output> : AutoCloseable {
    public val sendChannel: SendChannel<Input>
    public val receiveFlow: Flow<Output>
    public val coroutineScope: CoroutineScope
}

public interface SharedTransport<in Input, out Output> : Transport<Input, Output> {
    public override val receiveFlow: SharedFlow<Output>
}

public fun <Input, Output> Transport(
    sendChannel: SendChannel<Input>,
    receiveFlow: Flow<Output>,
    coroutineScope: CoroutineScope,
    onClose: () -> Unit = {},
): Transport<Input, Output> =
    TransportImpl(
        sendChannel = sendChannel,
        receiveFlow = receiveFlow,
        coroutineScope = coroutineScope,
        onClose = onClose
    )

public fun <Input, Output> SharedTransport(
    sendChannel: SendChannel<Input>,
    receiveFlow: SharedFlow<Output>,
    coroutineScope: CoroutineScope,
    onClose: () -> Unit = {},
): SharedTransport<Input, Output> =
    SharedTransportImpl(
        sendChannel = sendChannel,
        receiveFlow = receiveFlow,
        coroutineScope = coroutineScope,
        onClose = onClose
    )

public fun <Input, Output> Transport<Input, Output>.sharedIn(
    started: SharingStarted = SharingStarted.Lazily,
    replay: Int = 0,
): SharedTransport<Input, Output> =
    SharedTransport(
        sendChannel = sendChannel,
        receiveFlow = receiveFlow.shareIn(coroutineScope, started, replay),
        coroutineScope = coroutineScope,
        onClose = this::close
    )

public fun <T, R> SendChannel<R>.delegateInput(transform: (T) -> R): SendChannel<T> =
    DelegatingSendChannel(
        delegate = this,
        transform = transform
    )

public fun <T, R> SendChannel<R>.forwarded(
    coroutineScope: CoroutineScope,
    write: (Flow<T>) -> Flow<R>,
): SendChannel<T> {
    val targetChannel = this
    val inputChannel = Channel<T>()
    val forwardingJob = coroutineScope.launch {
        inputChannel.consumeAsFlow()
            .let(write)
            .collect { targetChannel.send(it) }
    }
    targetChannel.invokeOnClose {
        inputChannel.close(it)
        forwardingJob.cancel()
    }
    return inputChannel
}

public typealias JsonTransport = Transport<JsonElement, JsonElement>
public typealias StringTransport = Transport<String, String>
public typealias JsonRpcTransport = Transport<JsonRpcMessage, JsonRpcMessage>
public typealias JsonRpcClientTransport = Transport<JsonRpcClientMessage, JsonRpcServerMessage>
public typealias JsonRpcServerTransport = Transport<JsonRpcServerMessage, JsonRpcClientMessage>

public fun JsonTransport.asJsonRpcClientTransport(): JsonRpcClientTransport =
    Transport(
        sendChannel = sendChannel.delegateInput {
            JsonRpc.json.encodeToJsonElement(
                JsonRpcClientMessageSerializer,
                it
            )
        },
        receiveFlow = receiveFlow.map { JsonRpc.json.decodeFromJsonElement(JsonRpcServerMessageSerializer, it) },
        coroutineScope = this.coroutineScope,
        onClose = this::close
    )

public fun JsonTransport.asJsonRpcServerTransport(): JsonRpcServerTransport =
    Transport(
        sendChannel = sendChannel.delegateInput {
            JsonRpc.json.encodeToJsonElement(
                JsonRpcServerMessageSerializer,
                it
            )
        },
        receiveFlow = receiveFlow.map { JsonRpc.json.decodeFromJsonElement(JsonRpcClientMessageSerializer, it) },
        coroutineScope = this.coroutineScope,
        onClose = this::close
    )

public fun JsonTransport.asJsonRpcTransport(): JsonRpcTransport =
    Transport(
        sendChannel = sendChannel.delegateInput { JsonRpc.json.encodeToJsonElement(JsonRpcMessageSerializer, it) },
        receiveFlow = receiveFlow.map { JsonRpc.json.decodeFromJsonElement(JsonRpcMessageSerializer, it) },
        coroutineScope = this.coroutineScope,
        onClose = this::close
    )

public fun JsonRpcTransport.asJsonClientTransport(): JsonRpcClientTransport =
    Transport(
        sendChannel = sendChannel,
        receiveFlow = receiveFlow.filterIsInstance(),
        coroutineScope = this.coroutineScope,
        this::close
    )

public fun JsonRpcTransport.asJsonServerTransport(): JsonRpcServerTransport =
    Transport(
        sendChannel = sendChannel,
        receiveFlow = receiveFlow.filterIsInstance(),
        coroutineScope = this.coroutineScope,
        this::close
    )

public fun StringTransport.asJsonTransport(
    parse: Interceptor<Flow<String>> = { it },
    write: Interceptor<Flow<String>> = { it },
): JsonTransport =
    Transport(
        sendChannel = sendChannel.forwarded(coroutineScope, write)
            .delegateInput { JsonRpc.json.encodeToString(JsonElement.serializer(), it) },
        receiveFlow = parse(receiveFlow).map { JsonRpc.json.parseToJsonElement(it) },
        coroutineScope = this.coroutineScope,
        onClose = this::close
    )

public fun JsonRpcTransport.shareAsClientAndServerIn(
    started: SharingStarted = SharingStarted.Lazily,
    replay: Int = 0,
): Pair<JsonRpcClientTransport, JsonRpcServerTransport> {
    val shared = sharedIn(started, replay)
    return shared.asJsonClientTransport() to shared.asJsonServerTransport()
}