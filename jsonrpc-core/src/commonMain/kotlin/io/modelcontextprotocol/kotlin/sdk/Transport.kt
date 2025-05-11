package io.modelcontextprotocol.kotlin.sdk

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

public interface JsonRpcTransport {
    public val sendChannel: SendChannel<JsonRpcSendMessage>
    public val receiveChannel: ReceiveChannel<JsonRpcReceiveMessage>
}

public interface JsonRpcClient {
    public suspend fun request(request: JsonRpcRequest): JsonRpcReceiveMessage
    public suspend fun notify(notification: JsonRpcNotification)
}

public interface JsonRpcServer {
    public suspend fun onRequest(request: JsonRpcRequest): JsonRpcReceiveMessage
    public suspend fun onNotify(notification: JsonRpcNotification)
}