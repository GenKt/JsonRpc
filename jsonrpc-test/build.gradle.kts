plugins {
    id("kotlin-multiplatform-full")
}

kotlin {
    sourceSets {
        all {
            dependencies {
                implementation(project(":genkt-jsonrpc-client"))
                implementation(project(":genkt-jsonrpc-server"))
                implementation(project(":genkt-jsonrpc-transport-memory"))
                implementation(project(":genkt-jsonrpc-transport-stdio"))
                implementation(libs.kotlin.coroutines.test)
                implementation(libs.kotlin.test)
            }
        }
    }
}
