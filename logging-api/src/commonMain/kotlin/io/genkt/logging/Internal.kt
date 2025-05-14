package io.genkt.logging

internal class LoggerImpl(
    override val level: Level,
    override val transports: List<Transport>,
    override val formatter: (Message) -> String,
): Logger {
    override fun log(level: Level, message: String) =
        transports.forEach {
            it.log(level, message)
        }
}