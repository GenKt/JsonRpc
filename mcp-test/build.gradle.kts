plugins {
    id("kotlin-multiplatform-ktor")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":genkt-mcp-client"))
                api(project(":genkt-mcp-server"))
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}