plugins {
    id("kotlin-multiplatform-ktor")
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
                api(libs.ktor.websockets)
            }
        }
    }
}