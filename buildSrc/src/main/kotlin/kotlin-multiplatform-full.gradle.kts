plugins {
    id("kotlin-multiplatform-base")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.atomicfu)
}

kotlin {
    configureJvm(8)
    configureJs()
    configureWasm()
    configureNative()
}