/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbCounter
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbTtlSeconds
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extractModel
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.ItemConverterSpec
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.MappedItem
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.ValueConverterSpec
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import io.mockk.every
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SchemaModelBuilderTest {
    private val diagnostics = DiagnosticCollector()
    private val messages get() = diagnostics.diagnostics.map { it.message }
    private val generatedPkg = "org.example.dynamodbmapper.generatedschemas"

    private val mappableClass = fakeAnnotationClass(DynamoDbMappable::class.qualifiedName!!)
    private val itemClass = fakeAnnotationClass(DynamoDbItem::class.qualifiedName!!, listOf(fakeAnnotation(mappableClass)))
    private val pk = fakeAnnotation(fakeAnnotationClass(DynamoDbPartitionKey::class.qualifiedName!!))
    private val counter = fakeAnnotation(fakeAnnotationClass(DynamoDbCounter::class.qualifiedName!!))
    private fun ttl(value: Long) = fakeAnnotation(fakeAnnotationClass(DynamoDbTtlSeconds::class.qualifiedName!!), mapOf("value" to value))

    private val string = fakeKsType("kotlin.String")

    private fun mappedClass(
        fqName: String,
        table: Boolean,
        properties: (KSClassDeclaration) -> List<KSPropertyDeclaration>,
        itemConverterArgs: Map<String, Any?> = emptyMap(),
        classKind: ClassKind = ClassKind.CLASS,
    ): KSClassDeclaration {
        val marker = fakeAnnotation(if (table) itemClass else mappableClass, itemConverterArgs)
        val decl = fakeClassDeclaration(fqName, classKind, annotations = listOf(marker))
        val props = properties(decl)
        every { decl.getAllProperties() } answers { props.asSequence() }
        return decl
    }

    private fun typeOf(decl: KSClassDeclaration) = fakeKsType(decl.qualifiedName!!.asString(), declaration = decl)

    @Test
    fun testReferencedTypeGetsSingleSourcedValueConverter() {
        val child = mappedClass("org.example.Child", table = false, properties = { listOf(fakeProperty("name", string)) })
        val parent = mappedClass("org.example.Parent", table = true, properties = {
            listOf(fakeProperty("id", string, listOf(pk)), fakeProperty("child", fakeKsType("kotlin.collections.List", args = listOf(typeOf(child)))))
        })

        val (parentItem, childItem) = buildModel(listOf(parent, child), testRenderContext(), diagnostics)

        assertEquals(emptyList(), messages)
        val childValueConverter = TypeRef(generatedPkg, "ChildValueConverter")
        assertEquals(childValueConverter, childItem.valueConverter)
        assertNull(parentItem.valueConverter)

        // The reference from Parent and Child's own declaration come from the same source
        val childAttribute = (parentItem.itemConverter as ItemConverterSpec.Generated).attributes.single { it.property.name == "child" }
        assertEquals(ValueConverterSpec.ListConverter(ValueConverterSpec.NestedRef(childValueConverter)), childAttribute.converter)

        assertEquals(MappedItem.Kind.Table, parentItem.kind)
        assertEquals(MappedItem.Kind.Nested, childItem.kind)
        assertEquals(true, parentItem.generateGetTable)
        assertEquals(false, childItem.generateGetTable)
        assertEquals(generatedPkg, parentItem.destinationPkg)
        assertEquals(TypeRef(generatedPkg, "ParentConverter"), parentItem.itemConverter.converter)
        assertEquals(listOf("id"), parentItem.metadata.partitionKeys.map { it.ddbName })

        // All members mutable + zero-arg constructor + WHEN_REQUIRED => no builder
        assertNull(parentItem.builder)
    }

    @Test
    fun testSelfReferenceIsRejectedAsCycle() {
        val node = mappedClass("org.example.TreeNode", table = false, properties = { self -> listOf(fakeProperty("next", typeOf(self))) })

        assertEquals(emptyList(), buildModel(listOf(node), testRenderContext(), diagnostics))
        assertEquals(
            listOf(
                "Cyclic nesting detected involving type 'org.example.TreeNode'. Nesting @DynamoDbItem/@DynamoDbMappable " +
                    "types in a reference cycle (including self-references) is not yet supported.",
            ),
            messages,
        )
    }

    @Test
    fun testUserProvidedItemConverterNeverResolvesPropertyTypes() {
        val converter = fakeKsType("org.example.CustomConverter")
        val item = mappedClass(
            "org.example.CustomUser",
            table = true,
            properties = { listOf(fakeProperty("id", string, listOf(pk)), fakeProperty("thing", fakeKsType("org.example.PlainThing"))) },
            itemConverterArgs = mapOf("converter" to converter),
        )

        val built = buildModel(listOf(item), testRenderContext(), diagnostics).single()

        assertEquals(emptyList(), messages)
        assertEquals(ItemConverterSpec.UserProvided(TypeRef("org.example", "CustomConverter")), built.itemConverter)
    }

    @Test
    fun testAllProblemsInOneClassAreReportedInOrder() {
        val item = mappedClass("org.example.Broken", table = true, properties = {
            listOf(
                fakeProperty("thing", fakeKsType("org.example.PlainThing")),
                fakeProperty("expiresAt", fakeKsType("kotlin.Long"), listOf(ttl(-5L))),
                fakeProperty("accessCount", string, listOf(counter)),
            )
        })

        assertEquals(emptyList(), buildModel(listOf(item), testRenderContext(), diagnostics))
        assertEquals(
            listOf(
                "Expected between 1 and 4 properties annotated with @DynamoDbPartitionKey, found 0",
                "Unsupported attribute type TypeRef(pkg=org.example, shortName=PlainThing, genericArgs=[], nullable=false). " +
                    "If 'org.example.PlainThing' should be stored as a nested item, annotate it with @DynamoDbMappable " +
                    "(or @DynamoDbItem).",
                "@DynamoDbTtlSeconds must be positive, got -5 seconds on property expiresAt",
                "Property 'accessCount' annotated with @DynamoDbCounter must be of type Int or Long, but was String",
            ),
            messages,
        )
    }

    @Test
    fun testAnnotationClassIsNotExtractedFurther() {
        val decl = mappedClass(
            "org.example.MappableAnnotation",
            table = false,
            classKind = ClassKind.ANNOTATION_CLASS,
            properties = { listOf(fakeProperty("thing", fakeKsType("org.example.PlainThing"))) },
        )

        assertEquals(emptyList(), buildModel(listOf(decl), testRenderContext(), diagnostics))
        assertEquals(1, diagnostics.errorCount) // no spurious resolver error for `thing`
    }

    @Test
    fun testDiagnosticsAreFlushedWhenExtractionThrows() {
        val cyclic = mappedClass("org.example.TreeNode", table = false, properties = { self -> listOf(fakeProperty("next", typeOf(self))) })
        val exploding = mappedClass("org.example.Exploding", table = false, properties = { listOf(fakeProperty("name", string)) })
        every { exploding.declarations } throws IllegalStateException("boom")

        val logger = RecordingLogger()
        assertFailsWith<IllegalStateException> { extractModel(listOf(cyclic, exploding), testRenderContext(logger)) }

        // The cycle error collected before the failure still reached the logger
        assertEquals(
            listOf("Cyclic nesting detected involving type 'org.example.TreeNode'."),
            logger.errors.map { it.message.substringBefore(" Nesting") },
        )
    }

    @Test
    fun testExtractModelReturnsNullOnErrors() {
        val node = mappedClass("org.example.TreeNode", table = false, properties = { self -> listOf(fakeProperty("next", typeOf(self))) })
        assertNull(extractModel(listOf(node), testRenderContext()))
    }
}
