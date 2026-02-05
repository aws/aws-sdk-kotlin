/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters

import aws.sdk.kotlin.hll.mapping.core.util.Either
import aws.sdk.kotlin.hll.mapping.core.util.map
import aws.sdk.kotlin.hll.mapping.core.util.merge
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Models partial, asymmetrical conversion between a type [F] and a type [T], where some condition internal to the
 * converter splits the possible pathways data make take through conversion logic. One of these branches will be more
 * complex and involve converting types [F] and [T] to types [F2] and [T2] (respectively). The remaining conversion
 * between [F2] and [T2] will typically be delegated to another converter.
 *
 * Because they are partial and asymmetrical, [SplittingConverter] instances are typically not very useful on their own.
 * Most often they are combined with another [Converter] via the [mergeBy] extension method, forming a complete,
 * symmetrical converter between [F] and [T].
 *
 * Splitting converters use the [Either] type to denote values which may follow the simple branch ([Either.Left]) or the
 * complex branch ([Either.Right]).
 *
 * @param F The overall type being converted from
 * @param F2 The intermediate type being converted from on the complex branch
 * @param T2 The intermediate type being converted to on the complex branch
 * @param T The overall type being converted to
 */
@ExperimentalApi
public interface SplittingConverter<F, F2, T2, T> :
    ConvertsTo<F, Either<T, F2>>,
    ConvertsFrom<Either<F, T2>, T>

/**
 * Merges this [SplittingConverter] by delegating to a [Converter] instance that converts between types [F2] and [T2].
 * After the merge, a new [Converter] will be returned which fully converts between types [F] and [T].
 * @param F The overall type being converted from
 * @param F2 The intermediate type being converted from on the complex branch, which is also the source type of
 * [converter]
 * @param T2 The intermediate type being converted to on the complex branch, which is also the target type of
 * [converter]
 * @param T The overall type being converted to
 * @param converter A [Converter] between types [F2] and [T2]
 */
@ExperimentalApi
public fun <F, F2 : F, T, T2 : T> SplittingConverter<F, F2, T2, T>.mergeBy(
    converter: Converter<F2, T2>,
): Converter<F, T> = Converter(
    convertTo = { from: F -> this@mergeBy.convertTo(from).map(converter::convertTo).merge() },
    convertFrom = { to: T -> this@mergeBy.convertFrom(to).map(converter::convertFrom).merge() },
)
