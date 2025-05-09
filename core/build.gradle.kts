plugins {
    id("kotlin-multiplatform-convention-full")
    alias(libs.plugins.kotlinSerialization)
    id("publishing-convention")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.coroutine.core)
            }
        }
    }
}