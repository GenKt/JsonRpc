dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

rootProject.name = "mcp-sdk"

include(
    "serialization-json",
    "jsonrpc-common",
    "jsonrpc-client",
    "jsonrpc-server",
    "jsonrpc-test",
    "jsonrpc-transport-stdio",
    "jsonrpc-transport-memory",
)

