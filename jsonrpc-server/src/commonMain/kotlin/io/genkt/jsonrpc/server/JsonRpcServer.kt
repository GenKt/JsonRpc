package io.genkt.jsonrpc.server

import io.genkt.jsonrpc.*
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

public interface JsonRpcServer : AutoCloseable {
    public val transport: JsonRpcServerTransport
    public val uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit
    public val coroutineScope: CoroutineScope
    public fun start()
    public class Builder : GenericInterceptorScope {
        public var transport: JsonRpcServerTransport =
            Transport.ThrowingException { error("Using an uninitialized transport") }
        public var uncaughtErrorHandler: suspend CoroutineScope.(Throwable) -> Unit = {}
        public var additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext
        public var onRequest: suspend (JsonRpcRequest) -> JsonRpcServerSingleMessage = {
            throw NotImplementedError("Server feature not implemented.")
        }
        public var onNotification: suspend (JsonRpcNotification) -> Unit = {
            throw NotImplementedError("Server feature not implemented.")
        }
        public var requestInterceptor: Interceptor<suspend (JsonRpcRequest) -> JsonRpcServerSingleMessage> = { it }
        public var notificationInterceptor: Interceptor<suspend (JsonRpcNotification) -> Unit> = { it }
    }
}