rootProject.name = "GenKt-root"

val projects = listOf(
    "core"
)

include(projects)

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}