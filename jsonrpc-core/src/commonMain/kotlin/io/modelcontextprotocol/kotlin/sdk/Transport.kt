package io.modelcontextprotocol.kotlin.sdk

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.serialization.json.JsonObject

public data class JsonTransport(
    public val sendChannel: SendChannel<JsonObject>,
    public val receiveChannel: ReceiveChannel<JsonObject>
)

public data class ByteTransport(
    public val source: Source,
    public val sink: Sink
)

public data class JsonRpcClientTransport(
    public val sendChannel: SendChannel<JsonRpcClientMessage>,
    public val receiveChannel: ReceiveChannel<JsonRpcServerMessage>
)

public data class JsonRpcServerTransport(
    public val sendChannel: SendChannel<JsonRpcServerMessage>,
    public val receiveChannel: ReceiveChannel<JsonRpcClientMessage>
)