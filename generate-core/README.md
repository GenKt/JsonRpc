# Generation API

## The idea of Generation API

Generally, the way we use a LLM API is to send a request in JSON,
and then we get a response in JSON or streaming JSON.
The request we send and the response we get are both huge JSONs,
which have many details and parameters.

### Request: configure and generate

As for the request, there are two kinds of things exist.

The first kind of thing is some parameters that we often configure somewhere and use it through multiple requests.
For example, the model name, the temperature, etc.
The second kind of thing is the actual input to the model. 
They usually contain the prompt, the input data, the system instruction, etc.
They are all in the request JSON, but they are often processed in different stages. 
Also, they might be different for different API providers.

Generation API aims to unify the two kinds of things and provide a DSL-styled API to configure and generate.

To achieve a DSL, we need a mutable builder class for the request. So that we can write that:

```
configure { // receiver: RequestBuilder
    model = "your-model-name"
    temperature = <your-temperature>
}
```

Also, the whole process of configuring should be lazy and wrapping, so we can write that:

```
val base = ConfigurableGenerator<RequestBuilder> {
    apiKey = "your-api-key"
}
val version1 = base.configure {
    model = "model1"
}
val version2 = base.configure {
    model = "model2"
}
// Then we can use version1 and version2 to with for different models, sharing the same apiKey
```

After we complete the configuration, we need to fill the request with the input.
We want to use the generator like a function, accepting the input and returning the output.
And we can easily process the input and output with a DSL.

```
val generator1 = version1.generateBy(RequestBuilder::messages) // (List<Message>) -> Flow<ResponseChunk>
    .mapInput { newInput: Message -> listOf(newInput) } // (Message) -> Flow<RequestChunk>
    .mapInput ( newInput: String -> Message.user(newInput) ) // (String) -> Flow<RequestChunk>
    .mapOutput { response -> response.content } // (String) -> Flow<String>

generator1("hello") // Flow<String>
    .collect { print(it) } // collect the output
```

We use a functional way to implement these features.

### Response & message: inspired from Rust's traits

We are tired of such code:

```
val responseContent = response.choices[0].message.content
val responseReasoningOrNull = response.choices[0].message.reasoningContent
```

Generation API aims to provide a unified, simple and safe way to extract content from response and chat message.

If we have such a response like above, we can use interface to make it more typesafe:

```
@Serializable
data class Response(
    val choices: List<Choice>,
    val tokenUsage: Long,
): HaveText by choices.first(),
    MayHaveReasoning by choices.first()

@Serializable
data class Choice(
    val message: Message,
    val finishReason: String? = null,
): HaveText by message,
    MayHaveReasoning by message

@Serializable
data class Message(
    val role: String,
    @SerialName("reasoning_content")
    override val reasoning: String? = null,
    @SerialName("content")
    override val text: String,
): HaveText, MayHaveReasoning
```

Then we can use the interface to extract the content and reasoning from the response:

```
val output = response.text
val reasoning = response.reasoningOrNull
```