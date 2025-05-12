package io.github.genkt.jsonrpc.test

import io.github.genkt.jsonrpc.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow

@Suppress("FunctionName")
public fun InMemoryTransport(): Pair<StringTransport, StringTransport> {
    val channel1To2 = Channel<String>()
    val channel2To1 = Channel<String>()

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