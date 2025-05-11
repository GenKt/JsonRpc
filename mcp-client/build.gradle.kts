plugins {
    id("kotlin-multiplatform-ktor")
    id("publishing-convention")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":genkt-mcp-common"))
                api(libs.ktor.client.cio)
            }
        }
    }
}