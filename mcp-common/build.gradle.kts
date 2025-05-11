plugins {
    id("kotlin-multiplatform-ktor")
    id("publishing-convention")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":genkt-jsonrpc-core"))
                api(libs.kotlinx.coroutine.core)
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.io.core)
                api(libs.ktor.websockets)
            }
        }
    }
}