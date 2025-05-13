package io.github.genkt.jsonrpc.test

import io.genkt.jsonrpc.asJsonTransport
import io.github.genkt.jsonrpc.transport.memory.InMemoryTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test

class InMemoryTransportTest {
    @Test
    fun `test as string transport`() = runTest {
        withContext(Dispatchers.Default) {
            val (transportA, transportB) = InMemoryTransport<String>()
            testJsonTransport(transportA.asJsonTransport() to transportB.asJsonTransport())
        }
    }

    @Test
    fun `test as json transport`() = runTest {
        withContext(Dispatchers.Default) {
            testJsonTransport(InMemoryTransport())
        }
    }
}
