package io.genkt.generate.core

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

public fun <Input, Output> Generator<Input, Output>.limitedParallelism(max: Int): Generator<Input, Output> {
    require(max > 0) { "max should be greater than 0" }
    val semaphore = Semaphore(max)
    return { semaphore.withPermit { this(it) } }
}