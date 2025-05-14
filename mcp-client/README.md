# GenKt MCP Client Library
An MCP Client Library developed by GenKt Team.  
Provide a better experience in developing MCP applications than official SDK.

## Introduction
As MCP has grown up to a marvelous position in the LLM community, it has widely used for extending LLM's function and building efficient LLM applications.  
However, the official Kotlin SDK 

## API Design
### `interface McpClient`
The Client entity of the MCP Client. Used to send MCP requests and handle requests from server.  
You can use an inceptor to extend or modify its behavior.
#### Properties
##### `name: String`
The name of the client. Specified in MCP Specification
##### `version: String`
The version of the client. Specified in MCP Specification
##### 
```kotlin

interface McpClient {
    val name: String
    val version: String
}
```