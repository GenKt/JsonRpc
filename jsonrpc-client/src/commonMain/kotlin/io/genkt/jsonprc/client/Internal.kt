package io.genkt.jsonprc.client

import io.genkt.jsonrpc.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal class JsonRpcClientImpl(
    override val transport: JsonRpcClientTransport,
    override val errorHandler: suspend CoroutineScope.(Throwable) -> Unit = { },
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
) : JsonRpcClient {
    override val coroutineScope: CoroutineScope = transport.coroutineScope.newChild(coroutineContext)
    private val requestMapMutex = Mutex()
    private val requestMap = HashMap<RequestId, Continuation<JsonRpcSuccessResponse>>()
    private suspend fun handleResponse(response: JsonRpcServerMessage) {
        when (response) {
            is JsonRpcSuccessResponse -> resumeById(response, Result.success(response))
            is JsonRpcFailResponse -> resumeById(response, Result.failure(JsonRpcResponseException(response.error)))
            is JsonRpcServerMessageBatch -> response.messages.forEach { handleResponse(it) }
        }
    }

    private suspend fun resumeById(response: JsonRpcServerSingleMessage, result: Result<JsonRpcSuccessResponse>) {
        val id = when (response) {
            is JsonRpcSuccessResponse -> response.id
            is JsonRpcFailResponse -> response.id
        }
        requestMapMutex.withLock {
            requestMap.remove(id) ?: throw JsonRpcRequestIdNotFoundException(response)
        }.resumeWith(result)
    }

    private val listeningJob = coroutineScope.launch(start = CoroutineStart.LAZY) {
        transport.receiveFlow.collect { result ->
            result.onSuccess { handleResponse(it) }.onFailure { errorHandler(it) }
        }
    }

    override suspend fun start() {
        transport.start()
        listeningJob.start()
    }

    @Suppress("unchecked_cast")
    override suspend fun <R> execute(call: JsonRpcClientCall<R>): R {
        if (!listeningJob.isActive && !listeningJob.isCompleted) {
            start()
        }
        return when (call) {
            is JsonRpcRequest -> sendRequest(call) as R
            is JsonRpcNotification -> sendNotification(call) as R
        }
    }

    private suspend fun sendRequest(request: JsonRpcRequest): JsonRpcSuccessResponse {
        return suspendCancellableCoroutine { completion ->
            coroutineScope.launch {
                requestMapMutex.withLock { requestMap[request.id] = completion }
                transport.sendChannel.sendOrThrow(request)
            }
        }
    }

    private suspend fun sendNotification(notification: JsonRpcNotification) {
        transport.sendChannel.sendOrThrow(notification)
    }

    override fun close() {
        coroutineScope.cancel()
        transport.close()
    }
}

internal class InterceptedJsonRpcClient(
    val source: JsonRpcClient,
    interceptor: JsonRpcClientInterceptor,
) : JsonRpcClient {
    private val delegate = JsonRpcClientImpl(
        transport = interceptor.interceptTransport(source.transport),
        errorHandler = interceptor.interceptErrorHandler(source.errorHandler),
        coroutineContext = interceptor.additionalCoroutineContext,
    )
    override val transport: JsonRpcClientTransport by delegate::transport
    override val coroutineScope: CoroutineScope by delegate::coroutineScope
    override val errorHandler: suspend CoroutineScope.(Throwable) -> Unit by delegate::errorHandler
    private val interceptedExecute = interceptor.interceptCall { delegate.execute(it) }

    override suspend fun start() {
        transport.start()
        delegate.start()
    }

    @Suppress("unchecked_cast")
    override suspend fun <R> execute(call: JsonRpcClientCall<R>): R {
        return interceptedExecute(call) as R
    }

    override fun close() {
        // TODO: check if this ordering is proper
        coroutineScope.cancel()
        source.close()
        transport.close()
    }
}

private fun CoroutineScope.newChild(coroutineContext: CoroutineContext): CoroutineScope =
    CoroutineScope(this.coroutineContext + coroutineContext + Job(this.coroutineContext[Job]))