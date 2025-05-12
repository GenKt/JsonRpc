package io.github.genkt.jsonrpc

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement

public interface Transport<Input, Output> : AutoCloseable {
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
    DelegatingTransport(
        sendChannel = sendChannel.mapFrom({ it: JsonRpcClientMessage -> JsonRpc.json.encodeToJsonElement(JsonRpcClientMessageSerializer, it) }),
        receiveFlow = receiveFlow.map<JsonElement, JsonRpcServerMessage>({ it: JsonElement -> JsonRpc.json.decodeFromJsonElement(JsonRpcServerMessageSerializer, it) }
        ),
        delegate = this
    )

public fun JsonTransport.asJsonRpcServerTransport(): JsonRpcServerTransport =
    DelegatingTransport(
        sendChannel = sendChannel.mapFrom({ it: JsonRpcServerMessage -> JsonRpc.json.encodeToJsonElement(JsonRpcServerMessageSerializer, it) }),
        receiveFlow = receiveFlow.map<JsonElement, JsonRpcClientMessage>({ it: JsonElement -> JsonRpc.json.decodeFromJsonElement(JsonRpcClientMessageSerializer, it) }
        ),
        delegate = this
    )

public fun JsonTransport.asJsonRpcTransport(): JsonRpcTransport =
    DelegatingTransport(
        sendChannel = sendChannel.mapFrom({ it: JsonRpcMessage -> JsonRpc.json.encodeToJsonElement(JsonRpcMessageSerializer, it) }),
        receiveFlow = receiveFlow.map<JsonElement, JsonRpcMessage>({ it: JsonElement -> JsonRpc.json.decodeFromJsonElement(JsonRpcMessageSerializer, it) }
        ),
        delegate = this
    )

public fun JsonRpcTransport.asJsonClientTransport(): JsonRpcClientTransport =
    DelegatingTransport(
        sendChannel = sendChannel.mapFrom<JsonRpcClientMessage, JsonRpcMessage>({ it: JsonRpcClientMessage -> it }),
        receiveFlow = receiveFlow.map<JsonRpcMessage, JsonRpcServerMessage>({ it: JsonRpcMessage ->
                                                                                when (it) {
                                                                                    is JsonRpcClientMessage -> JsonRpcServerMessageBatch(emptyList())
                                                                                    is JsonRpcServerMessage -> it
                                                                                }
                                                                            }
        ),
        delegate = this
    )

public fun JsonRpcTransport.asJsonServerTransport(): JsonRpcServerTransport =
    DelegatingTransport(
        sendChannel = sendChannel.mapFrom<JsonRpcServerMessage, JsonRpcMessage>({ it: JsonRpcServerMessage -> it }),
        receiveFlow = receiveFlow.map<JsonRpcMessage, JsonRpcClientMessage>({ it: JsonRpcMessage ->
                                                                                when (it) {
                                                                                    is JsonRpcClientMessage -> it
                                                                                    is JsonRpcServerMessage -> JsonRpcClientMessageBatch(emptyList())
                                                                                }
                                                                            }
        ),
        delegate = this
    )

public fun StringTransport.asJsonTransport(parse: (Flow<String>) -> Flow<String> = { it }): JsonTransport {
    return TransportImpl(
        sendChannel = sendChannel.mapFrom { JsonRpc.json.encodeToString(JsonElement.serializer(), it) },
        receiveFlow = parse(receiveFlow).map { JsonRpc.json.parseToJsonElement(it) },
        onClose = ::close
    )
}