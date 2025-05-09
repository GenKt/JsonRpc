package client


import io.ktor.server.websocket.*
import kotlinx.coroutines.CompletableDeferred

class WebSocketTransportTest : BaseTransportTest() {
//    @Test
//    @Disabled("Test disabled for investigation #17")
//    fun `should start then close cleanly`() = testApplication {
//        install(WebSockets)
//        routing {
//            mcpWebSocket()
//        }
//
//        val client = createClient {
//            install(io.ktor.client.plugins.websocket.WebSockets)
//        }.mcpWebSocketTransport()
//
//        testClientOpenClose(client)
//    }
//
//    @Test
//    @Disabled("Test disabled for investigation #17")
//    fun `should read messages`() = testApplication {
//        val clientFinished = CompletableDeferred<Unit>()
//
//        install(WebSockets)
//        routing {
//            mcpWebSocketTransport {
//                onMessage {
//                    send(it)
//                }
//
//                clientFinished.await()
//            }
//        }
//
//        val client = createClient {
//            install(io.ktor.client.plugins.websocket.WebSockets)
//        }.mcpWebSocketTransport()
//
//        testClientRead(client)
//
//        clientFinished.complete(Unit)
//    }
}
