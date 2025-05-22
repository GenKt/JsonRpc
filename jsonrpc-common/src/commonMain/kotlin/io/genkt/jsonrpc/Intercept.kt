package io.genkt.jsonrpc

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

/**
 * A typealias for a function that takes a value of type [T] and returns a value of type [T].
 * Interceptors can be chained together to modify a value in a pipeline.
 */
public typealias Interceptor<T> = (T) -> T

/**
 * Combines two interceptors into a single interceptor.
 * The resulting interceptor applies the `this` interceptor first, then applies [other] interceptor to the result.
 * @param other The interceptor to apply after `this`.
 * @return A new interceptor that applies both interceptors in sequence.
 */
public inline operator fun <T> Interceptor<T>.plus(
    crossinline other: Interceptor<T>,
): Interceptor<T> = { t ->
   other(this(t))
}

/**
 * A marker interface for scopes that can be used to build interceptors.
 */
public interface GenericInterceptorScope

/**
 * Creates an interceptor that applies a timeout to a suspend function.
 * If the timeout exceeds, the function is canceled and throws a [kotlinx.coroutines.TimeoutCancellationException]
 * @param duration The maximum time to wait for the function to complete.
 */
@Suppress("FunctionName", "UnusedReceiverParameter")
public fun <T, R> GenericInterceptorScope.TimeOut(duration: Duration): Interceptor<suspend (T) -> R> =
    { f -> { param -> withTimeout(duration) { f(param) } } }

/**
 * Creates an interceptor that catches exceptions of type [E] thrown by a suspend function and handles them.
 * @param E The type of exception to catch.
 * @param handleException A function that takes an exception of type [E] and returns a value of type [R].
 */
@Suppress("FunctionName")
public inline fun <T, R, reified E : Throwable> GenericInterceptorScope.Catch(
    crossinline handleException: suspend (T, E) -> R
): Interceptor<suspend (T) -> R> =
    { f ->
        { param ->
            try {
                f(param)
            } catch (e: Throwable) {
                if (e !is E) throw e
                handleException(param, e)
            }
        }
    }

/**
 * Creates an interceptor that performs an action before invoking a suspend function.
 * @param action A function that takes the input parameter of type [T].
 */
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

/**
 * Creates an interceptor that performs an action after a suspend function is invoked.
 * The action receives the input parameter and the [Result] of the function invocation.
 * @param action A function that takes the input parameter of type [T] and the [Result] of type [R].
 */
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

/**
 * An interceptor for [Transport] instances.
 * It allows modifying the send and receive flows of a transport.
 * @param Input The type of the input messages for the transport.
 * @param Output The type of the output messages for the transport.
 * @property send The interceptor for the send flow.
 * @property receive The interceptor for the receive flow.
 */
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
    /**
     * A builder for creating [TransportInterceptor] instances.
     * @param Input The type of the input messages for the transport.
     * @param Output The type of the output messages for the transport.
     */
    public class Builder<Input, Output> : GenericInterceptorScope {
        /** The interceptor for the send flow. */
        public var sendChannelInterceptor: Interceptor<Flow<SendAction<Input>>> = { it }
        /** The interceptor for the receive flow. */
        public var receiveFlowInterceptor: Interceptor<Flow<Result<Output>>> = { it }
    }

    public companion object {
        /**
         * Creates a [TransportInterceptor] using the builder pattern.
         * @param buildAction A lambda function to configure the builder.
         */
        public operator fun <Input, Output> invoke(buildAction: Builder<Input, Output>.() -> Unit): TransportInterceptor<Input, Output> =
            Builder<Input, Output>().apply(buildAction).build()
    }
}

/**
 * Builds a [TransportInterceptor] from a [TransportInterceptor.Builder].
 */
public fun <Input, Output> TransportInterceptor.Builder<Input, Output>.build(): TransportInterceptor<Input, Output> =
    TransportInterceptor(
        send = sendChannelInterceptor,
        receive = receiveFlowInterceptor,
    )

/**
 * Applies an interceptor to a [Transport].
 * @param interceptor The interceptor to apply.
 * @return The intercepted [Transport].
 */
public fun <Input, Output> Transport<Input, Output>.interceptWith(
    interceptor: Interceptor<Transport<Input, Output>>
): Transport<Input, Output> = interceptor(this)

/**
 * Applies a [TransportInterceptor] to a [Transport] using the builder pattern.
 * @param buildAction A lambda function to configure the [TransportInterceptor.Builder].
 * @return The intercepted [Transport].
 */
public fun <Input, Output> Transport<Input, Output>.intercept(
    buildAction: TransportInterceptor.Builder<Input, Output>.() -> Unit
): Transport<Input, Output> =
    interceptWith(TransportInterceptor(buildAction))