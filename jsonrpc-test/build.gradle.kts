plugins {
    id("kotlin-multiplatform-full")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":genkt-jsonrpc-core"))
            }
        }
        commonTest {
            dependencies {
                implementation(project(":genkt-jsonrpc-client"))
                implementation(project(":genkt-jsonrpc-server"))
                implementation(libs.kotlin.test)
            }
        }
    }
}