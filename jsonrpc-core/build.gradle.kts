plugins {
    id("kotlin-multiplatform-full")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.atomicfu)
    id("publishing-convention")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.coroutine.core)
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.io.core)
                implementation(libs.streamlin)
            }
        }
    }
}