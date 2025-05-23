plugins {
    id("kotlin-multiplatform-full")
    id("publishing-convention")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":jsonrpc-common"))
                api(project(":serialization-json"))
                implementation(libs.streamlin)
                api(libs.kotlinx.coroutine.core)
                api(libs.kotlinx.serialization.json)
            }
        }
    }
}