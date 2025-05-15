package io.github.genkt.jsonrpc.test

import io.genkt.jsonrpc.asJsonTransport
import io.github.genkt.jsonrpc.transport.memory.InMemoryTransport
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class InMemoryTransportTest {
    @Test
    fun `test as string transport`() = runTest {
        val (transportA, transportB) = InMemoryTransport<String>()
        testJsonTransport(transportA.asJsonTransport() to transportB.asJsonTransport())
    }

    @Test
    fun `test as json transport`() = runTest {
        println("test as json transport")
        testJsonTransport(InMemoryTransport())
        println("test as json transport completed")
    }
}
