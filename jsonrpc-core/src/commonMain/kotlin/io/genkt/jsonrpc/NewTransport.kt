package io.genkt.jsonrpc

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.map
import kotlin.coroutines.*
import kotlin.experimental.ExperimentalTypeInference

public data class EmitItem<T>(
    public val value: T,
    public val completion: Continuation<Unit>,
)

public typealias SafeFlowCollector<T> = FlowCollector<EmitItem<T>>
public typealias SafeChannel<T> = Channel<EmitItem<T>>
public typealias SafeFlow<T> = Flow<EmitItem<T>>

public suspend fun <T> SafeFlowCollector<T>.safeEmit(
    value: T,
) {
    val deferred = CompletableDeferred<Result<Unit>>()
    suspendCancellableCoroutine { continuation ->
        suspend { emit(EmitItem(value, continuation)) }
            .startCoroutine(Continuation(EmptyCoroutineContext) { deferred.complete(it) })
    }
    deferred.await()
}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTypeInference::class)
public fun <T, R> SafeFlowCollector<R>.mapFrom(
    @BuilderInference
    transform: (SafeFlow<T>) -> SafeFlow<R>,
): SafeFlowCollector<T> {
    val deferred = CompletableDeferred<SafeFlowCollector<T>>()
    suspend {
        object : SafeFlow<T> {
            override suspend fun collect(collector: SafeFlowCollector<T>) {
                deferred.complete(collector)
            }
        }.let(transform).collect(this)
    }.startCoroutine(NoopContinuation)
    return deferred.getCompleted().also { println("Completed") }
}

public fun <T> makeFlowWithCollector(
    channel: Channel<EmitItem<T>>
): Pair<SafeFlow<T>, SafeFlowCollector<T>> {
    val collector = SafeFlowCollector {
        channel.send(it)
    }
    val flow = object : SafeFlow<T> {
        override suspend fun collect(collector: SafeFlowCollector<T>) {
            channel.consumeEach { collector.emit(it) }
        }
    }
    return flow to collector
}

internal object NoopContinuation : Continuation<Unit> {
    override val context = EmptyCoroutineContext
    override fun resumeWith(result: Result<Unit>) {
        println("NoopContinuation.resumeWith")
    }
}

public suspend fun main(): Unit = coroutineScope {
    val channel = Channel<EmitItem<Int>>()
    val (flow, collector) = makeFlowWithCollector(channel)
    launch {
        flow.collect {
            println("Flow collected: ${it.value}")
            if (it.value % 2 == 0) {
                it.fail(IllegalStateException("Even number"))
            } else {
                it.commit()
            }
        }
    }
    val newCollector = collector.mapFrom<Int, Int> { it.map { it.copy(value = it.value + 1) } }
    for (i in 1..10) {
        try {
            delay(1000)
            println("Emitting: $i")
            newCollector.safeEmit(i)
            println("Emitted: $i")
        } catch (_: Exception) {
            println("Failed to emit: $i")
        }
    }
    channel.close()
}

public fun EmitItem<*>.commit() {
    completion.resume(Unit)
}

public fun EmitItem<*>.fail(t: Throwable) {
    completion.resumeWithException(t)
}