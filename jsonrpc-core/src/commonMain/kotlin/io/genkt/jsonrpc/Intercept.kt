package io.genkt.jsonrpc

import kotlinx.coroutines.flow.Flow

public typealias Interceptor<T> = (T) -> T

public inline operator fun <T> Interceptor<T>.plus(
    crossinline other: Interceptor<T>,
): Interceptor<T> = { t ->
    this(other(t))
}

public class TransportInterceptor<Input, Output>(
    send: Interceptor<Flow<SendAction<Input>>>,
    receive: Interceptor<Flow<Result<Output>>>,
) : Interceptor<Transport<Input, Output>> by { transport ->
    Transport(
        sendChannel = transport.sendChannel.forwarded(transport.coroutineScope, send),
        receiveFlow = receive(transport.receiveFlow),
        coroutineScope = transport.coroutineScope,
        onClose = transport::close,
    )
} {
    public class Builder<Input, Output> {
        public var sendInterceptor: Interceptor<Flow<SendAction<Input>>> = { it }
        public var receiveInterceptor: Interceptor<Flow<Result<Output>>> = { it }
    }
}

public fun <Input, Output> TransportInterceptor.Builder<Input, Output>.build(): Interceptor<Transport<Input, Output>> =
    TransportInterceptor(
        send = sendInterceptor,
        receive = receiveInterceptor,
    )

public fun <Input, Output> TransportInterceptor.Builder<Input, Output>.interceptSend(
    send: Interceptor<Flow<SendAction<Input>>>
) {
    sendInterceptor = send
}

public fun <Input, Output> TransportInterceptor.Builder<Input, Output>.interceptReceive(
    receive: Interceptor<Flow<Result<Output>>>
) {
    receiveInterceptor = receive
}

public fun <Input, Output> Transport<Input, Output>.interceptedWith(
    interceptor: Interceptor<Transport<Input, Output>>
): Transport<Input, Output> = interceptor(this)

public fun <Input, Output> Transport<Input, Output>.intercepted(
    buildAction: TransportInterceptor.Builder<Input, Output>.() -> Unit
): Transport<Input, Output> =
    interceptedWith(TransportInterceptor.Builder<Input, Output>().apply(buildAction).build())