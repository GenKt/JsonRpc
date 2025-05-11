package io.github.genkt.jsonrpc.transport.stdio

import io.github.genkt.jsonrpc.StringTransport
import io.github.genkt.jsonrpc.Transport
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.consumeAsFlow
import kotlin.coroutines.CoroutineContext

@Suppress("FunctionName")
@OptIn(DelicateCoroutinesApi::class)
public fun StdioTransport(coroutineContext: CoroutineContext = Dispatchers.Default): StringTransport {
    val scope = CoroutineScope(coroutineContext)
    val input = Channel<String>()
    val inputJob = scope.launch {
        while (currentCoroutineContext().isActive) {
            val line = readlnOrNull() ?: break
            input.send(line)
        }
    }
    val output = Channel<String>()
    val outputJob = scope.launch {
        while (currentCoroutineContext().isActive) {
            output.consumeEach { print(it) }
        }
    }
    return Transport(
        sendChannel = output,
        receiveFlow = input.consumeAsFlow(),
        onClose = {
            input.cancel()
            inputJob.cancel()
            output.cancel()
            outputJob.cancel()
        },
    )
}