/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.Attr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.AttrPathElement
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.AttributePath

internal data class AttrPathNameImpl(override val name: String) : AttrPathElement.Name {
    override fun toString(): String = name
}

internal data class AttrPathIndexImpl(override val index: Int) : AttrPathElement.Index {
    override fun toString(): String = "[$index]"
}

internal data class AttributePathImpl(
    override val element: AttrPathElement,
    override val parent: AttributePath? = null,
) : AttributePath {
    private val dottedNotation by lazy {
        buildString {
            parent?.let { append(it.toString()) }
            if (isNotEmpty() && element is AttrPathElement.Name) {
                append('.')
            }
            append(element.toString())
        }
    }

    init {
        require(element is AttrPathElement.Name || parent != null) {
            "Top-level attribute paths must be names (not indices)"
        }
    }

    override fun get(index: Int) = AttributePath(index, parent = this)
    override fun get(key: String) = AttributePath(key, parent = this)
    override fun toString(): String = dottedNotation
}

internal object AttrImpl : Attr {
    override fun get(name: String) = AttributePath(name)
}
