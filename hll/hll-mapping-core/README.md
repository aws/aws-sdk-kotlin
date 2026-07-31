# HLL Converters

This module provides a framework for typed data mapping, including base interfaces and some reusable implementations.
This document describes the theory behind the conversion framework and how it may be applied.

## Terminology

In the context of this library, the following terms are used:

* **Converter** — an interface capable of bidirectional data mapping between two types, referred to as **left** and
  **right**. Implementations provide `convertRight` and `convertLeft` methods directly.
* **Left** — the name for the type on one side of a bidirectional data mapping. By convention, this is typically used to
  represent types that are closer to your application or business logic. One may think of **left** types as "my" types.
* **Right** — the name for the type on one side of a bidirectional data mapping. By convention, this is typically used
  to represent types that are farther away from your application or business logic. One may think of **right** types as
  "their" types.

## Interfaces and classes

### Converter

The core interface for bidirectional data mapping:

```kotlin
interface Converter<L, R> {
    fun convertRight(from: L): R
    fun convertLeft(from: R): L
}
```

`Converter` is a generic type which maps between `L` (the left type) and `R` (the right type). Implementations directly
override `convertRight` and `convertLeft` to provide conversion logic.

### ConverterImpl

A simple implementation of `Converter` backed by lambda functions:

```kotlin
class ConverterImpl<L, R>(
    private val convertRight: (L) -> R,
    private val convertLeft: (R) -> L,
) : Converter<L, R>
```

This is the standard way to create a converter from a pair of conversion functions.

### ConverterChain

A converter that composes two converters in sequence:

```kotlin
open class ConverterChain<L, M, R>(
    private val first: Converter<L, M>,
    private val next: Converter<M, R>,
) : Converter<L, R>
```

`ConverterChain` performs rightward conversion by passing data through `first` then `next`, and leftward conversion by
passing data through `next` then `first`. Because it is an open class, it can be used as a superclass for converters
that are naturally expressed as a composition of two stages.

### Example usage

A simple converter that maps between a string and a boolean could be implemented as follows:

```kotlin
val converter = ConverterImpl<String, Boolean>(
    convertRight = { input ->
        when (input.lowercase()) {
            "yes", "true" -> true
            "no", "false" -> false
            else -> error("Cannot convert '$input' to Boolean!")
        }
    },
    convertLeft = { input ->
        when (input) {
            true -> "yes"
            false -> "no"
        }
    },
)
println(converter.convertRight("no")) // Prints false
println(converter.convertLeft(false)) // Prints "no"
```

## Composing converters

Sometimes data transformations are complex or share common logic with other transformations. In these cases, simpler
converters may be reused by composing them. There are two primary forms of composition:

### Chaining

Converters may be "chained" together in a series of sequential transformations. In a chain, the output of one converter
becomes the input to another:

![](docs/img/chaining-converter.png)

`ConverterChain` is used to chain individual converters together:

```kotlin
open class ConverterChain<L, M, R>(
    private val first: Converter<L, M>,
    private val next: Converter<M, R>,
) : Converter<L, R>
```

The generic variable `M` represents the **middle** data type. Because a chained converter passes data from one delegate
to the next, the joining types of the converters must be the same. In other words, the right type of the first converter
and the left type of the next converter must match.

The following example shows how to combine two existing converters:

```kotlin
val shortToInt: Converter<Short, Int> = ...
val intToLong: Converter<Int, Long> = ...

val shortToLong: Converter<Short, Long> = ConverterChain(shortToInt, intToLong)
```

In the preceding example, `shortToLong` is a chained converter which converts rightward by turning `Short` into `Int`
by delegating to `shortToInt` and then turning the resulting `Int` into `Long` by delegating to `intToLong`. Conversely,
it converts leftward by turning `Long` into `Int` by delegating to `intToLong` and then turning the resulting `Int` into
`Short` by delegating to `shortToInt`.

Because `ConverterChain` is itself a `Converter`, it may be further chained with more converters:

```kotlin
val byteToShort: Converter<Byte, Short> = ...
val shortToInt: Converter<Short, Int> = ...
val intToLong: Converter<Int, Long> = ...

val byteToLong: Converter<Byte, Long> = ConverterChain(ConverterChain(byteToShort, shortToInt), intToLong)
```

`ConverterChain` is also commonly used as a superclass for converters that are naturally expressed as a two-stage
composition:

```kotlin
class CharValueConverter : ConverterChain<Char, String, AttributeValue>(
    TextConverters.Char,
    StringValueConverter,
)
```

### Element mapping

Converters may be wrapped by a collection converter in order to transform collection elements from one type to another:

![](docs/img/element-mapping-converter.png)

Collection converters are typically built using factory functions which accept one or more delegate converters as
arguments. For example:

```kotlin
fun <L, R> ListMappingConverter(delegate: Converter<L, R>): Converter<List<L>, List<R>>
```

The preceding function creates a converter which transforms between `List<L>` and `List<R>` by mapping over each element
and using the delegate converter to transform between element types `L` and `R`.

The following example illustrates composing an element converter with a list mapping converter:

```kotlin
val intToLong: Converter<Int, Long> = ...
val intListToLongList: Converter<List<Int>, List<Long>> = ListMappingConverter(intToLong)
```

In the preceding example, `intToLong` is passed as a delegate to the `ListMappingConverter` factory function and a list
mapping converter is returned. When a list is transformed through this converter, each element is passed to the delegate
for individual transformation in either direction.
