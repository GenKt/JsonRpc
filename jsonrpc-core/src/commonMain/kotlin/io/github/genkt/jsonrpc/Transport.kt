package io.github.genkt.jsonrpc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.JsonElement

public interface Transport<in Input, out Output> : AutoCloseable {
    public val sendChannel: SendChannel<Input>
    public val receiveFlow: Flow<Output>
}

public fun <Input, Output> Transport(
    sendChannel: SendChannel<Input>,
    receiveFlow: Flow<Output>,
    onClose: () -> Unit = {},
): Transport<Input, Output> =
    TransportImpl(
        sendChannel = sendChannel,
        receiveFlow = receiveFlow,
        onClose = onClose
    )

public fun <Input, Output> Transport<Input, Output>.sharedIn(
    scope: CoroutineScope,
    started: SharingStarted = SharingStarted.Lazily,
    replay: Int = 0,
): Transport<Input, Output> =
    Transport(
        sendChannel = sendChannel,
        receiveFlow = receiveFlow.shareIn(scope, started, replay),
        onClose = this::close
    )

public fun <T, R> SendChannel<R>.delegateInput(transform: (T) -> R): SendChannel<T> =
    DelegatingSendChannel(
        delegate = this,
        transform = transform
    )

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
        onClose = this::close
    )

public fun JsonTransport.asJsonRpcTransport(): JsonRpcTransport =
    Transport(
        sendChannel = sendChannel.delegateInput { JsonRpc.json.encodeToJsonElement(JsonRpcMessageSerializer, it) },
        receiveFlow = receiveFlow.map { JsonRpc.json.decodeFromJsonElement(JsonRpcMessageSerializer, it) },
        onClose = this::close
    )

public fun JsonRpcTransport.asJsonClientTransport(): JsonRpcClientTransport =
    Transport(
        sendChannel = sendChannel,
        receiveFlow = receiveFlow.filterIsInstance(),
        this::close
    )

public fun JsonRpcTransport.asJsonServerTransport(): JsonRpcServerTransport =
    Transport(
        sendChannel = sendChannel,
        receiveFlow = receiveFlow.filterIsInstance(),
        this::close
    )

public fun StringTransport.asJsonTransport(parse: (Flow<String>) -> Flow<String> = { it }): JsonTransport =
    Transport(
        sendChannel = sendChannel.delegateInput { JsonRpc.json.encodeToString(JsonElement.serializer(), it) },
        receiveFlow = parse(receiveFlow).map { JsonRpc.json.parseToJsonElement(it) },
        onClose = this::close
    )

public fun JsonRpcTransport.shareAsClientAndServerIn(
    scope: CoroutineScope,
    started: SharingStarted = SharingStarted.Lazily,
    replay: Int = 0,
): Pair<JsonRpcClientTransport, JsonRpcServerTransport> {
    val shared = sharedIn(scope, started, replay)
    return shared.asJsonClientTransport() to shared.asJsonServerTransport()
}