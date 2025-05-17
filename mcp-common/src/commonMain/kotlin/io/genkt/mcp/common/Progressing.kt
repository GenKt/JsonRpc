package io.genkt.mcp.common

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel

public data class Progress(
    public val progress: Int,
    public val total: Int,
    public val message: String,
)

public interface ProgressingResult<R> {
    public val progressChannel: Channel<Progress>
    public val result: Deferred<R>
}

internal class ProgressingResultImpl<R>(
    override val result: Deferred<R>,
    override val progressChannel: Channel<Progress> = Channel(),
): ProgressingResult<R>

@Suppress("FunctionName")
public fun <R> ProgressResult(
    result: Deferred<R>,
    progressChannel: Channel<Progress> = Channel(),
): ProgressingResult<R> =
    ProgressingResultImpl(result, progressChannel)