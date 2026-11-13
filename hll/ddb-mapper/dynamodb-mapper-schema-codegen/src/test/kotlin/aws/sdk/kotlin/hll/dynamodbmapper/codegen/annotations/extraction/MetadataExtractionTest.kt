/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.ksp.asSymbolRef
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.Types
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbCounter
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbTtlSeconds
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.MappedProperty
import com.google.devtools.ksp.symbol.KSAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals

class MetadataExtractionTest {
    private val diagnostics = DiagnosticCollector()
    private val messages get() = diagnostics.diagnostics.map { it.message }

    private fun ttl(value: Any?) = fakeAnnotation(fakeAnnotationClass(DynamoDbTtlSeconds::class.qualifiedName!!), mapOf("value" to value))
    private val counter = fakeAnnotation(fakeAnnotationClass(DynamoDbCounter::class.qualifiedName!!))

    private fun prop(name: String, type: TypeRef, vararg annotations: KSAnnotation, ddbName: String = name) = SourceProperty(
        fakeProperty(name, fakeKsType(type.fullName), annotations.toList()),
        MappedProperty(name, ddbName, type),
    )

    @Test
    fun testTtlFieldsAreOrderedAndKeyedByKotlinName() {
        val properties = listOf(
            prop("id", Types.Kotlin.String),
            prop("expiresAt", Types.Kotlin.Long, ttl(3600L), ddbName = "expires_at"),
            prop("actuallyExpiresAt", Types.Kotlin.Long, ttl(7200L)),
        )

        val fields = ttlFields(properties, diagnostics)

        assertEquals(listOf("expiresAt" to 3600L, "actuallyExpiresAt" to 7200L), fields.toList())
        assertEquals(emptyList(), messages)
    }

    @Test
    fun testInvalidTtlsAreReportedWithDdbNameAndOmitted() {
        val nonConstant = prop("a", Types.Kotlin.Long, ttl(null), ddbName = "a_ddb")
        val negative = prop("b", Types.Kotlin.Long, ttl(-5L), ddbName = "b_ddb")

        val fields = ttlFields(listOf(nonConstant, negative), diagnostics)

        assertEquals(emptyMap(), fields)
        assertEquals(
            listOf(
                "@DynamoDbTtlSeconds annotation argument on property a_ddb could not be evaluated at compile time. Use a " +
                    "literal value like @DynamoDbTtlSeconds(3600) instead of expressions like " +
                    "@DynamoDbTtlSeconds(1.hours.inWholeSeconds).",
                "@DynamoDbTtlSeconds must be positive, got -5 seconds on property b_ddb",
            ),
            messages,
        )
        assertEquals(listOf(nonConstant.decl.asSymbolRef(), negative.decl.asSymbolRef()), diagnostics.diagnostics.map { it.symbol })
    }

    @Test
    fun testCounterFields() {
        val properties = listOf(
            prop("hits", Types.Kotlin.Long, counter),
            prop("name", Types.Kotlin.String),
            prop("views", Types.Kotlin.Int, counter),
        )

        assertEquals(listOf("hits", "views"), counterFields(properties, diagnostics).toList())
        assertEquals(emptyList(), messages)
    }

    @Test
    fun testInvalidCounterTypesAreReportedWithKotlinName() {
        val properties = listOf(
            prop("accessCount", Types.Kotlin.String, counter, ddbName = "access_count"),
            prop("nullableCount", Types.Kotlin.Int.copy(nullable = true), counter),
        )

        assertEquals(emptySet(), counterFields(properties, diagnostics))
        assertEquals(
            listOf(
                "Property 'accessCount' annotated with @DynamoDbCounter must be of type Int or Long, but was String",
                "Property 'nullableCount' annotated with @DynamoDbCounter must be of type Int or Long, but was Int",
            ),
            messages,
        )
    }
}
