package client

import io.modelcontentprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.net.Socket

class ClientIntegrationTest {

    fun createTransport(): StdioClientTransport {
        val socket = Socket("localhost", 3000)

        return StdioClientTransport(
            socket.inputStream.asSource().buffered(),
            socket.outputStream.asSink().buffered()
        )
    }

//    @Disabled("This test requires a running server")
//    @Test
//    fun testRequestTools() = runTest {
//        val client = Client(
//            Implementation("test", "1.0"),
//        )
//
//        val transport = createTransport()
//        try {
//            client.connect(transport)
//
//            val response: ListToolsResult? = client.listTools()
//            println(response?.tools)
//
//        } finally {
//            transport.close()
//        }
//    }

}
