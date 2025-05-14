package io.genkt.jsonrpc

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.*

public data class SendAction<T>(
    public val value: T,
    public val completion: Continuation<Unit>,
)

public fun <T> SendAction<T>.map(
    transform: (T) -> T,
): SendAction<T> = copy(value = transform(value))

public fun SendAction<*>.commit() {
    completion.resume(Unit)
}

public fun SendAction<*>.fail(t: Throwable) {
    completion.resumeWithException(t)
}

public typealias SafeFlow<T> = Flow<SendAction<T>>

public suspend fun <T> SendChannel<SendAction<T>>.safeSend(
    value: T,
) {
    val deferred = CompletableDeferred<Result<Unit>>()
    suspendCancellableCoroutine { continuation ->
        suspend { send(SendAction(value, continuation)) }
            .startCoroutine(Continuation(EmptyCoroutineContext) { deferred.complete(it) })
    }
    deferred.await()
}

public suspend fun main(): Unit = coroutineScope {
    withContext(CoroutineName("Main")) {
        val channel = Channel<SendAction<Int>>()
        launch(CoroutineName("Collect")) {
            channel.consumeEach {
                println("Flow collected: ${it.value} in ${currentCoroutineContext()[CoroutineName]}")
                if (it.value % 2 == 0) {
                    it.fail(IllegalStateException("Even number"))
                } else {
                    it.commit()
                }
            }
        }
        val newSendChannel =
            channel.forwarded(CoroutineScope(CoroutineName("Forward"))) { upStream: Flow<SendAction<Int>> ->
                upStream.map {
                    println("Mapping: ${it.value} to ${it.value + 1} in ${currentCoroutineContext()[CoroutineName]}")
                    it.map { it + 1 }
                }.flowOn(CoroutineName("MapFrom"))
                    .onEach { println("After flowOn with ${it.value} in ${currentCoroutineContext()[CoroutineName]}") }
            }
        withContext(CoroutineName("Emitter")) {
            for (i in 1..10) {
                try {
                    delay(1000)
                    println("Emitting: $i in ${currentCoroutineContext()[CoroutineName]}")
                    newSendChannel.safeSend(i)
                    println("Emitted: $i in ${currentCoroutineContext()[CoroutineName]}")
                } catch (_: Exception) {
                    println("Failed to emit: $i in ${currentCoroutineContext()[CoroutineName]}")
                }
            }
        }
        channel.close()
    }
}
