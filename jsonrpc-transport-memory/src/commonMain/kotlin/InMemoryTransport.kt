package io.github.genkt.jsonrpc.transport.memory

import io.github.genkt.jsonrpc.Transport
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow

@Suppress("FunctionName")
public fun <T> InMemoryTransport(bufferSize: Int = 128): Pair<Transport<T, T>, Transport<T, T>> {
    val channel1To2 = Channel<T>(
        bufferSize,
        BufferOverflow.DROP_OLDEST,
    )
    val channel2To1 = Channel<T>(
        bufferSize,
        BufferOverflow.DROP_OLDEST,
    )

    val onClose: () -> Unit = {
        channel1To2.close()
        channel2To1.close()
    }

    val transport1 = Transport(
        channel1To2,
        channel2To1.consumeAsFlow(),
        onClose
    )

    val transport2 = Transport(
        channel2To1,
        channel1To2.consumeAsFlow(),
        onClose
    )

    return Pair(
        transport1,
        transport2,
    )
}