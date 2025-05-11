package io.github.genkt.jsonrpc

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement

public interface Transport<Input, Output> : AutoCloseable {
    public val sendChannel: SendChannel<Input>
    public val receiveFlow: Flow<Output>
}

public fun <Input, Output, NewInput, NewOutput> Transport<Input, Output>.map(
    transform: (NewInput) -> Input,
    reverseTransform: (Output) -> NewOutput
): Transport<NewInput, NewOutput> =
    DelegatingTransport(
        sendChannel = sendChannel.mapFrom(transform),
        receiveFlow = receiveFlow.map(reverseTransform),
        delegate = this
    )

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

public fun <T, R> SendChannel<R>.mapFrom(transform: (T) -> R): SendChannel<T> =
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
    map(
        { JsonRpc.json.encodeToJsonElement(JsonRpcClientMessageSerializer, it) },
        { JsonRpc.json.decodeFromJsonElement(JsonRpcServerMessageSerializer, it) }
    )

public fun JsonTransport.asJsonRpcServerTransport(): JsonRpcServerTransport =
    map(
        { JsonRpc.json.encodeToJsonElement(JsonRpcServerMessageSerializer, it) },
        { JsonRpc.json.decodeFromJsonElement(JsonRpcClientMessageSerializer, it) }
    )

public fun JsonTransport.asJsonRpcTransport(): JsonRpcTransport =
    map(
        { JsonRpc.json.encodeToJsonElement(JsonRpcMessageSerializer, it) },
        { JsonRpc.json.decodeFromJsonElement(JsonRpcMessageSerializer, it) }
    )

public fun JsonRpcTransport.asJsonClientTransport(): JsonRpcClientTransport =
    map(
        { it },
        {
            when (it) {
                is JsonRpcClientMessage -> JsonRpcServerMessageBatch(emptyList())
                is JsonRpcServerMessage -> it
            }
        }
    )

public fun JsonRpcTransport.asJsonServerTransport(): JsonRpcServerTransport =
    map(
        { it },
        {
            when (it) {
                is JsonRpcClientMessage -> it
                is JsonRpcServerMessage -> JsonRpcClientMessageBatch(emptyList())
            }
        }
    )

public fun StringTransport.asJsonTransport(parse: (Flow<String>) -> Flow<String>): JsonTransport {
    return TransportImpl(
        sendChannel = sendChannel.mapFrom { JsonRpc.json.encodeToString(JsonElement.serializer(), it) },
        receiveFlow = parse(receiveFlow).map { JsonRpc.json.parseToJsonElement(it) },
        onClose = ::close
    )
}