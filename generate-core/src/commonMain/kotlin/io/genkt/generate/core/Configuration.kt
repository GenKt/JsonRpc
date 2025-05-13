package io.genkt.generate.core

import kotlin.reflect.KMutableProperty1

public interface ConfiguredGenerator<Request, Response> {
    public val currentRequest: Provider<Request>
    public val generator: Generator<Request, Response>
}

public class ConfigurableGenerator<Request, Response>(
    public override val currentRequest: Provider<Request>,
    public override val generator: Generator<Request, Response>,
) : Generator<Request, Response> by generator, ConfiguredGenerator<Request, Response>

public fun <Request, RequestBuilder, Response> Generator<Request, Response>.configurable(
    requestBuilder: Provider<RequestBuilder>,
    buildRequest: RequestBuilder.() -> Request,
): ConfigurableGenerator<RequestBuilder, Response> =
    ConfigurableGenerator(requestBuilder) { this(requestBuilder().buildRequest()) }

public fun <Request, Response> Generator<Request, Response>.configurable(
    buildRequest: Provider<Request>,
): ConfigurableGenerator<Request, Response> = ConfigurableGenerator(buildRequest, this)

public fun <HistoryItem, Response> Generator<History<HistoryItem>, Response>.configurable(): ConfigurableGenerator<MutableHistory<HistoryItem>, Response> =
    configurable { mutableListOf() }

public fun <Request, Response> ConfiguredGenerator<Request, Response>.configure(
    buildRequest: Request.() -> Unit
): ConfiguredGenerator<Request, Response> =
    ConfigurableGenerator({ currentRequest().apply(buildRequest) }, generator)

public fun <Request, Input, Response> ConfiguredGenerator<Request, Response>.generateBy(
    transform: suspend Request.(Input) -> Unit,
): Generator<Input, Response> = { input -> generator(currentRequest().apply { transform(input) }) }

public fun <Request, Input, Response> ConfiguredGenerator<Request, Response>.generateBy(
    property: KMutableProperty1<Request, Input>
) = generateBy { input: Input -> property.set(this, input) }

public fun <Request, Input : Any, Response> ConfiguredGenerator<Request, Response>.generateByNotNullable(
    property: KMutableProperty1<Request, Input?>
) = generateBy(property).notNullableInput()