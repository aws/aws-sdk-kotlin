/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.util

import aws.sdk.kotlin.hll.codegen.rendering.RenderOptions.VisibilityAttribute
import aws.sdk.kotlin.hll.codegen.rendering.Visibility
import aws.smithy.kotlin.runtime.collections.*

/**
 * Combines this [Attributes] collection with another collection and returns the new result
 * @param other The other attributes to merge
 */
operator fun Attributes.plus(other: Attributes): Attributes = toMutableAttributes().apply { merge(other) }

/**
 * Adds another attribute to this collection and returns the new result
 * @param other A tuple of [AttributeKey] to a value (which may be `null`)
 */
operator fun <T : Any> Attributes.plus(other: Pair<AttributeKey<T>, T?>): Attributes =
    toMutableAttributes().apply {
        other.second?.let { set(other.first, it) } ?: remove(other.first)
    }

/**
 * Convert this [Attributes]' [VisibilityAttribute] to a string
 */
public val Attributes.visibility: String
    get() = when (this[VisibilityAttribute]) {
        Visibility.PUBLIC -> "public "
        Visibility.INTERNAL -> "internal "
    }
