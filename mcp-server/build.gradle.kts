plugins {
    id("kotlin-multiplatform-ktor")
    id("publishing-convention")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":genkt-mcp-common"))
                api(libs.ktor.server.cio)
                api(libs.ktor.server.sse)
                api(libs.ktor.server.websockets)
            }
        }
    }
}