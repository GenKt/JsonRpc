package io.genkt.logging.transport

import io.genkt.logging.Level
import io.genkt.logging.Transport

public class ConsoleTransport(override val level: Level) : Transport {
    override fun log(message: String) {
        println(message)
    }
}