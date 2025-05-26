package io.genkt.jsonprc.client

import io.genkt.jsonrpc.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

internal class JsonRpcClientImpl(
    override val transport: JsonRpcClientTransport,
    override val uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit,
    callInterceptor: Interceptor<suspend (JsonRpcClientCall<*>) -> Any?>,
    additionalCoroutineContext: CoroutineContext,
) : JsonRpcClient {
    override val coroutineScope: CoroutineScope = transport.coroutineScope.newChild(additionalCoroutineContext)
    val callExecutor = callInterceptor(this::executeImpl)
    val requestMapMutex = Mutex()
    val requestMap = HashMap<RequestId, Continuation<JsonRpcSuccessResponse>>()
    override fun start() {
        transport.start()
        coroutineScope.launch {
            transport.receiveFlow.collect { result ->
                result.mapCatching { handleResponse(it) }.onFailure { uncaughtErrorHandler(it) }
            }
        }
    }

    @Suppress("unchecked_cast")
    override suspend fun <R> execute(call: JsonRpcClientCall<R>): R {
        return callExecutor(call) as R
    }

    override fun close() {
        coroutineScope.cancel()
        transport.close()
    }
}

private suspend fun JsonRpcClientImpl.executeImpl(call: JsonRpcClientCall<*>): Any? {
    return when (call) {
        is JsonRpcRequest -> this.sendRequestImpl(call)
        is JsonRpcNotification -> this.sendNotificationImpl(call)
    }
}

private suspend fun JsonRpcClientImpl.sendRequestImpl(request: JsonRpcRequest): JsonRpcSuccessResponse {
    return suspendCancellableCoroutine { completion ->
        coroutineScope.launch {
            requestMapMutex.withLock { requestMap[request.id] = completion }
            transport.sendChannel.sendOrThrow(request)
        }
    }
}

private suspend fun JsonRpcClientImpl.sendNotificationImpl(notification: JsonRpcNotification) {
    transport.sendChannel.sendOrThrow(notification)
}

private suspend fun JsonRpcClientImpl.handleResponse(response: JsonRpcServerMessage) {
    when (response) {
        is JsonRpcSuccessResponse -> resumeById(response, Result.success(response))
        is JsonRpcFailResponse -> resumeById(response, Result.failure(JsonRpcResponseException(response.error)))
        is JsonRpcServerMessageBatch -> response.messages.forEach { handleResponse(it) }
    }
}

private suspend fun JsonRpcClientImpl.resumeById(
    response: JsonRpcServerSingleMessage,
    result: Result<JsonRpcSuccessResponse>
) {
    requestMapMutex.withLock {
        requestMap.remove(response.id) ?: throw JsonRpcRequestIdNotFoundException(response)
    }.resumeWith(result)
}

private fun CoroutineScope.newChild(coroutineContext: CoroutineContext): CoroutineScope =
    CoroutineScope(this.coroutineContext + coroutineContext + SupervisorJob(this.coroutineContext[Job]))