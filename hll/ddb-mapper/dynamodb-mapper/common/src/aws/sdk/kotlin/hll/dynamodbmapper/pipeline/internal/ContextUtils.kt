/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal

internal interface Combinable<T, V> {
    operator fun plus(value: V): T
}

internal interface ErrorCombinable<T> {
    val error: Throwable?
    operator fun plus(e: Throwable?): T
}

internal fun Throwable?.suppressing(e: Throwable?) = this?.apply { e?.let(::addSuppressed) } ?: e
