# Serialization JSON Module

The `serialization-json` module provides custom serialization utilities for working with `kotlinx.serialization-json`. 

## Design Principles

- Extend `kotlinx.serialization`
- Avoid reflexion

## Components

### `JsonPolymorphicSerializer`

This class aims to provide a no-reflection solution for custom polymorphic serialization of JSON, 
instead of `kotlinx.serialization.json.JsonContentPolymorphicSerializer`, making it more friendly to native images.

You can use Kotlin's delegation to mix it into your own serializer:

```
object MySerializer: KSerializer<MyType> by JsonPolymorphicSerializer(...)
```