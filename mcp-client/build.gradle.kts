plugins {
    id("kotlin-multiplatform-full")
    id("publishing-convention")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":genkt-mcp-common"))
                implementation(project(":genkt-jsonrpc-client"))
                implementation(project(":genkt-jsonrpc-server"))
            }
        }
    }
}