/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.rendering.Visibility
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbCounter
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbTtlSeconds
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.DestinationPackage
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extractModel
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.ItemConverterSpec
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.MappedItem
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.ValueConverterSpec
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier
import io.mockk.every
import kotlin.test.Test
import kotlin.test.assertContains
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
        modifiers: Set<Modifier> = emptySet(),
        hasZeroArgConstructor: Boolean = true,
    ): KSClassDeclaration {
        val marker = fakeAnnotation(if (table) itemClass else mappableClass, itemConverterArgs)
        val decl = fakeClassDeclaration(fqName, classKind, annotations = listOf(marker), modifiers = modifiers, hasZeroArgConstructor = hasZeroArgConstructor)
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
    fun testMatchSourceVisibilityFollowsTheItemClass() {
        val publicItem = mappedClass("org.example.PublicUser", table = true, properties = { listOf(fakeProperty("id", string, listOf(pk))) })
        val internalItem = mappedClass(
            "org.example.InternalUser",
            table = true,
            properties = { listOf(fakeProperty("id", string, listOf(pk))) },
            modifiers = setOf(Modifier.INTERNAL),
        )

        val (publicBuilt, internalBuilt) = buildModel(listOf(publicItem, internalItem), testRenderContext(), diagnostics)

        assertEquals(emptyList(), messages)
        assertEquals(Visibility.PUBLIC, publicBuilt.visibility)
        assertEquals(Visibility.INTERNAL, internalBuilt.visibility)
    }

    @Test
    fun testMatchSourceVisibilityFollowsBuilderPropertyTypes() {
        val child = mappedClass(
            "org.example.Child",
            table = false,
            properties = { listOf(fakeProperty("name", string)) },
            modifiers = setOf(Modifier.INTERNAL),
        )
        fun parent(name: String, hasZeroArgConstructor: Boolean) = mappedClass(
            name,
            table = true,
            properties = { listOf(fakeProperty("id", string, listOf(pk)), fakeProperty("child", typeOf(child))) },
            hasZeroArgConstructor = hasZeroArgConstructor,
        )
        val withBuilder = parent("org.example.WithBuilder", hasZeroArgConstructor = false)
        val withoutBuilder = parent("org.example.WithoutBuilder", hasZeroArgConstructor = true)

        val built = buildModel(listOf(withBuilder, withoutBuilder, child), testRenderContext(), diagnostics)

        assertEquals(emptyList(), messages)
        // A builder exposes the internal property type, so everything generated for its class must be internal
        assertEquals(Visibility.INTERNAL, built.single { it.name == "WithBuilder" }.visibility)
        // Without a builder, the internal type is only referenced inside the converter's initializer
        assertEquals(Visibility.PUBLIC, built.single { it.name == "WithoutBuilder" }.visibility)
    }

    @Test
    fun testMatchSourceVisibilityFollowsUserProvidedItemConverter() {
        val internalConverter = fakeKsType(
            "org.example.CustomConverter",
            declaration = fakeClassDeclaration("org.example.CustomConverter", ClassKind.OBJECT, modifiers = setOf(Modifier.INTERNAL)),
        )
        val item = mappedClass(
            "org.example.CustomUser",
            table = true,
            properties = { listOf(fakeProperty("id", string, listOf(pk))) },
            itemConverterArgs = mapOf("converter" to internalConverter),
        )

        val built = buildModel(listOf(item), testRenderContext(), diagnostics).single()

        assertEquals(emptyList(), messages)
        assertEquals(Visibility.INTERNAL, built.visibility)
    }

    @Test
    fun testMatchSourceVisibilityIgnoresNestedItemUserProvidedConverter() {
        val internalConverter = fakeKsType(
            "org.example.AddressConverter",
            declaration = fakeClassDeclaration("org.example.AddressConverter", ClassKind.OBJECT, modifiers = setOf(Modifier.INTERNAL)),
        )
        val item = mappedClass(
            "org.example.Address",
            table = false,
            properties = { listOf(fakeProperty("street", string)) },
            itemConverterArgs = mapOf("converter" to internalConverter),
        )

        val built = buildModel(listOf(item), testRenderContext(), diagnostics).single()

        assertEquals(emptyList(), messages)
        assertEquals(Visibility.PUBLIC, built.visibility)
    }

    @Test
    fun testInaccessibleClassIsNotExtractedFurther() {
        val item = mappedClass(
            "org.example.Hidden",
            table = true,
            properties = { listOf(fakeProperty("thing", fakeKsType("org.example.PlainThing"))) },
            modifiers = setOf(Modifier.PRIVATE),
        )

        assertEquals(emptyList(), buildModel(listOf(item), testRenderContext(), diagnostics))
        assertEquals(1, messages.size)
        assertContains(messages.single(), "'org.example.Hidden' is private")
    }

    @Test
    fun testSimilarNamesDoNotCollideWhenTheirDeclarationsAreNotGenerated() {
        // UserValue's converter is named like User's value converter, which isn't generated since nothing references User
        val user = mappedClass("org.example.User", table = true, properties = { listOf(fakeProperty("id", string, listOf(pk))) })
        val userValue = mappedClass("org.example.UserValue", table = true, properties = { listOf(fakeProperty("id", string, listOf(pk))) })

        val built = buildModel(listOf(user, userValue), testRenderContext(), diagnostics)

        assertEquals(emptyList(), messages)
        assertEquals(listOf("User", "UserValue"), built.map { it.name })
    }

    @Test
    fun testGeneratedValueConverterCollidingWithGeneratedItemConverterIsReported() {
        val user = mappedClass("org.example.User", table = false, properties = { listOf(fakeProperty("name", string)) })
        val userValue = mappedClass("org.example.UserValue", table = true, properties = { listOf(fakeProperty("id", string, listOf(pk))) })
        val holder = mappedClass("org.example.Holder", table = true, properties = {
            listOf(fakeProperty("id", string, listOf(pk)), fakeProperty("user", typeOf(user)))
        })

        buildModel(listOf(user, userValue, holder), testRenderContext(), diagnostics)

        assertEquals(
            listOf(
                "'org.example.User' and 'org.example.UserValue' would both generate '$generatedPkg.UserValueConverter'. " +
                    "Rename one of them, or generate their code into different packages.",
                "'org.example.UserValue' and 'org.example.User' would both generate '$generatedPkg.UserValueConverter'. " +
                    "Rename one of them, or generate their code into different packages.",
            ),
            messages,
        )
    }

    @Test
    fun testSameNamesInOneDestinationPackageAreReportedOncePerClass() {
        val first = mappedClass("org.a.User", table = true, properties = { listOf(fakeProperty("id", string, listOf(pk))) })
        val second = mappedClass("org.b.User", table = true, properties = { listOf(fakeProperty("id", string, listOf(pk))) })
        val unrelated = mappedClass("org.a.Order", table = true, properties = { listOf(fakeProperty("id", string, listOf(pk))) })

        buildModel(listOf(first, second, unrelated), testRenderContext(destinationPackage = DestinationPackage.Absolute("gen")), diagnostics)

        assertEquals(
            listOf(
                "'org.a.User' and 'org.b.User' would both generate 'gen.UserConverter'. Rename one of them, or generate " +
                    "their code into different packages.",
                "'org.b.User' and 'org.a.User' would both generate 'gen.UserConverter'. Rename one of them, or generate " +
                    "their code into different packages.",
            ),
            messages,
        )
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
