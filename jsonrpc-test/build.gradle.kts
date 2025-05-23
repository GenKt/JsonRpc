import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("kotlin-multiplatform-full")
}

kotlin {
    sourceSets {
        all {
            dependencies {
                implementation(project(":jsonrpc-client"))
                implementation(project(":jsonrpc-server"))
                implementation(project(":jsonrpc-transport-memory"))
                implementation(project(":jsonrpc-transport-stdio"))
                implementation(libs.kotlin.coroutines.test)
                implementation(libs.kotlin.test)
            }
        }
    }
    explicitApi = ExplicitApiMode.Disabled
}
