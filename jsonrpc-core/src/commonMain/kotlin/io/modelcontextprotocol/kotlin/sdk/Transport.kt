package io.modelcontextprotocol.kotlin.sdk

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.serialization.json.JsonElement

public data class JsonTransport(
    public val sendChannel: SendChannel<JsonElement>,
    public val receiveChannel: ReceiveChannel<JsonElement>
)

public data class StringTransport(
    public val sendChannel: SendChannel<String>,
    public val receiveChannel: ReceiveChannel<String>
)

public data class ByteTransport(
    public val source: Source,
    public val sink: Sink
)

public data class JsonRpcTransport(
    public val sendChannel: SendChannel<JsonRpcMessage>,
    public val receiveChannel: ReceiveChannel<JsonRpcMessage>
)

public data class JsonRpcClientTransport(
    public val sendChannel: SendChannel<JsonRpcClientMessage>,
    public val receiveChannel: ReceiveChannel<JsonRpcServerMessage>
)

public data class JsonRpcServerTransport(
    public val sendChannel: SendChannel<JsonRpcServerMessage>,
    public val receiveChannel: ReceiveChannel<JsonRpcClientMessage>
)

public fun JsonTransport.asJsonRpcClientTransport(): JsonRpcClientTransport = JsonRpcClientTransport(
    sendChannel = sendChannel.mapFrom {
        JsonRpc.json.encodeToJsonElement(JsonRpcClientMessageSerializer, it)
    },
    receiveChannel = receiveChannel.mapTo {
        JsonRpc.json.decodeFromJsonElement(JsonRpcServerMessageSerializer, it)
    }
)

public fun JsonTransport.asJsonRpcServerTransport(): JsonRpcServerTransport = JsonRpcServerTransport(
    sendChannel = sendChannel.mapFrom {
        JsonRpc.json.encodeToJsonElement(JsonRpcServerMessageSerializer, it)
    },
    receiveChannel = receiveChannel.mapTo {
        JsonRpc.json.decodeFromJsonElement(JsonRpcClientMessageSerializer, it)
    }
)

public fun JsonTransport.asJsonRpcTransport(): JsonRpcTransport = JsonRpcTransport(
    sendChannel = sendChannel.mapFrom {
        JsonRpc.json.encodeToJsonElement(JsonRpcMessageSerializer, it)
    },
    receiveChannel = receiveChannel.mapTo {
        JsonRpc.json.decodeFromJsonElement(JsonRpcMessageSerializer, it)
    }
)

public fun JsonRpcTransport.asJsonClientTransport(): JsonRpcClientTransport = JsonRpcClientTransport(
    sendChannel = sendChannel,
    receiveChannel = receiveChannel.mapTo {
        when (it) {
            is JsonRpcClientMessage -> JsonRpcServerMessageBatch(emptyList())
            is JsonRpcServerMessage -> it
        }
    }
)

public fun JsonRpcTransport.asJsonServerTransport(): JsonRpcServerTransport = JsonRpcServerTransport(
    sendChannel = sendChannel,
    receiveChannel = receiveChannel.mapTo {
        when (it) {
            is JsonRpcClientMessage -> it
            is JsonRpcServerMessage -> JsonRpcClientMessageBatch(emptyList())
        }
    }
)

public fun ByteTransport.asStringTransport(): StringTransport {
    return StringTransport(
        sendChannel = sink.toStringChannel(),
        receiveChannel = source.toStringChannel(),
    )
}

public fun StringTransport.asJsonTransport(parse: (Flow<String>) -> Flow<String>): JsonTransport {
    return JsonTransport(
        sendChannel = sendChannel.mapFrom { JsonRpc.json.encodeToString(JsonElement.serializer(), it) },
        receiveChannel = receiveChannel.consumeAsFlow()
            .let(parse)
            .asChannel()
            .mapTo { JsonRpc.json.decodeFromString(it) },
    )
}

public fun ByteTransport.asJsonTransport(parse: (Flow<String>) -> Flow<String>): JsonTransport =
    asStringTransport().asJsonTransport(parse)