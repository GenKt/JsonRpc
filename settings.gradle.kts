dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

rootProject.name = "mcp-sdk"

fun genKtModules(vararg modules: String) {
    for (module in modules) {
        include(":genkt-$module")
        project(":genkt-$module").projectDir = file(module)
    }
}

genKtModules(
    "serialization-json",
    "jsonrpc-common",
    "jsonrpc-client",
    "jsonrpc-server",
    "jsonrpc-test",
    "jsonrpc-transport-stdio",
    "jsonrpc-transport-memory",
    "mcp-common",
    "mcp-server",
    "mcp-client",
)

