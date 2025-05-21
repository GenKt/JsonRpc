package io.genkt.jsonrpc

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.JsonElement
import kotlin.coroutines.*
import kotlin.jvm.JvmName

/**
 * @property coroutineScope will be closed when the transport is closed.
 */
public interface Transport<in Input, out Output> : AutoCloseable {
    public val sendChannel: SendChannel<SendAction<Input>>
    public val receiveFlow: Flow<Result<Output>>
    public val coroutineScope: CoroutineScope
    public fun start()

    public companion object {
        public val Disabled: Transport<Any?, Nothing> =
            ThrowingException { UnsupportedOperationException("This transport is disabled") }

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

public interface SharedTransport<in Input, out Output> : Transport<Input, Output> {
    public override val receiveFlow: SharedFlow<Result<Output>>
}

public data class SendAction<out T>(
    public val value: T,
    public val completion: Continuation<Unit>,
)

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

public fun SendAction<*>.complete(result: Result<Unit>) {
    completion.resumeWith(result)
}

public inline fun <T> SendAction<T>.completeCatching(
    crossinline consumer: (T) -> Unit,
) {
    complete(runCatching { consumer(value) })
}

public suspend inline fun <T> SendAction<T>.completeCatchingSuspend(
    crossinline consumer: suspend (T) -> Unit,
) {
    complete(runCatching { consumer(value) })
}

public fun SendAction<*>.commit() {
    completion.resume(Unit)
}

public fun SendAction<*>.fail(t: Throwable) {
    completion.resumeWithException(t)
}

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

public fun <T, R> SendChannel<R>.delegateInput(transform: (T) -> R): SendChannel<T> =
    DelegatingSendChannel(
        delegate = this,
        transform = transform
    )

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

public typealias JsonTransport = Transport<JsonElement, JsonElement>
public typealias StringTransport = Transport<String, String>
public typealias JsonRpcTransport = Transport<JsonRpcMessage, JsonRpcMessage>
public typealias JsonRpcClientTransport = Transport<JsonRpcClientMessage, JsonRpcServerMessage>
public typealias JsonRpcServerTransport = Transport<JsonRpcServerMessage, JsonRpcClientMessage>

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

public fun JsonRpcTransport.asJsonClientTransport(): JsonRpcClientTransport =
    Transport(
        sendChannel = sendChannel,
        receiveFlow = receiveFlow.filterIsInstance(),
        coroutineScope = this.coroutineScope,
        this::close,
        onStart = this::start,
    )

public fun JsonRpcTransport.asJsonServerTransport(): JsonRpcServerTransport =
    Transport(
        sendChannel = sendChannel,
        receiveFlow = receiveFlow.filterIsInstance(),
        coroutineScope = this.coroutineScope,
        this::close,
        onStart = this::start,
    )

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