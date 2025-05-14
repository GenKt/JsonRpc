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
        public var sendChannelInterceptor: Interceptor<Flow<SendAction<Input>>> = { it }
        public var receiveFlowInterceptor: Interceptor<Flow<Result<Output>>> = { it }
    }

    public companion object {
        public operator fun <Input, Output> invoke(buildAction: Builder<Input, Output>.() -> Unit): TransportInterceptor<Input, Output> =
            Builder<Input, Output>().apply(buildAction).build()
    }
}

public fun <Input, Output> TransportInterceptor.Builder<Input, Output>.build(): TransportInterceptor<Input, Output> =
    TransportInterceptor(
        send = sendChannelInterceptor,
        receive = receiveFlowInterceptor,
    )

public fun <Input, Output> Transport<Input, Output>.intercepted(
    interceptor: Interceptor<Transport<Input, Output>>
): Transport<Input, Output> = interceptor(this)

public fun <Input, Output> Transport<Input, Output>.intercepted(
    buildAction: TransportInterceptor.Builder<Input, Output>.() -> Unit
): Transport<Input, Output> =
    intercepted(TransportInterceptor(buildAction))