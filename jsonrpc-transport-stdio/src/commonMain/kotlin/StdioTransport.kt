package io.github.genkt.jsonrpc.transport.stdio

import io.genkt.jsonrpc.SendAction
import io.genkt.jsonrpc.StringTransport
import io.genkt.jsonrpc.Transport
import io.genkt.jsonrpc.completeCatchingSuspend
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.consumeAsFlow

@Suppress("FunctionName")
@OptIn(DelicateCoroutinesApi::class)
public fun CoroutineScope.StdioTransport(): StringTransport {
    val input = Channel<Result<String>>()
    val inputJob = launch(start = CoroutineStart.LAZY) {
        while (currentCoroutineContext().isActive) {
            val line = readlnOrNull() ?: break
            input.send(Result.success(line))
        }
    }
    val output = Channel<SendAction<String>>()
    val outputJob = launch(start = CoroutineStart.LAZY) {
        while (currentCoroutineContext().isActive) {
            output.consumeEach { sendAction ->
                sendAction.completeCatchingSuspend { print(it) }
            }
        }
    }
    val onStart: suspend () -> Unit = suspend {
        inputJob.start()
        outputJob.start()
    }
    return Transport(
        sendChannel = output,
        receiveFlow = input.consumeAsFlow(),
        coroutineScope = this,
        onClose = {
            input.cancel()
            inputJob.cancel()
            output.cancel()
            outputJob.cancel()
        },
        onStart = onStart
    )
}