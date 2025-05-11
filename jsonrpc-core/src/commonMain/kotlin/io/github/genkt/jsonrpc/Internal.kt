package io.github.genkt.jsonrpc

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.writeString

internal inline fun <T, R> ReceiveChannel<T>.mapTo(crossinline transform: (T) -> R): ReceiveChannel<R> {
    val newChannel = Channel<R>()
    val receiveJob = CoroutineScope(Dispatchers.Default).launch {
        consumeAsFlow().onCompletion { newChannel.close(it) }
            .collect { newChannel.send(transform(it)) }
    }
    newChannel.invokeOnClose {
        receiveJob.cancel()
        cancel()
    }
    return newChannel
}

internal fun <T> Flow<T>.asChannel(): ReceiveChannel<T> {
    val channel = Channel<T>()
    val flowJob = CoroutineScope(Dispatchers.Default).launch {
        onCompletion { channel.close(it) }
            .collect { channel.send(it) }
    }
    channel.invokeOnClose {
        flowJob.cancel()
    }
    return channel
}

internal inline fun <T, R> SendChannel<R>.mapFrom(crossinline transform: (T) -> R): SendChannel<T> {
    val newChannel = Channel<T>()
    val sendJob = CoroutineScope(Dispatchers.Default).launch {
        newChannel.consumeEach {
            this@mapFrom.send(transform(it))
        }
    }
    newChannel.invokeOnClose {
        sendJob.cancel()
        close(it)
    }
    return newChannel
}

internal fun Source.toStringChannel(): ReceiveChannel<String> {
    val channel = Channel<String>()
    val receiveJob = CoroutineScope(Dispatchers.Default).launch {
        while (currentCoroutineContext().isActive) {
            val line = readlnOrNull() ?: break
            channel.send(line)
        }
        channel.close()
    }
    channel.invokeOnClose {
        receiveJob.cancel()
        close()
    }
    return channel
}

internal fun Sink.toStringChannel(): SendChannel<String> {
    val channel = Channel<String>()
    val sendJob = CoroutineScope(Dispatchers.Default).launch {
        channel.consumeEach { writeString(it) }
    }
    channel.invokeOnClose {
        sendJob.cancel()
        close()
    }
    return channel
}