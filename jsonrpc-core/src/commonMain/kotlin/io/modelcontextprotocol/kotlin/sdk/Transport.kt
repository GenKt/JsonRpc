package io.modelcontextprotocol.kotlin.sdk

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

public interface JsonRpcClientTransport {
    public val sendChannel: SendChannel<JsonRpcClientMessage>
    public val receiveChannel: ReceiveChannel<JsonRpcServerMessage>
}

public interface JsonRpcServerTransport {
    public val sendChannel: SendChannel<JsonRpcServerMessage>
    public val receiveChannel: ReceiveChannel<JsonRpcClientMessage>
}