/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.core.SymbolRef
import aws.sdk.kotlin.hll.codegen.model.Types
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.KeyAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.MappedProperty
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes

/**
 * Resolves the key spec types for a key [property]. This is the sole detection site for unsupported key attribute
 * types.
 * @param symbol The source symbol an unsupported-type error is reported against
 * @return the resolved key, or `null` if the property's type is not a supported key attribute type (reported to
 * [diagnostics])
 */
internal fun resolveKeyAttribute(
    property: MappedProperty,
    symbol: SymbolRef?,
    diagnostics: DiagnosticCollector,
): KeyAttribute? {
    val type = property.type
    val (keySpecType, thenType) = when (type) {
        Types.Kotlin.Byte -> MapperTypes.Items.KeySpecByte to MapperTypes.Items.KeySpecThenByte
        Types.Kotlin.ByteArray -> MapperTypes.Items.KeySpecByteArray to MapperTypes.Items.KeySpecThenByteArray
        Types.Kotlin.Int -> MapperTypes.Items.KeySpecInt to MapperTypes.Items.KeySpecThenInt
        Types.Kotlin.Long -> MapperTypes.Items.KeySpecLong to MapperTypes.Items.KeySpecThenLong
        Types.Kotlin.Short -> MapperTypes.Items.KeySpecShort to MapperTypes.Items.KeySpecThenShort
        Types.Kotlin.String -> MapperTypes.Items.KeySpecString to MapperTypes.Items.KeySpecThenString
        else -> {
            diagnostics.error("Unsupported key attribute type $type", symbol)
            return null
        }
    }
    return KeyAttribute(property.ddbName, type, keySpecType, thenType)
}
