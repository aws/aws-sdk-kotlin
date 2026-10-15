/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model

import aws.sdk.kotlin.hll.codegen.model.TypeRef

/**
 * Describes the item converter for a [MappedItem]
 */
internal sealed interface ItemConverterSpec {
    /**
     * The converter type referenced by the schema and item-level value converter
     */
    val converter: TypeRef

    /**
     * An item converter generated for the item. The generated object is named after [converter]'s short name.
     * @param attributes One attribute descriptor per mapped property, in property order
     */
    data class Generated(
        override val converter: TypeRef,
        val attributes: List<GeneratedAttribute>,
    ) : ItemConverterSpec

    /**
     * A user-provided item converter (via the marker annotation's converter argument). Always referenced as-is. Its
     * attributes' value converters are the user's concern, so they are never resolved.
     */
    data class UserProvided(override val converter: TypeRef) : ItemConverterSpec
}

/**
 * An attribute descriptor within a generated item converter
 * @param property The mapped property
 * @param converter The fully-resolved value converter for the property
 */
internal data class GeneratedAttribute(
    val property: MappedProperty,
    val converter: ValueConverterSpec,
)
