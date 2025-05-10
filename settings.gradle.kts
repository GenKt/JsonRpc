dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

rootProject.name = "GenKt"

fun genKtModules(vararg modules: String) {
    for (module in modules) {
        include(":genkt-$module")
        project(":genkt-$module").projectDir = file(module)
    }
}

genKtModules(
    "generate-core",
    "mcp-common",
    "mcp-server",
    "mcp-client",
    "mcp-test"
)

