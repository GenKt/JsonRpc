@file:Suppress("nothing_to_inline")

package io.genkt.generate.core

import kotlinx.coroutines.runBlocking

public inline fun <T, R> (suspend (T) -> R).blocking(): (T) -> R = { runBlocking { invoke(it) } }