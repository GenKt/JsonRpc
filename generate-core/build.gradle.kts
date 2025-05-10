plugins {
    id("kotlin-multiplatform-full")
    alias(libs.plugins.kotlinSerialization)
    id("publishing-convention")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.coroutine.core)
                api(libs.kotlinx.serialization.core)
            }
        }
    }
}