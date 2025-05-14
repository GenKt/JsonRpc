package io.github.genkt.jsonrpc.transport.stdio

import io.genkt.jsonrpc.SendAction
import io.genkt.jsonrpc.StringTransport
import io.genkt.jsonrpc.Transport
import io.genkt.jsonrpc.commit
import io.genkt.jsonrpc.resume
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.consumeAsFlow

@Suppress("FunctionName")
@OptIn(DelicateCoroutinesApi::class)
public fun CoroutineScope.StdioTransport(): StringTransport {
    val input = Channel<Result<String>>()
    val inputJob = launch {
        while (currentCoroutineContext().isActive) {
            val line = readlnOrNull() ?: break
            input.send(Result.success(line))
        }
    }
    val output = Channel<SendAction<String>>()
    val outputJob = launch {
        while (currentCoroutineContext().isActive) {
            output.consumeEach {
                it.resume(
                    runCatching {
                        print(it.value)
                    }
                )
            }
        }
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
    )
}