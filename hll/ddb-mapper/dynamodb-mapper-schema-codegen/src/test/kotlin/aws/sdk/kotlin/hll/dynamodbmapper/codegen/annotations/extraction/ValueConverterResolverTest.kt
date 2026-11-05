/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.ksp.asSymbolRef
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbAttributeConverter
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.MapKeyConverterSpec
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.ValueConverterSpec
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ValueConverterResolverTest {
    private val string = fakeKsType("kotlin.String")
    private val int = fakeKsType("kotlin.Int")
    private val animal = fakeClassDeclaration("org.example.Animal", ClassKind.ENUM_CLASS)
    private val animalType = fakeKsType("org.example.Animal", declaration = animal)
    private val animalRef = TypeRef("org.example", "Animal")

    private val diagnostics = DiagnosticCollector()
    private fun resolve(type: KSType) = resolveValueConverter(type, testRenderContext(), diagnostics)
    private fun resolveProperty(prop: com.google.devtools.ksp.symbol.KSPropertyDeclaration) = resolvePropertyConverter(prop, testRenderContext(), diagnostics)

    private fun list(element: KSType) = fakeKsType("kotlin.collections.List", args = listOf(element))
    private fun set(element: KSType) = fakeKsType("kotlin.collections.Set", args = listOf(element))
    private fun map(key: KSType, value: KSType) = fakeKsType("kotlin.collections.Map", args = listOf(key, value))

    private val mappableAnnotationClass = fakeAnnotationClass(DynamoDbMappable::class.qualifiedName!!)
    private val itemAnnotationClass = fakeAnnotationClass(
        DynamoDbItem::class.qualifiedName!!,
        metaAnnotations = listOf(fakeAnnotation(mappableAnnotationClass)),
    )

    private fun nestedType(fqName: String, viaItem: Boolean = false, hasSourceFile: Boolean = true) = fakeKsType(
        fqName,
        declaration = fakeClassDeclaration(
            fqName,
            annotations = listOf(fakeAnnotation(if (viaItem) itemAnnotationClass else mappableAnnotationClass)),
            hasSourceFile = hasSourceFile,
        ),
    )

    @Test
    fun testScalar() {
        assertEquals(ValueConverterSpec.Scalar(MapperTypes.Values.Scalars.StringValueConverter), resolve(string))
    }

    @Test
    fun testNullable() {
        assertEquals(
            ValueConverterSpec.Nullable(ValueConverterSpec.Scalar(MapperTypes.Values.Scalars.IntValueConverter)),
            resolve(fakeKsType("kotlin.Int", nullable = true)),
        )
    }

    @Test
    fun testEnum() {
        assertEquals(ValueConverterSpec.Enum(animalRef), resolve(animalType))
    }

    @Test
    fun testScalarSet() {
        assertEquals(
            ValueConverterSpec.SetConverter(MapperTypes.Values.Collections.StringSetValueConverter),
            resolve(set(string)),
        )
    }

    @Test
    fun testEnumSet() {
        assertEquals(ValueConverterSpec.EnumSetConverter(animalRef), resolve(set(animalType)))
    }

    @Test
    fun testEnumKeyedMap() {
        assertEquals(
            ValueConverterSpec.MapConverter(
                MapKeyConverterSpec.Enum(animalRef),
                ValueConverterSpec.Scalar(MapperTypes.Values.Scalars.StringValueConverter),
            ),
            resolve(map(animalType, string)),
        )
    }

    @Test
    fun testScalarKeyedMap() {
        assertEquals(
            ValueConverterSpec.MapConverter(
                MapKeyConverterSpec.Scalar(MapperTypes.Values.Scalars.IntToStringConverter),
                ValueConverterSpec.Scalar(MapperTypes.Values.Scalars.StringValueConverter),
            ),
            resolve(map(int, string)),
        )
    }

    @Test
    fun testDeeplyNestedMappable() {
        // List<Map<String, Nested>> where Nested is a @DynamoDbItem (meta-annotated with @DynamoDbMappable)
        val nested = nestedType("org.example.Nested", viaItem = true)
        assertEquals(
            ValueConverterSpec.ListConverter(
                ValueConverterSpec.MapConverter(
                    MapKeyConverterSpec.Scalar(MapperTypes.Values.Scalars.StringToStringConverter),
                    ValueConverterSpec.NestedRef(TypeRef("org.example.dynamodbmapper.generatedschemas", "NestedValueConverter")),
                ),
            ),
            resolve(list(map(string, nested))),
        )
    }

    private fun singleError(): Diagnostic = diagnostics.diagnostics.single().also {
        assertEquals(DiagnosticSeverity.ERROR, it.severity)
    }

    @Test
    fun testCrossModuleNestingReportsError() {
        val remote = nestedType("org.other.Remote", hasSourceFile = false)
        assertNull(resolve(remote))

        val error = singleError()
        assertTrue(error.message.startsWith("Nested type 'org.other.Remote' is defined in another module."), error.message)
        // With no referencing property, the (binary) declaration is the only location available
        assertEquals(remote.declaration.asSymbolRef(), error.symbol)
    }

    @Test
    fun testCrossModuleNestingIsLocatedAtReferencingProperty() {
        val prop = fakeProperty("remote", list(nestedType("org.other.Remote", hasSourceFile = false)))
        assertNull(resolveProperty(prop))
        assertEquals(prop.asSymbolRef(), singleError().symbol)
    }

    @Test
    fun testUnsupportedSetElementReportsError() {
        assertNull(resolve(set(nestedType("org.example.Tag"))))
        assertEquals("Unsupported set element Tag", singleError().message)
    }

    @Test
    fun testUnsupportedMapKeyReportsError() {
        assertNull(resolve(map(list(string), string)))
        assertTrue(singleError().message.startsWith("Unsupported key type: "))
    }

    @Test
    fun testUnsupportedUserClassReportsHint() {
        assertNull(resolve(fakeKsType("org.example.PlainThing")))
        val message = singleError().message
        assertTrue(message.contains("If 'org.example.PlainThing' should be stored as a nested item"), message)
    }

    @Test
    fun testUnsupportedTypeInsideCollectionFailsWholeConverter() {
        // A failure deep inside the type tree must not yield a partial spec
        assertNull(resolve(list(map(string, fakeKsType("org.example.PlainThing")))))
        assertEquals(1, diagnostics.errorCount)
    }

    @Test
    fun testUnsupportedTypeErrorIsLocatedAtProperty() {
        val prop = fakeProperty("thing", fakeKsType("org.example.PlainThing"))
        assertNull(resolveProperty(prop))
        assertEquals(prop.asSymbolRef(), singleError().symbol)
    }

    @Test
    fun testCustomConverterSkipsTypeResolution() {
        val converterClass = fakeKsType("org.example.OccupationConverter")
        val converterAnnotation = fakeAnnotation(
            fakeAnnotationClass(DynamoDbAttributeConverter::class.qualifiedName!!),
            arguments = mapOf("converter" to converterClass),
        )
        // The property's type is unsupported (an unannotated class); with a custom converter it must never be resolved.
        val typeRef = mockk<com.google.devtools.ksp.symbol.KSTypeReference>()
        val prop = mockk<com.google.devtools.ksp.symbol.KSPropertyDeclaration> {
            every { annotations } answers { sequenceOf(converterAnnotation) }
            every { type } returns typeRef
        }

        assertEquals(
            ValueConverterSpec.Custom(TypeRef("org.example", "OccupationConverter"), needsCtorInvoke = true),
            resolveProperty(prop),
        )
        verify(exactly = 0) { typeRef.resolve() }
    }

    @Test
    fun testCustomObjectConverterIsNotInstantiated() {
        val converterObject = fakeKsType(
            "org.example.HealthcareConverter",
            declaration = fakeClassDeclaration("org.example.HealthcareConverter", ClassKind.OBJECT),
        )
        val prop = fakeProperty(
            "healthcare",
            fakeKsType("org.example.Healthcare"),
            annotations = listOf(
                fakeAnnotation(
                    fakeAnnotationClass(DynamoDbAttributeConverter::class.qualifiedName!!),
                    arguments = mapOf("converter" to converterObject),
                ),
            ),
        )

        assertEquals(
            ValueConverterSpec.Custom(TypeRef("org.example", "HealthcareConverter"), needsCtorInvoke = false),
            resolveProperty(prop),
        )
    }

    @Test
    fun testPropertyWithoutCustomConverterDelegates() {
        assertEquals(
            ValueConverterSpec.Scalar(MapperTypes.Values.Scalars.StringValueConverter),
            resolveProperty(fakeProperty("name", string)),
        )
    }
}
