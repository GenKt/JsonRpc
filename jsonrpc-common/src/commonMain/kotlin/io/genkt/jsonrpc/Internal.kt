package io.genkt.jsonrpc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

internal data class TransportImpl<in Input, out Output>(
    override val sendChannel: SendChannel<SendAction<Input>>,
    override val receiveFlow: Flow<Result<Output>>,
    override val coroutineScope: CoroutineScope,
    private val onClose: () -> Unit = {},
    private val onStart: suspend () -> Unit = {},
) : Transport<Input, Output> {
    override fun close() = onClose()
    override suspend fun start() = onStart()
}

internal data class SharedTransportImpl<in Input, out Output>(
    override val sendChannel: SendChannel<SendAction<Input>>,
    override val receiveFlow: SharedFlow<Result<Output>>,
    override val coroutineScope: CoroutineScope,
    private val onClose: () -> Unit = {},
    private val onStart: suspend () -> Unit = {},
) : SharedTransport<Input, Output> {
    override fun close() = onClose()
    override suspend fun start() = onStart()
}

@Suppress("UNCHECKED_CAST")
internal class DelegatingSendChannel<T, Delegate>(
    private val delegate: SendChannel<Delegate>,
    private val transform: (T) -> Delegate,
) : SendChannel<T> by (delegate as SendChannel<T>) {
    override suspend fun send(element: T) = delegate.send(transform(element))
    override fun trySend(element: T): ChannelResult<Unit> = delegate.trySend(transform(element))
}