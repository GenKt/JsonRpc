plugins {
    id("kotlin-multiplatform-ktor")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":genkt-mcp-sdk-client"))
                api(project(":genkt-mcp-sdk-server"))
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}