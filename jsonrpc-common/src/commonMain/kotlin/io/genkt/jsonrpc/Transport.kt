package io.genkt.jsonrpc

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.JsonElement
import kotlin.coroutines.*
import kotlin.jvm.JvmName

/**
 * Represents a bidirectional communication channel for sending and receiving messages.
 * @param Input The type of messages that can be sent.
 * @param Output The type of messages that can be received.
 * @property coroutineScope The [CoroutineScope] associated with this transport. It will be closed when the transport is closed.
 */
public interface Transport<in Input, out Output> : AutoCloseable {
    /** The channel for sending messages. */
    public val sendChannel: SendChannel<SendAction<Input>>
    /** The flow for receiving messages. */
    public val receiveFlow: Flow<Result<Output>>
    /** The [CoroutineScope] associated with this transport. */
    public val coroutineScope: CoroutineScope
    /** Starts the transport, allowing it to send and receive messages. */
    public fun start()

    /** Companion object for [Transport]. */
    public companion object {
        /** A disabled transport that throws [UnsupportedOperationException] for all operations. */
        public val Disabled: Transport<Any?, Nothing> =
            ThrowingException { UnsupportedOperationException("This transport is disabled") }

        /**
         * Creates a transport that throws the specified exception for all operations.
         * @param exception A function that returns the [Throwable] to be thrown.
         * @return A [Transport] instance that throws the specified exception.
         */
        @Suppress("FunctionName")
        public fun ThrowingException(exception: () -> Throwable): Transport<Any?, Nothing> =
            object : Transport<Any?, Nothing> {
                override val sendChannel: SendChannel<SendAction<Any?>>
                    get() = throw exception()
                override val receiveFlow: Flow<Result<Nothing>>
                    get() = throw exception()
                override val coroutineScope: CoroutineScope
                    get() = throw exception()

                override fun start() = throw exception()
                override fun close() = throw exception()
            }
    }
}

/**
 * A [Transport] that uses a [SharedFlow] for receiving messages, allowing multiple collectors.
 * @param Input The type of messages that can be sent.
 * @param Output The type of messages that can be received.
 */
public interface SharedTransport<in Input, out Output> : Transport<Input, Output> {
    /** The shared flow for receiving messages. */
    public override val receiveFlow: SharedFlow<Result<Output>>
}

/**
 * Represents an action to send a value, along with a [Continuation] to signal completion.
 * @param T The type of the value to send.
 * @property value The value to send.
 * @property completion The [Continuation] to resume when the send operation is complete.
 */
public data class SendAction<out T>(
    public val value: T,
    public val completion: Continuation<Unit>,
)

/**
 * Maps the value of a [SendAction] using the given transform function.
 * If the transform function throws an exception, the [SendAction] is failed with that exception.
 * @param transform The function to transform the value.
 * @return A new [SendAction] with the transformed value.
 */
public fun <T, R> SendAction<T>.mapOrThrow(
    transform: (T) -> R,
): SendAction<R> {
    return try {
        SendAction(
            value = transform(value),
            completion = completion,
        )
    } catch (e: Throwable) {
        fail(e)
        throw e
    }
}

/**
 * Completes the [SendAction] with the given [result].
 * @param result The result of the send operation.
 */
public fun SendAction<*>.complete(result: Result<Unit>) {
    completion.resumeWith(result)
}

/**
 * Completes the [SendAction] by running the [consumer] and capturing any exceptions.
 * @param consumer The function to consume the value.
 */
public inline fun <T> SendAction<T>.completeCatching(
    crossinline consumer: (T) -> Unit,
) {
    complete(runCatching { consumer(value) })
}

/**
 * Completes the [SendAction] by running the suspendable [consumer] and capturing any exceptions.
 * @param consumer The suspendable function to consume the value.
 */
public suspend inline fun <T> SendAction<T>.completeCatchingSuspend(
    crossinline consumer: suspend (T) -> Unit,
) {
    complete(runCatching { consumer(value) })
}

/**
 * Completes the [SendAction] successfully.
 */
public fun SendAction<*>.commit() {
    completion.resume(Unit)
}

/**
 * Fails the [SendAction] with the given [Throwable].
 * @param t The throwable that caused the failure.
 */
public fun SendAction<*>.fail(t: Throwable) {
    completion.resumeWithException(t)
}

/**
 * Sends a value to the channel or throws an exception if the send fails.
 * This function suspends until the send operation is completed.
 * @param value The value to send.
 * @throws Throwable if the send operation fails.
 */
public suspend fun <T> SendChannel<SendAction<T>>.sendOrThrow(
    value: T,
) {
    val deferred = CompletableDeferred<Result<Unit>>()
    suspendCancellableCoroutine { continuation ->
        suspend { send(SendAction(value, continuation)) }
            .startCoroutine(Continuation(EmptyCoroutineContext) { deferred.complete(it) })
    }
    deferred.await()
}

/**
 * Creates a [Transport] instance.
 * @param Input The type of messages that can be sent.
 * @param Output The type of messages that can be received.
 * @param sendChannel The channel for sending messages.
 * @param receiveFlow The flow for receiving messages.
 * @param coroutineScope The [CoroutineScope] for the transport.
 * @param onClose A lambda to be called when the transport is closed.
 * @param onStart A lambda to be called when the transport is started.
 * @return A new [Transport] instance.
 */
public fun <Input, Output> Transport(
    sendChannel: SendChannel<SendAction<Input>>,
    receiveFlow: Flow<Result<Output>>,
    coroutineScope: CoroutineScope,
    onClose: () -> Unit = {},
    onStart: () -> Unit = {},
): Transport<Input, Output> =
    TransportImpl(
        sendChannel = sendChannel,
        receiveFlow = receiveFlow,
        coroutineScope = coroutineScope,
        onClose = onClose,
        onStart = onStart,
    )

/**
 * Creates a [SharedTransport] instance.
 * @param Input The type of messages that can be sent.
 * @param Output The type of messages that can be received.
 * @param sendChannel The channel for sending messages.
 * @param receiveFlow The shared flow for receiving messages.
 * @param coroutineScope The [CoroutineScope] for the transport.
 * @param onClose A lambda to be called when the transport is closed.
 * @param onStart A lambda to be called when the transport is started.
 * @return A new [SharedTransport] instance.
 */
public fun <Input, Output> SharedTransport(
    sendChannel: SendChannel<SendAction<Input>>,
    receiveFlow: SharedFlow<Result<Output>>,
    coroutineScope: CoroutineScope,
    onClose: () -> Unit = {},
    onStart: () -> Unit = {},
): SharedTransport<Input, Output> =
    SharedTransportImpl(
        sendChannel = sendChannel,
        receiveFlow = receiveFlow,
        coroutineScope = coroutineScope,
        onClose = onClose,
        onStart = onStart,
    )

/**
 * Converts a [Transport] to a [SharedTransport] by sharing its [receiveFlow].
 * @param coroutineContext Additional [CoroutineContext] elements to combine with the transport's [coroutineScope].
 * @param started The [SharingStarted] strategy for the shared flow.
 * @param replay The number of items to replay to new collectors.
 * @return A new [SharedTransport] instance.
 */
public fun <Input, Output> Transport<Input, Output>.sharedIn(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    started: SharingStarted = SharingStarted.Lazily,
    replay: Int = 0,
): SharedTransport<Input, Output> =
    SharedTransport(
        sendChannel = sendChannel,
        receiveFlow = receiveFlow.shareIn(coroutineScope.newChild(coroutineContext), started, replay),
        coroutineScope = coroutineScope,
        onClose = this::close,
        onStart = this::start,
    )

/**
 * Creates a new [SendChannel] that delegates to this channel, transforming input values using the provided function.
 * @param transform The function to transform input values.
 * @return A new [SendChannel] that transforms input values.
 */
public fun <T, R> SendChannel<R>.delegateInput(transform: (T) -> R): SendChannel<T> =
    DelegatingSendChannel(
        delegate = this,
        transform = transform
    )

/**
 * Creates a new [SendChannel] that delegates to this channel, transforming input [SendAction] values using the provided function.
 * If the transform function throws an exception, the [SendAction] is failed with that exception.
 * @param transform The function to transform input values.
 * @return A new [SendChannel] that transforms input [SendAction] values.
 */
public fun <T, R> SendChannel<SendAction<R>>.delegateInputCatching(
    transform: (T) -> R
): SendChannel<SendAction<T>> =
    DelegatingSendChannel(
        delegate = this,
        transform = { it.mapOrThrow(transform) }
    )

@JvmName("delegateInputCatchingResult")
internal inline fun <T, R> Flow<Result<T>>.mapCatching(
    crossinline transform: suspend (T) -> R,
): Flow<Result<R>> = map { result -> result.mapCatching { transform(it) } }

/**
 * Transforms a [Flow] of [SendAction] by applying the [transform] function to the value of each [SendAction].
 * If the [transform] function throws an exception, the [SendAction] is failed with that exception.
 * Otherwise, a new [SendAction] with the transformed value is emitted.
 * @param T The original type of the value in [SendAction].
 * @param R The transformed type of the value in [SendAction].
 * @param transform The suspend function to transform the value.
 * @return A [Flow] of transformed [SendAction]s.
 */
@JvmName("delegateInputCatchingSendAction")
public fun <T, R> Flow<SendAction<T>>.mapCatching(
    transform: suspend (T) -> R,
): Flow<SendAction<R>> =
    transform { sendAction ->
        runCatching { transform(sendAction.value) }
            .onFailure { sendAction.fail(it) }
            .onSuccess {
                emit(
                    SendAction(
                        value = it,
                        completion = sendAction.completion
                    )
                )
            }
    }

/**
 * Forwards messages from a new [SendChannel] to this [SendChannel], transforming them using the provided [write] function.
 * The [write] function takes a [Flow] of input messages and returns a [Flow] of output messages.
 * @param coroutineScope The [CoroutineScope] to launch the forwarding job in.
 * @param write The function to transform the flow of messages.
 * @return A new [SendChannel] that forwards messages to this channel.
 */
public fun <T, R> SendChannel<R>.forwarded(
    coroutineScope: CoroutineScope,
    write: (Flow<T>) -> Flow<R>,
): SendChannel<T> {
    val targetChannel = this
    val inputChannel = Channel<T>()
    val forwardingJob = coroutineScope.launch {
        inputChannel.consumeAsFlow()
            .let(write)
            .collect { targetChannel.send(it) }
    }
    targetChannel.invokeOnClose {
        inputChannel.close(it)
        forwardingJob.cancel()
    }
    return inputChannel
}

/** A [Transport] for [JsonElement] messages. */
public typealias JsonTransport = Transport<JsonElement, JsonElement>
/** A [Transport] for [String] messages. */
public typealias StringTransport = Transport<String, String>
/** A [Transport] for [JsonRpcMessage] messages. */
public typealias JsonRpcTransport = Transport<JsonRpcMessage, JsonRpcMessage>
/** A [Transport] for sending [JsonRpcClientMessage] and receiving [JsonRpcServerMessage]. */
public typealias JsonRpcClientTransport = Transport<JsonRpcClientMessage, JsonRpcServerMessage>
/** A [Transport] for sending [JsonRpcServerMessage] and receiving [JsonRpcClientMessage]. */
public typealias JsonRpcServerTransport = Transport<JsonRpcServerMessage, JsonRpcClientMessage>

/**
 * Converts a [JsonTransport] to a [JsonRpcClientTransport].
 * Messages sent are encoded as [JsonRpcClientMessage] and received messages are decoded as [JsonRpcServerMessage].
 * @return A [JsonRpcClientTransport] instance.
 */
public fun JsonTransport.asJsonRpcClientTransport(): JsonRpcClientTransport =
    Transport(
        sendChannel = sendChannel.delegateInputCatching {
            JsonRpc.json.encodeToJsonElement(
                JsonRpcClientMessageSerializer,
                it
            )
        },
        receiveFlow = receiveFlow.mapCatching {
            JsonRpc.json.decodeFromJsonElement(JsonRpcServerMessageSerializer, it)
        },
        coroutineScope = this.coroutineScope,
        onClose = this::close,
        onStart = this::start
    )

/**
 * Converts a [JsonTransport] to a [JsonRpcServerTransport].
 * Messages sent are encoded as [JsonRpcServerMessage] and received messages are decoded as [JsonRpcClientMessage].
 * @return A [JsonRpcServerTransport] instance.
 */
public fun JsonTransport.asJsonRpcServerTransport(): JsonRpcServerTransport =
    Transport(
        sendChannel = sendChannel.delegateInputCatching {
            JsonRpc.json.encodeToJsonElement(
                JsonRpcServerMessageSerializer,
                it
            )
        },
        receiveFlow = receiveFlow.mapCatching {
            JsonRpc.json.decodeFromJsonElement(JsonRpcClientMessageSerializer, it)
        },
        coroutineScope = this.coroutineScope,
        onClose = this::close,
        onStart = this::start,
    )

/**
 * Converts a [JsonTransport] to a [JsonRpcTransport].
 * Messages sent and received are encoded/decoded as [JsonRpcMessage].
 * @return A [JsonRpcTransport] instance.
 */
public fun JsonTransport.asJsonRpcTransport(): JsonRpcTransport =
    Transport(
        sendChannel = sendChannel.delegateInputCatching {
            JsonRpc.json.encodeToJsonElement(JsonRpcMessageSerializer, it)
        },
        receiveFlow = receiveFlow.mapCatching {
            JsonRpc.json.decodeFromJsonElement(JsonRpcMessageSerializer, it)
        },
        coroutineScope = this.coroutineScope,
        onClose = this::close,
        onStart = this::start,
    )

/**
 * Converts a [JsonRpcTransport] to a [JsonRpcClientTransport].
 * This is primarily a type-casting operation, ensuring that the receive flow only emits [JsonRpcServerMessage].
 * @return A [JsonRpcClientTransport] instance.
 */
public fun JsonRpcTransport.asJsonClientTransport(): JsonRpcClientTransport =
    Transport(
        sendChannel = sendChannel,
        receiveFlow = receiveFlow.filterIsInstance(),
        coroutineScope = this.coroutineScope,
        this::close,
        onStart = this::start,
    )

/**
 * Converts a [JsonRpcTransport] to a [JsonRpcServerTransport].
 * This is primarily a type-casting operation, ensuring that the receive flow only emits [JsonRpcClientMessage].
 * @return A [JsonRpcServerTransport] instance.
 */
public fun JsonRpcTransport.asJsonServerTransport(): JsonRpcServerTransport =
    Transport(
        sendChannel = sendChannel,
        receiveFlow = receiveFlow.filterIsInstance(),
        coroutineScope = this.coroutineScope,
        this::close,
        onStart = this::start,
    )

/**
 * Converts a [StringTransport] to a [JsonTransport].
 * Sent [JsonElement] messages are encoded to strings, and received strings are parsed as [JsonElement].
 * @param parse An interceptor for the receive flow of strings.
 * @param write An interceptor for the send flow of strings.
 * @return A [JsonTransport] instance.
 */
public fun StringTransport.asJsonTransport(
    parse: Interceptor<Flow<Result<String>>> = { it },
    write: Interceptor<Flow<SendAction<String>>> = { it },
): JsonTransport =
    Transport(
        sendChannel = sendChannel.forwarded(coroutineScope) { upStream: Flow<SendAction<JsonElement>> ->
            upStream.mapCatching { JsonRpc.json.encodeToString(JsonElement.serializer(), it) }
                .let(write)
        },
        receiveFlow = parse(receiveFlow).mapCatching { JsonRpc.json.parseToJsonElement(it) },
        coroutineScope = this.coroutineScope,
        onClose = this::close,
        onStart = this::start,
    )

/**
 * Shares a [JsonRpcTransport] as a pair of [JsonRpcClientTransport] and [JsonRpcServerTransport].
 * This allows a single underlying transport to be used for both client and server communication.
 * @param coroutineContext Additional [CoroutineContext] elements to combine with the transport's [coroutineScope].
 * @param started The [SharingStarted] strategy for the shared flow.
 * @param replay The number of items to replay to new collectors.
 * @return A pair containing the [JsonRpcClientTransport] and [JsonRpcServerTransport].
 */
public fun JsonRpcTransport.shareAsClientAndServerIn(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    started: SharingStarted = SharingStarted.Lazily,
    replay: Int = 0,
): Pair<JsonRpcClientTransport, JsonRpcServerTransport> {
    val shared = sharedIn(coroutineContext, started, replay)
    return shared.asJsonClientTransport() to shared.asJsonServerTransport()
}

private fun CoroutineScope.newChild(coroutineContext: CoroutineContext): CoroutineScope =
    CoroutineScope(this.coroutineContext + coroutineContext + Job(this.coroutineContext[Job]))