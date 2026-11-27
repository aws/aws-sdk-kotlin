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

    /**
     * A `HeterogeneousItemConverter` generated for a family. The generated object is named after [converter]'s short
     * name.
     * @param typeAttribute The attribute storing each item's type name
     * @param registrations The family's registrations, already in the order they must be registered
     */
    data class Heterogeneous(
        override val converter: TypeRef,
        val typeAttribute: String,
        val registrations: List<SubtypeRegistration>,
    ) : ItemConverterSpec
}

/**
 * A single registration within a [ItemConverterSpec.Heterogeneous] converter
 * @param typeName The value stored in the type attribute for matching objects
 * @param type The registered class
 * @param exact Whether objects must be exactly of [type] (`exactType`) rather than instances of it (`instanceOf`)
 * @param converter The item converter for matching objects
 */
internal data class SubtypeRegistration(
    val typeName: String,
    val type: TypeRef,
    val exact: Boolean,
    val converter: TypeRef,
)

/**
 * An attribute descriptor within a generated item converter
 * @param property The mapped property
 * @param converter The fully-resolved value converter for the property
 */
internal data class GeneratedAttribute(
    val property: MappedProperty,
    val converter: ValueConverterSpec,
)
