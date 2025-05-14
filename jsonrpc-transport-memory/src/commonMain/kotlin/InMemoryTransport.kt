package io.github.genkt.jsonrpc.transport.memory

import io.genkt.jsonrpc.SendAction
import io.genkt.jsonrpc.Transport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

@Suppress("FunctionName")
public fun <T> CoroutineScope.InMemoryTransport(
    bufferSize: Int = Channel.UNLIMITED,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    onSendUndeliveredElement: ((SendAction<T>) -> Unit)? = null,
    onReceiveUndeliveredElement: ((Result<T>) -> Unit)? = null,
): Pair<Transport<T, T>, Transport<T, T>> {
    val channelIn1 = Channel(bufferSize, onBufferOverflow, onSendUndeliveredElement)
    val channelOut1 = Channel(bufferSize, onBufferOverflow, onReceiveUndeliveredElement)
    val channelIn2 = Channel(bufferSize, onBufferOverflow, onSendUndeliveredElement)
    val channelOut2 = Channel(bufferSize, onBufferOverflow, onReceiveUndeliveredElement)
    val coroutineScope = this
    val onClose: () -> Unit = {
        channelIn1.close()
        channelOut1.close()
        channelIn2.close()
        channelOut2.close()
    }

    launch {
        channelIn1.consumeAsFlow().collect { channelOut2.send(Result.success(it.value)) }
    }
    launch {
        channelIn2.consumeAsFlow().collect { channelOut1.send(Result.success(it.value)) }
    }

    return Transport(
        channelIn1,
        channelOut1.consumeAsFlow(),
        coroutineScope,
        onClose
    ) to Transport(
        channelIn2,
        channelOut2.consumeAsFlow(),
        coroutineScope,
        onClose
    )
}