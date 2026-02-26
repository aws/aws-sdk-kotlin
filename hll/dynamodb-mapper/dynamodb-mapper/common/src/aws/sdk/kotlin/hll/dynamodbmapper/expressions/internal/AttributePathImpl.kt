/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.Attr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.AttrPathElement
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.AttributePath

internal data class AttrPathNameImpl(override val name: String) : AttrPathElement.Name

internal data class AttrPathIndexImpl(override val index: Int) : AttrPathElement.Index

internal data class AttributePathImpl(
    override val element: AttrPathElement,
    override val parent: AttributePath? = null,
) : AttributePath {
    init {
        require(element is AttrPathElement.Name || parent != null) {
            "Top-level attribute paths must be names (not indices)"
        }
    }

    override fun get(index: Int) = AttributePath(index, parent = this)
    override fun get(key: String) = AttributePath(key, parent = this)
}

internal object AttrImpl : Attr {
    override fun get(name: String) = AttributePath(name)
}
