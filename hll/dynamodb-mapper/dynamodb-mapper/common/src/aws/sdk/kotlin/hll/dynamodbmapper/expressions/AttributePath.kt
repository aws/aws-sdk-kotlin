/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.AttrPathIndexImpl
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.AttrPathNameImpl
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.AttributePathImpl

/**
 * Represents an element in an [AttributePath]
 */
public sealed interface AttrPathElement {
    /**
     * Represents the name of a top-level attribute or a key in a map
     */
    public interface Name : AttrPathElement {
        /**
         * The name or key of this element
         */
        public val name: String
    }

    /**
     * Represents an index into a list/set
     */
    public interface Index : AttrPathElement {
        /**
         * The index (starting at `0`)
         */
        public val index: Int
    }
}

/**
 * Represents an expression that consists of an attribute. Attributes are referenced by attribute paths, analogous to
 * [document paths in DynamoDB](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.Attributes.html#Expressions.Attributes.NestedElements.DocumentPathExamples).
 * Attribute paths consist of one or more elements, which are either names (e.g., of a top-level attribute or a nested
 * key in a map attribute) or indices (i.e., into a list). The first (and often only) element of an attribute path is a
 * name.
 *
 * See [Filter] for more information about creating references to attributes.
 */
public interface AttributePath : Expression {
    /**
     * The [AttrPathElement] for this path
     */
    public val element: AttrPathElement

    /**
     * The parent [AttributePath] (if any). If [parent] is `null` then this instance represents a top-level attribute
     * and [element] must be a name (not an index).
     */
    public val parent: AttributePath?

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new [AttributePath] reference with the given name and optional parent path
 * @param name The name or key of this element
 * @param parent The parent [AttributePath] (if any) of this element. If [parent] is `null` then this instance
 * represents a top-level attribute.
 */
public fun AttributePath(name: String, parent: AttributePath? = null): AttributePath =
    AttributePathImpl(AttrPathNameImpl(name), parent)

/**
 * Creates a new [AttributePath] reference with the given index and parent path
 * @param index The index (starting at `0`) of this element
 * @param parent The parent [AttributePath] of this element
 */
public fun AttributePath(index: Int, parent: AttributePath): AttributePath =
    AttributePathImpl(AttrPathIndexImpl(index), parent)
