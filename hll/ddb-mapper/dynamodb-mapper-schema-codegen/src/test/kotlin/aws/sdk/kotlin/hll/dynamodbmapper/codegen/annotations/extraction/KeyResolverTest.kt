/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.Types
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.KeyAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.MappedProperty
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KeyResolverTest {
    private fun prop(type: TypeRef) = MappedProperty(name = "id", ddbName = "pk", type = type)

    @Test
    fun testSupportedKeyTypes() {
        val expected = mapOf(
            Types.Kotlin.Byte to (MapperTypes.Items.KeySpecByte to MapperTypes.Items.KeySpecThenByte),
            Types.Kotlin.ByteArray to (MapperTypes.Items.KeySpecByteArray to MapperTypes.Items.KeySpecThenByteArray),
            Types.Kotlin.Int to (MapperTypes.Items.KeySpecInt to MapperTypes.Items.KeySpecThenInt),
            Types.Kotlin.Long to (MapperTypes.Items.KeySpecLong to MapperTypes.Items.KeySpecThenLong),
            Types.Kotlin.Short to (MapperTypes.Items.KeySpecShort to MapperTypes.Items.KeySpecThenShort),
            Types.Kotlin.String to (MapperTypes.Items.KeySpecString to MapperTypes.Items.KeySpecThenString),
        )

        expected.forEach { (type, specs) ->
            val diagnostics = DiagnosticCollector()
            assertEquals(KeyAttribute("pk", type, specs.first, specs.second), resolveKeyAttribute(prop(type), null, diagnostics))
            assertEquals(emptyList(), diagnostics.diagnostics)
        }
    }

    @Test
    fun testUnsupportedKeyTypeReportsError() {
        val diagnostics = DiagnosticCollector()
        assertNull(resolveKeyAttribute(prop(Types.Kotlin.Boolean), null, diagnostics))
        assertEquals(
            "Unsupported key attribute type ${Types.Kotlin.Boolean}",
            diagnostics.diagnostics.single().message,
        )
    }
}
