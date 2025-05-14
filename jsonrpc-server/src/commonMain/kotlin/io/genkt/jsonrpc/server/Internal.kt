package io.genkt.jsonrpc.server

import io.genkt.jsonrpc.InternalError
import io.genkt.jsonrpc.JsonRpcClientMessage
import io.genkt.jsonrpc.JsonRpcClientMessageBatch
import io.genkt.jsonrpc.JsonRpcFailResponse
import io.genkt.jsonrpc.JsonRpcNotification
import io.genkt.jsonrpc.JsonRpcRequest
import io.genkt.jsonrpc.JsonRpcServerMessage
import io.genkt.jsonrpc.JsonRpcServerTransport
import io.genkt.jsonrpc.sendOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlin.coroutines.cancellation.CancellationException

internal class JsonRpcServerImpl(
    override val transport: JsonRpcServerTransport,
    override val onRequest: suspend (JsonRpcRequest) -> JsonRpcServerMessage,
    override val onNotification: suspend (JsonRpcNotification) -> Unit,
    override val errorHandler: suspend CoroutineScope.(Throwable) -> Unit = {}
) : JsonRpcServer {
    private val coroutineScope: CoroutineScope = transport.coroutineScope
    private val receiveJob = coroutineScope.launch {
        transport.receiveFlow.cancellable()
            .collect { result ->
                result.onSuccess {
                    handleMessageSafe(it)
                }.onFailure {
                    errorHandler(it)
                }
            }
    }

    private fun CoroutineScope.handleMessageSafe(request: JsonRpcClientMessage) {
        launch(receiveJob) {
            when (request) {
                is JsonRpcRequest -> onRequestSafe(request)
                is JsonRpcNotification -> onNotificationSafe(request)
                is JsonRpcClientMessageBatch -> request.messages.forEach { handleMessageSafe(it) }
            }
        }
    }

    private suspend fun CoroutineScope.onNotificationSafe(request: JsonRpcNotification) {
        try {
            onNotification(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            launch { errorHandler(e) }
        }
    }

    private suspend fun CoroutineScope.onRequestSafe(request: JsonRpcRequest) {
        val response = try {
            onRequest(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            launch { errorHandler(e) }
            JsonRpcFailResponse(
                request.id,
                JsonRpcFailResponse.Error(
                    code = JsonRpcFailResponse.Error.Code.InternalError,
                    message = e.message ?: "Internal error",
                    data = JsonPrimitive(e.stackTraceToString())
                )
            )
        }
        launch { transport.sendChannel.sendOrThrow(response) }
    }

    override fun close() {
        coroutineScope.cancel()
        transport.close()
    }
}

internal class InterceptedJsonRpcServer(
    private val delegate: JsonRpcServer,
    interceptor: JsonRpcServerInterceptor,
): JsonRpcServer by delegate {
    override val transport = interceptor.interceptTransport(delegate.transport)
    val requestHandler = interceptor.interceptRequestHandler(delegate.onRequest)
    val notificationHandler = interceptor.interceptNotificationHandler(delegate.onNotification)
    override val errorHandler = interceptor.interceptErrorHandler(delegate.errorHandler)
}