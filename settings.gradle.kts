dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

rootProject.name = "jsonrpc"

include(
    "serialization-json",
    "jsonrpc-common",
    "jsonrpc-client",
    "jsonrpc-server",
    "jsonrpc-test",
    "jsonrpc-transport-stdio",
    "jsonrpc-transport-memory",
)

