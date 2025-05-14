plugins {
    id("kotlin-multiplatform-full")
    id("publishing-convention")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":genkt-logging-api"))
            }
        }
    }
}