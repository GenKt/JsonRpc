plugins {
    id("kotlin-multiplatform-full")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":genkt-jsonrpc-client"))
                api(project(":genkt-jsonrpc-server"))
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}