plugins {
    id("kotlin-multiplatform-full")
    id("publishing-convention")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":jsonrpc-common"))
            }
        }
    }
}