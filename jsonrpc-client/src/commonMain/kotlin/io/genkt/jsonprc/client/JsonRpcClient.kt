package io.genkt.jsonprc.client

import io.genkt.jsonrpc.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlin.coroutines.Continuation
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public fun CoroutineScope.JsonRpcClient(
    transport: JsonRpcClientTransport,
    timeOut: Duration = 10.seconds
): JsonRpcClient = JsonRpcClient(
    transport,
    timeOut,
    this
)

public class JsonRpcClient(
    public val transport: JsonRpcClientTransport,
    public val timeOut: Duration = 10.seconds,
    private val coroutineScope: CoroutineScope = transport.coroutineScope,
) : AutoCloseable {
    private val requestMapMutex = Mutex()
    private val requestMap = HashMap<RequestId, Continuation<JsonRpcSuccessResponse>>()
    public var maxNumberId: RequestId.NumberId = RequestId.NumberId(0)
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
            transport.receiveFlow
                .collect { handleResponse(it) }
        }
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

    override fun close() {
        coroutineScope.cancel()
        transport.close()
    }
}

public suspend fun JsonRpcClient.sendRequest(
    id: RequestId = RequestId.NumberId(maxNumberId.value + 1),
    method: String,
    params: JsonElement? = null,
    jsonrpc: String = JsonRpc.VERSION,
): JsonRpcSuccessResponse {
    if (id == RequestId.NumberId(maxNumberId.value + 1)) {
        maxNumberId = RequestId.NumberId(maxNumberId.value + 1)
    }
    return send(
        JsonRpcRequest(
            id = id,
            method = method,
            params = params ?: JsonNull,
            jsonrpc = jsonrpc,
        )
    )
}

public suspend fun JsonRpcClient.sendNotification(
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