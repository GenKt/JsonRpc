package io.github.genkt.jsonrpc.test

import io.github.genkt.jsonrpc.transport.stdio.StdioTransport
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.*

class StdioTransportTest {
    private val originalIn = System.`in`
    private val originalOut = System.out

    @AfterTest
    fun tearDown() {
        // Restore original stdin and stdout after each test
        System.setIn(originalIn)
        System.setOut(originalOut)
    }

    @Test
    fun `test stdio transport can be created`() = runBlocking {
        // Create a stdio transport
        val transport = StdioTransport()

        // Verify the transport is not null
        assertNotNull(transport)

        // Verify the send channel is not null
        assertNotNull(transport.sendChannel)

        // Verify the receive flow is not null
        assertNotNull(transport.receiveFlow)

        // Close the transport
        transport.close()
    }

    @Test
    fun `test stdio transport can read from stdin`() = runBlocking {
        // Mock stdin with test data
        val testInput = "Test input data\n"
        val inputStream = ByteArrayInputStream(testInput.toByteArray())
        System.setIn(inputStream)

        // Create a stdio transport
        val transport = StdioTransport()

        // Read from the transport's receive flow
        val receivedData = transport.receiveFlow.first()

        // Verify the received data matches the input
        assertEquals("Test input data", receivedData)

        // Close the transport
        transport.close()
    }

    @Test
    fun `test stdio transport can write to stdout`() = runBlocking {
        // Mock stdout to capture output
        val outputStream = ByteArrayOutputStream()
        val printStream = PrintStream(outputStream)
        System.setOut(printStream)

        // Create a stdio transport
        val transport = StdioTransport()

        // Send data through the transport
        val testOutput = "Test output data"
        transport.sendChannel.send(testOutput)

        // Give some time for the coroutine to process the output
        delay(100)

        // Verify the output was written to stdout
        assertEquals(testOutput, outputStream.toString())

        // Close the transport
        transport.close()
    }

    @Test
    fun `test stdio transport can handle multiple lines`() = runBlocking {
        // Mock stdin with multiple lines of test data
        val testInput = "Line 1\nLine 2\nLine 3\n"
        val inputStream = ByteArrayInputStream(testInput.toByteArray())
        System.setIn(inputStream)

        // Create a stdio transport
        val transport = StdioTransport()

        // Read multiple lines from the transport's receive flow
        val receivedLines = transport.receiveFlow.take(3).toList()

        // Verify the received lines match the input
        assertEquals(listOf("Line 1", "Line 2", "Line 3"), receivedLines)

        // Close the transport
        transport.close()
    }
}
