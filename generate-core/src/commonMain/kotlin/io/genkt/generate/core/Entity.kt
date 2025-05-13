package io.genkt.generate.core

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
public enum class DataDirection {
    Input,
    Output
}

public interface Message<Content> {
    public val direction: DataDirection
    public val content: Content
}

@Serializable
@JvmInline
public value class SystemInstruction(public val instruction: String)

@Serializable
@JvmInline
public value class Text(public val text: String)

@Serializable
@JvmInline
public value class Reasoning(public val reasoning: String)

@Serializable
public data class ToolCallRequest(
    public val toolId: String,
    public val param: String
)


@Serializable
@JvmInline
public value class ToolCallResult(
    public val result: String
)