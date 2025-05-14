package io.genkt.jsonprc.client

import io.genkt.jsonrpc.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.Continuation

internal class JsonRpcClientImpl(
    override val transport: JsonRpcClientTransport,
    override val errorHandler: suspend CoroutineScope.(Throwable) -> Unit = { },
) : JsonRpcClient {
    private val coroutineScope: CoroutineScope = transport.coroutineScope
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

    init {
        coroutineScope.launch {
            transport.receiveFlow.collect { result ->
                    result.onSuccess { handleResponse(it) }.onFailure { errorHandler(it) }
                }
        }
    }

    override suspend fun send(request: JsonRpcRequest): JsonRpcSuccessResponse {
        return suspendCancellableCoroutine { completion ->
            coroutineScope.launch {
                requestMapMutex.withLock { requestMap[request.id] = completion }
                transport.sendChannel.sendOrThrow(request)
            }
        }
    }

    override suspend fun send(notification: JsonRpcNotification) {
        transport.sendChannel.sendOrThrow(notification)
    }

    override fun close() {
        coroutineScope.cancel()
        transport.close()
    }
}

internal class InterceptedJsonRpcClient(
    private val delegate: JsonRpcClient,
    interceptor: JsonRpcClientInterceptor,
) : JsonRpcClient by delegate {
    override val transport: JsonRpcClientTransport = interceptor.interceptTransport(delegate.transport)
    override val errorHandler: suspend CoroutineScope.(Throwable) -> Unit =
        interceptor.interceptErrorHandler(delegate.errorHandler)
    private val sendRequest = interceptor.interceptRequest(delegate::send)
    private val sendNotification = interceptor.interceptNotification(delegate::send)

    override suspend fun send(request: JsonRpcRequest): JsonRpcSuccessResponse {
        return sendRequest(request)
    }

    override suspend fun send(notification: JsonRpcNotification) {
        sendNotification(notification)
    }
}