/*
    Copied from https://github.com/Kotlin/kotlinx.serialization
 */
plugins {
    id("kotlin-multiplatform-convention-base")
}

kotlin {
    configureJvm(8)
    configureJs()
    configureWasm()
    configureNative()
}