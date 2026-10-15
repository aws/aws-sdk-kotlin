/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.core.SymbolRef
import aws.sdk.kotlin.hll.codegen.ksp.asSymbolRef
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbCounter
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbTtlSeconds
import com.google.devtools.ksp.symbol.ClassKind
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaValidationTest {
    private val diagnostics = DiagnosticCollector()
    private val messages get() = diagnostics.diagnostics.map { it.message }

    private val pkAnnotation = fakeAnnotation(fakeAnnotationClass(DynamoDbPartitionKey::class.qualifiedName!!))
    private val ttlAnnotation = fakeAnnotation(fakeAnnotationClass(DynamoDbTtlSeconds::class.qualifiedName!!))
    private val counterAnnotation = fakeAnnotation(fakeAnnotationClass(DynamoDbCounter::class.qualifiedName!!))

    @Test
    fun testAnnotationClassIsRejected() {
        val decl = fakeClassDeclaration("org.example.MappableAnnotation", ClassKind.ANNOTATION_CLASS)
        assertFalse(validateNotAnnotationClass(decl, diagnostics))
        assertEquals(decl.asSymbolRef(), diagnostics.diagnostics.single().symbol)
        assertEquals(
            listOf(
                "@DynamoDbMappable cannot be applied to annotation classes (org.example.MappableAnnotation); it is only " +
                    "valid on regular or data classes.",
            ),
            messages,
        )
    }

    @Test
    fun testRegularClassIsAccepted() {
        assertTrue(validateNotAnnotationClass(fakeClassDeclaration("org.example.User"), diagnostics))
        assertEquals(emptyList(), messages)
    }

    @Test
    fun testKeyCardinality() {
        val decl = fakeClassDeclaration("org.example.User")

        validateTableItemKeys(decl, partitionKeyCount = 1, sortKeyCount = 0, diagnostics)
        validateTableItemKeys(decl, partitionKeyCount = 4, sortKeyCount = 4, diagnostics)
        assertEquals(emptyList(), messages)

        validateTableItemKeys(decl, partitionKeyCount = 0, sortKeyCount = 5, diagnostics)
        validateTableItemKeys(decl, partitionKeyCount = 5, sortKeyCount = 0, diagnostics)
        assertEquals(
            listOf(
                "Expected between 1 and 4 properties annotated with @DynamoDbPartitionKey, found 0",
                "Expected between 0 and 4 properties annotated with @DynamoDbSortKey, found 5",
                "Expected between 1 and 4 properties annotated with @DynamoDbPartitionKey, found 5",
            ),
            messages,
        )
        assertTrue(diagnostics.diagnostics.all { it.symbol == decl.asSymbolRef() })
    }

    @Test
    fun testNestedItemRejectsTableOnlyAnnotations() {
        val string = fakeKsType("kotlin.String")
        val properties = listOf(
            fakeProperty("id", string, listOf(pkAnnotation)),
            fakeProperty("name", string),
            fakeProperty("expiresAt", fakeKsType("kotlin.Long"), listOf(ttlAnnotation, counterAnnotation)),
        )

        validateNestedItem("Nested", properties, diagnostics)
        assertEquals(listOf(properties[0].asSymbolRef(), properties[2].asSymbolRef()), diagnostics.diagnostics.map { it.symbol })

        assertEquals(
            listOf(
                "Property 'id' on nested type 'Nested' is annotated with @DynamoDbPartitionKey. These annotations are " +
                    "only valid on a @DynamoDbItem table item, not on a nested @DynamoDbMappable type.",
                "Property 'expiresAt' on nested type 'Nested' is annotated with @DynamoDbTtlSeconds, @DynamoDbCounter. " +
                    "These annotations are only valid on a @DynamoDbItem table item, not on a nested @DynamoDbMappable " +
                    "type.",
            ),
            messages,
        )
    }

    @Test
    fun testCollectorFlushPreservesOrderAndSeverity() {
        val symbol = mockk<SymbolRef>()
        diagnostics.warn("first", symbol)
        diagnostics.error("second")
        diagnostics.warn("third")

        val logger = RecordingLogger()
        diagnostics.flushTo(logger)

        assertTrue(diagnostics.hasErrors)
        assertEquals(1, diagnostics.errorCount)
        assertEquals(
            listOf(
                RecordingLogger.Entry("warn", "first", symbol),
                RecordingLogger.Entry("error", "second", null),
                RecordingLogger.Entry("warn", "third", null),
            ),
            logger.entries,
        )
    }

    @Test
    fun testCollectorSnapshotIsImmutable() {
        val snapshot = diagnostics.diagnostics
        diagnostics.error("later")
        assertEquals(emptyList(), snapshot)
    }
}
