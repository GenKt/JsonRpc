package io.github.genkt.jsonrpc.transport.stdio

import io.modelcontextprotocol.kotlin.sdk.StringTransport
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach

@Suppress("FunctionName")
@OptIn(DelicateCoroutinesApi::class)
public fun StdioTransport(): StringTransport {
    val input = Channel<String>()
    GlobalScope.launch {
        while (currentCoroutineContext().isActive) {
            val line = readlnOrNull() ?: break
            input.send(line)
        }
    }
    val output = Channel<String>()
    GlobalScope.launch {
        while (currentCoroutineContext().isActive) {
            output.consumeEach { print(it) }
        }
    }
    return StringTransport(
        sendChannel = output,
        receiveChannel = input,
    )
}