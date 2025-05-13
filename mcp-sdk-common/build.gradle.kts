plugins {
    id("kotlin-multiplatform-ktor")
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