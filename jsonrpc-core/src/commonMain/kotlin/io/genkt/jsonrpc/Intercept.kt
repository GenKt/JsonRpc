package io.genkt.jsonrpc

import kotlinx.coroutines.flow.Flow

public typealias Interceptor<T> = (T) -> T

public class TransportInterceptor<Input, Output>(
    send: Interceptor<Flow<Input>>,
    receive: Interceptor<Flow<Output>>,
) : Interceptor<Transport<Input, Output>> by { transport ->
    Transport(
        sendChannel = transport.sendChannel.forwarded(transport.coroutineScope, send),
        receiveFlow = receive(transport.receiveFlow),
        coroutineScope = transport.coroutineScope,
        onClose = transport::close,
    )
} {
    public class Builder<Input, Output> {
        public var sendInterceptor: Interceptor<Flow<Input>> = { it }
        public var receiveInterceptor: Interceptor<Flow<Output>> = { it }
    }
}

public fun <Input, Output> TransportInterceptor.Builder<Input, Output>.build(): Interceptor<Transport<Input, Output>> =
    TransportInterceptor(
        send = sendInterceptor,
        receive = receiveInterceptor,
    )

public fun <Input, Output> TransportInterceptor.Builder<Input, Output>.interceptSend(
    send: Interceptor<Flow<Input>>
) {
    sendInterceptor = send
}

public fun <Input, Output> TransportInterceptor.Builder<Input, Output>.interceptReceive(
    receive: Interceptor<Flow<Output>>
) {
    receiveInterceptor = receive
}

public fun <Input, Output> Transport<Input, Output>.intercepted(
    interceptor: Interceptor<Transport<Input, Output>>
): Transport<Input, Output> = interceptor(this)

public fun <Input, Output> Transport<Input, Output>.intercepted(
    buildAction: TransportInterceptor.Builder<Input, Output>.() -> Unit
): Transport<Input, Output> =
    intercepted(TransportInterceptor.Builder<Input, Output>().apply(buildAction).build())