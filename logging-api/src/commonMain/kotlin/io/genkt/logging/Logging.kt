package io.genkt.logging

public enum class Level {
    DEBUG, INFO, WARN, ERROR
}

public class Message(
    public val level: Level,
    public val timestamp: Long,
    public val message: String,
)

public interface Logger {
    public val level: Level
    public val transports: List<Transport>
    public val formatter: (Message) -> String
    public fun log(level: Level, message: String)
}

public interface Transport {
    public val level: Level
    public fun log(level: Level, message: String)
}

public fun Logger(
    level: Level = Level.INFO,
    transports: List<Transport> = emptyList(),
    formatter: (Message) -> String = { "${it.timestamp} [${it.level}] ${it.message}" },
): Logger = LoggerImpl(
    level = level,
    transports = transports,
    formatter = formatter,
)