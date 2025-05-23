plugins {
    id("kotlin-multiplatform-full")
    id("publishing-convention")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":mcp-common"))
                implementation(project(":jsonrpc-client"))
                implementation(project(":jsonrpc-server"))
            }
        }
    }
}