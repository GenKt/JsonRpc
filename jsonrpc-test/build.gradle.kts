plugins {
    id("kotlin-multiplatform-full")
}

kotlin {
    sourceSets {
        commonTest {
            dependencies {
                implementation(project(":genkt-jsonrpc-client"))
                implementation(project(":genkt-jsonrpc-server"))
                implementation(project(":genkt-jsonrpc-transport-stdio"))
                implementation(libs.kotlin.test)
            }
        }
    }
}