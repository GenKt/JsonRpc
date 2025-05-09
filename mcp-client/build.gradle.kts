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
                api(project(":genkt-mcp-common"))
                api(libs.ktor.client.cio)
            }
        }
    }
}