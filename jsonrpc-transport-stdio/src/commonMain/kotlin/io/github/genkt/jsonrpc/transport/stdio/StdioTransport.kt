package io.github.genkt.jsonrpc.transport.stdio

import io.modelcontextprotocol.kotlin.sdk.ByteTransport
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.io.Buffer
import kotlinx.io.readLine
import kotlinx.io.writeString

@Suppress("FunctionName")
@OptIn(DelicateCoroutinesApi::class)
public fun StdioTransport(): ByteTransport {
    val stdin = Channel<String>()
    GlobalScope.launch {
        while (currentCoroutineContext().isActive) {
            val line = readlnOrNull() ?: break
            stdin.send(line)
        }
    }
    val inputBuffer = Buffer()
    GlobalScope.launch { stdin.consumeEach { inputBuffer.writeString(it) } }
    val outputBuffer = Buffer()
    GlobalScope.launch {
        while (currentCoroutineContext().isActive) {
            val line = outputBuffer.readLine() ?: break
            println(line)
        }
    }
    return ByteTransport(
        source = inputBuffer,
        sink = outputBuffer,
    )
}