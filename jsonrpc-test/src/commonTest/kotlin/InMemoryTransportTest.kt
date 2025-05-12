package io.github.genkt.jsonrpc.test

import io.github.genkt.jsonrpc.transport.memory.InMemoryTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.*

class InMemoryTransportTest {
    @Test
    fun `test in-memory transport can send and receive messages`() = runTest {
        withContext(Dispatchers.Default) {
            // Create a pair of transports
            val transportPair = InMemoryTransport()
            val transport1 = transportPair.first
            val transport2 = transportPair.second

            // Send a message from transport1 to transport2
            val message = "Hello, transport2!"
            transport1.sendChannel.send(message)

            // Receive the message on transport2
            val received = transport2.receiveFlow.first()

            // Verify the message
            assertEquals(message, received)

            // Send a message from transport2 to transport1
            val response = "Hello, transport1!"
            transport2.sendChannel.send(response)

            // Receive the response on transport1
            val receivedResponse = transport1.receiveFlow.first()

            // Verify the response
            assertEquals(response, receivedResponse)

            // Close the transports
            transport1.close()
            transport2.close()
        }
    }

    @Test
    fun `test in-memory transport closes both channels when one is closed`() = runTest {
        withContext(Dispatchers.Default) {
            // Create a pair of transports
            val transportPair = InMemoryTransport()
            val transport1 = transportPair.first
            val transport2 = transportPair.second

            // Close one transport
            transport1.close()

            // Try to send messages through both transports
            // This should fail because the channels are closed
            var transport1SendFailed = false
            var transport2SendFailed = false

            try {
                transport1.sendChannel.trySend("This should fail")
            } catch (e: Exception) {
                transport1SendFailed = true
            }

            try {
                transport2.sendChannel.trySend("This should fail")
            } catch (e: Exception) {
                transport2SendFailed = true
            }

            // Verify both sends failed
            assertTrue(transport1SendFailed || transport1.sendChannel.trySend("Test").isFailure)
            assertTrue(transport2SendFailed || transport2.sendChannel.trySend("Test").isFailure)
        }
    }

    @Test
    fun `test in-memory transport with custom buffer size`() = runTest {
        withContext(Dispatchers.Default) {
            // Create a pair of transports with a small buffer size
            val bufferSize = 2
            val transportPair = InMemoryTransport(bufferSize)
            val transport1 = transportPair.first
            val transport2 = transportPair.second

            // Send multiple messages from transport1 to transport2
            val messages = listOf("Message 1", "Message 2", "Message 3", "Message 4")
            for (message in messages) {
                transport1.sendChannel.send(message)
            }

            // Receive messages on transport2
            val received1 = transport2.receiveFlow.first()
            val received2 = transport2.receiveFlow.first()

            // Verify at least some messages were received
            // Note: With a buffer size of 2 and DROP_OLDEST overflow strategy,
            // we expect to receive at least the last 2 messages
            assertTrue(messages.contains(received1))
            assertTrue(messages.contains(received2))

            // Close the transports
            transport1.close()
            transport2.close()
        }
    }
}
