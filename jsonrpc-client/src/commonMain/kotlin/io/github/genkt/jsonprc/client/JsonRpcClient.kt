package io.github.genkt.jsonprc.client

import io.github.genkt.jsonrpc.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public class JsonRpcClient(
    public val transport: JsonRpcClientTransport,
    public val timeOut: Duration = 10.seconds,
    public val coroutineContext: CoroutineContext = Dispatchers.Default,
) : AutoCloseable {
    private val requestMapMutex = Mutex()
    private val requestMap = HashMap<RequestId, Continuation<JsonRpcSuccessResponse>>()
    private suspend fun handleResponse(response: JsonRpcServerMessage) {
        when (response) {
            is JsonRpcSuccessResponse -> {
                requestMapMutex.withLock { requestMap.remove(response.id) }
                    ?.resume(response)
                    ?: coroutineContext.handleException(JsonRpcRequestIdNotFoundException(response.id))
            }

            is JsonRpcFailResponse -> {
                requestMapMutex.withLock { requestMap.remove(response.id) }
                    ?.resumeWithException(JsonRpcResponseException(response.error))
                    ?: coroutineContext.handleException(JsonRpcRequestIdNotFoundException(response.id))
            }

            is JsonRpcServerMessageBatch -> {
                response.messages.forEach { handleResponse(it) }
            }
        }
    }

    private val receiveJob = CoroutineScope(coroutineContext).launch {
        transport.receiveFlow
            .catch { coroutineContext.handleException(it) }
            .collect { handleResponse(it) }
    }

    public suspend fun send(request: JsonRpcRequest): JsonRpcSuccessResponse {
        return withTimeoutOrNull(timeOut) {
            suspendCancellableCoroutine { completion ->
                launch {
                    requestMapMutex.withLock { requestMap[request.id] = completion }
                    transport.sendChannel.send(request)
                }
            }
        } ?: throw JsonRpcTimeoutException(request, timeOut)
    }

    public suspend fun send(notification: JsonRpcNotification) {
        transport.sendChannel.send(notification)
    }

    public suspend fun sendRequest(
        id: RequestId,
        method: String,
        params: JsonElement? = null,
        jsonrpc: String = JsonRpc.VERSION,
    ): JsonRpcSuccessResponse {
        return send(
            JsonRpcRequest(
                id = id,
                method = method,
                params = params ?: JsonNull,
                jsonrpc = jsonrpc,
            )
        )
    }

    public suspend fun sendNotification(
        method: String,
        params: JsonElement? = null,
        jsonrpc: String = JsonRpc.VERSION,
    ) {
        send(
            JsonRpcNotification(
                method = method,
                params = params ?: JsonNull,
                jsonrpc = jsonrpc,
            )
        )
    }

    override fun close() {
        receiveJob.cancel()
        transport.close()
    }
}

private fun CoroutineContext.handleException(
    exception: Throwable,
) {
    this[CoroutineExceptionHandler]?.handleException(this, exception)
}