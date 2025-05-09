@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("kotlin-multiplatform-base")
}

kotlin {
    configureJvm(8)
    configureJs()
    wasmJs {
        nodejs()
    }
    configureNative()
}