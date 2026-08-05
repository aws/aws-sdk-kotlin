/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters

public open class ConverterChain<L, M, R>(
    private val first: Converter<L, M>,
    private val next: Converter<M, R>,
) : Converter<L, R> {
    override fun convertRight(from: L): R = next.convertRight(first.convertRight(from))
    override fun convertLeft(from: R): L = first.convertLeft(next.convertLeft(from))
}
