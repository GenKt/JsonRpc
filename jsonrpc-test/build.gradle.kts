plugins {
    id("kotlin-multiplatform-full")
}

kotlin {
    sourceSets {
        commonTest {
            dependencies {
                implementation(project(":genkt-jsonrpc-client"))
                implementation(project(":genkt-jsonrpc-server"))
                implementation(project(":genkt-jsonrpc-transport-memory"))
                implementation(libs.kotlin.coroutines.test)
                implementation(libs.kotlin.test)
            }
        }
    }
}