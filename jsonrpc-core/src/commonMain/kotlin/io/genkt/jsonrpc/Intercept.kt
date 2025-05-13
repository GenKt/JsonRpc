package io.genkt.jsonrpc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

public typealias Interceptor<T> = (T) -> T

public typealias ScopedInterceptor<T> = suspend CoroutineScope.(T) -> T

@Suppress("FunctionName")
public fun <Input, Output> CoroutineScope.TransportInterceptor(
    send: Interceptor<Flow<Input>>,
    receive: Interceptor<Flow<Output>>,
): Interceptor<Transport<Input, Output>> = { transport ->
    val coroutineScope = this
    Transport(
        sendChannel = transport.sendChannel.forwarded(coroutineScope, send),
        receiveFlow = receive(transport.receiveFlow),
        onClose = transport::close,
    )
}

public fun <Input, Output> Transport<Input, Output>.intercepted(
    interceptor: Interceptor<Transport<Input, Output>>
): Transport<Input, Output> = interceptor(this)