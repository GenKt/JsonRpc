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
    "serialization-json",
    "jsonrpc-core",
    "jsonrpc-client",
    "jsonrpc-server",
    "jsonrpc-test",
    "jsonrpc-transport-stdio",
    "jsonrpc-transport-memory",
    "mcp-common",
    "mcp-server",
    "mcp-client",
    "mcp-sdk-common",
    "mcp-sdk-server",
    "mcp-sdk-client",
    "mcp-sdk-test"
)

