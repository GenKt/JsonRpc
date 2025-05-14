package io.genkt.logging.transport

import io.genkt.logging.Level
import io.genkt.logging.Transport
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

public class FileTransport(
    override val level: Level,
    private val path: String,
) : Transport {
    override fun log(message: String) {
        val file = SystemFileSystem.sink(Path(path), true).buffered()
        file.write((message + "\n").encodeToByteArray())
    }
}