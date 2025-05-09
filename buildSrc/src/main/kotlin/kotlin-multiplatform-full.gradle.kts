plugins {
    id("kotlin-multiplatform-base")
}

kotlin {
    configureJvm(8)
    configureJs()
    configureWasm()
    configureNative()
}