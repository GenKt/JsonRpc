package io.github.genkt.jsonrpc

import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow

internal data class DelegatingTransport<Input, Output>(
    override val sendChannel: SendChannel<Input>,
    override val receiveFlow: Flow<Output>,
    val delegate: Transport<*, *>,
) : Transport<Input, Output>, AutoCloseable by delegate

internal data class TransportImpl<Input, Output>(
    override val sendChannel: SendChannel<Input>,
    override val receiveFlow: Flow<Output>,
    private val onClose: () -> Unit = {},
) : Transport<Input, Output> {
    override fun close() = onClose()
}

@Suppress("UNCHECKED_CAST")
internal class DelegatingSendChannel<T, Delegate>(
    private val delegate: SendChannel<Delegate>,
    private val transform: (T) -> Delegate,
) : SendChannel<T> by (delegate as SendChannel<T>) {
    override suspend fun send(element: T) = delegate.send(transform(element))
    override fun trySend(element: T): ChannelResult<Unit> = delegate.trySend(transform(element))
}