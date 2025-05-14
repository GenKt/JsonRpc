package io.genkt.logging

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

public enum class Level {
    DEBUG, INFO, WARN, ERROR;
    public fun greaterOrEqual(level: Level): Boolean = ordinal >= level.ordinal
}

public class Message(
    public val level: Level,
    public val timestamp: Instant,
    public val message: String,
)

public interface Logger {
    public val level: Level
    public val transports: List<Transport>
    public val formatter: (Message) -> String
    public fun log(message: Message)
}

public interface Transport {
    public val level: Level
    public fun log(level: Level, message: String)
}

public fun Logger(
    level: Level = Level.INFO,
    transports: List<Transport> = emptyList(),
    formatter: (Message) -> String = { "[${it.timestamp.toLocalDateTime(TimeZone.UTC)}][${it.level}]${it.message}" },
): Logger = LoggerImpl(
    level = level,
    transports = transports,
    formatter = formatter,
)