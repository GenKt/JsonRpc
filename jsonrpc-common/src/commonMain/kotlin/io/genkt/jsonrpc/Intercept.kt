package io.genkt.jsonrpc

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

public typealias Interceptor<T> = (T) -> T

public inline operator fun <T> Interceptor<T>.plus(
    crossinline other: Interceptor<T>,
): Interceptor<T> = { t ->
    this(other(t))
}

public interface GenericInterceptorScope

@Suppress("FunctionName", "UnusedReceiverParameter")
public fun <T, R> GenericInterceptorScope.TimeOut(duration: Duration): Interceptor<suspend (T) -> R> =
    { f -> { param -> withTimeout(duration) { f(param) } } }

@Suppress("FunctionName")
public inline fun <T, R, reified E : Throwable> GenericInterceptorScope.Catch(
    crossinline handleException: suspend (E) -> R
): Interceptor<suspend (T) -> R> =
    { f ->
        { param ->
            try {
                f(param)
            } catch (e: Throwable) {
                if (e !is E) throw e
                handleException(e)
            }
        }
    }

@Suppress("FunctionName", "UnusedReceiverParameter")
public inline fun <T, R> GenericInterceptorScope.BeforeInvoke(
    crossinline action: (T) -> Unit
): Interceptor<suspend (T) -> R> =
    { f ->
        { param ->
            action(param)
            f(param)
        }
    }

@Suppress("FunctionName")
public inline fun <T, R> GenericInterceptorScope.OnInvoke(
    crossinline action: (T, Result<R>) -> Unit
): Interceptor<suspend (T) -> R> =
    { f ->
        { param ->
            val result = runCatching { f(param) }
            action(param, result)
            result.getOrThrow()
        }
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
    public class Builder<Input, Output> : GenericInterceptorScope {
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

public fun <Input, Output> Transport<Input, Output>.interceptWith(
    interceptor: Interceptor<Transport<Input, Output>>
): Transport<Input, Output> = interceptor(this)

public fun <Input, Output> Transport<Input, Output>.intercept(
    buildAction: TransportInterceptor.Builder<Input, Output>.() -> Unit
): Transport<Input, Output> =
    interceptWith(TransportInterceptor(buildAction))