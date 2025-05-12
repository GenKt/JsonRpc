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
    "jsonrpc-core",
    "jsonrpc-client",
    "jsonrpc-server",
    "jsonrpc-test",
    "jsonrpc-transport-stdio",
    "mcp-common",
    "mcp-server",
    "mcp-client",
    "mcp-test"
)

