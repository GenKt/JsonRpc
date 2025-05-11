@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("kotlin-multiplatform-base")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.atomicfu)
}

kotlin {
    configureJvm(8)
    configureJs()
    wasmJs {
        nodejs()
    }
    configureNative()
}