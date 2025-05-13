package io.genkt.jsonrpc

import kotlinx.coroutines.flow.Flow

public typealias Interceptor<T> = (T) -> T

@Suppress("FunctionName")
public fun <Input, Output> TransportInterceptor(
    send: Interceptor<Flow<Input>>,
    receive: Interceptor<Flow<Output>>,
): Interceptor<Transport<Input, Output>> = { transport ->
    Transport(
        sendChannel = transport.sendChannel.forwarded(transport.coroutineScope, send),
        receiveFlow = receive(transport.receiveFlow),
        coroutineScope = transport.coroutineScope,
        onClose = transport::close,
    )
}

public fun <Input, Output> Transport<Input, Output>.intercepted(
    interceptor: Interceptor<Transport<Input, Output>>
): Transport<Input, Output> = interceptor(this)