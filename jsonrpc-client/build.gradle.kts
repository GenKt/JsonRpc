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
                implementation(project(":genkt-jsonrpc-core"))
            }
        }
    }
}