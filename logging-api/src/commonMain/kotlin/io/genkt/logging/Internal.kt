package io.genkt.logging

internal class LoggerImpl(
    override val level: Level,
    override val transports: List<Transport>,
    override val formatter: (Message) -> String,
): Logger {
    override fun log(message: Message) =
        transports.filter { transport ->
            message.level.greaterOrEqual(transport.level)
        }.forEach {
            it.log(formatter(message))
        }
}