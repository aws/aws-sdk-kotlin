/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Identifies how to handle attributes whose keys aren't in an item schema when converting from an item to an entity
 * type
 * @param B The type of builder object used to construct instances of the entity type
 */
public sealed interface UnknownValueHandling<out B> {
    /**
     * Indicates that unknown attributes should be ignored
     */
    public data object Ignore : UnknownValueHandling<Nothing>

    /**
     * Indicates that unknown attributes should cause an exception to be thrown
     */
    public data object ThrowException : UnknownValueHandling<Nothing>

    /**
     * Indicates that unknown attributes should be passed to a user-defined function for custom handling
     */
    public fun interface Custom<B> : UnknownValueHandling<B> {
        /**
         * Handle an attribute whose key isn't present in an item schema. Implementations may throw exceptions (which
         * causes item conversion to fail), take no action, or take action on the [builder] to read/update entity
         * properties.
         * @param name The key of the attribute
         * @param value The value of the attribute
         * @param builder An instance of a builder object being used to construct an entity type, which may be used to
         * read and set entity properties
         */
        public fun handleUnknown(name: String, value: AttributeValue, builder: B)
    }
}
