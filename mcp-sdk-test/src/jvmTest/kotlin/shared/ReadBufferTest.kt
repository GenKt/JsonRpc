package shared

import io.modelcontextprotocol.kotlin.sdk.JSONRPCMessage
import io.modelcontextprotocol.kotlin.sdk.JSONRPCNotification
import io.modelcontextprotocol.kotlin.sdk.serializeMessage
import kotlinx.serialization.json.Json
import io.modelcontextprotocol.kotlin.sdk.shared.ReadBuffer
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReadBufferTest {
    private val testMessage: JSONRPCMessage = JSONRPCNotification(method = "foobar")

    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    @Test
    fun `should have no messages after initialization`() {
        val readBuffer = ReadBuffer()
        assertNull(readBuffer.readMessage())
    }

    @Test
    fun `should only yield a message after a newline`() {
        val readBuffer = ReadBuffer()

        // Append message without newline
        val messageBytes = json.encodeToString(testMessage)
            .toByteArray(StandardCharsets.UTF_8)
        readBuffer.append(messageBytes)
        assertNull(readBuffer.readMessage())

        // Append newline and verify message is now available
        readBuffer.append("\n".toByteArray(StandardCharsets.UTF_8))
        assertEquals(testMessage, readBuffer.readMessage())
        assertNull(readBuffer.readMessage())
    }

    @Test
    fun `should be reusable after clearing`() {
        val readBuffer = ReadBuffer()

        readBuffer.append("foobar".toByteArray(Charsets.UTF_8))
        readBuffer.clear()
        assertNull(readBuffer.readMessage())

        val messageJson = serializeMessage(testMessage)
        readBuffer.append(messageJson.toByteArray(Charsets.UTF_8))
        readBuffer.append("\n".toByteArray(Charsets.UTF_8))
        val message = readBuffer.readMessage()
        assertEquals(testMessage, message)
    }
}
