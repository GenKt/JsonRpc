plugins {
    id("kotlin-multiplatform-ktor")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.atomicfu)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":genkt-mcp-client"))
                api(project(":genkt-mcp-server"))
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}