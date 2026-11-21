/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbAttributeConverter
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbHeterogeneousAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbHeterogeneousType
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.ValueConverterSpec
import com.google.devtools.ksp.symbol.ClassKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FamiliesTest {
    private data class Reg(val name: String, val ancestors: Set<String> = emptySet())

    private fun order(vararg regs: Reg) = orderRegistrations(regs.toList(), Reg::name, Reg::ancestors).map { it.name }

    @Test
    fun testUnrelatedRegistrationsAreSortedByName() {
        assertEquals(listOf("a.Bike", "a.Car", "a.Truck"), order(Reg("a.Truck"), Reg("a.Car"), Reg("a.Bike")))
    }

    @Test
    fun testSubtypesPrecedeTheirSupertypes() {
        // Animal <- Dog <- Puppy; Cat is unrelated but sorts before Dog
        val ordered = order(
            Reg("a.Animal"),
            Reg("a.Dog", setOf("a.Animal")),
            Reg("a.Puppy", setOf("a.Dog", "a.Animal")),
            Reg("a.Cat", setOf("a.Animal")),
        )
        assertEquals(listOf("a.Cat", "a.Puppy", "a.Dog", "a.Animal"), ordered)
    }

    private val diagnostics = DiagnosticCollector()
    private val messages get() = diagnostics.diagnostics.map { it.message }

    private val familyAnnotationClass = fakeAnnotationClass(DynamoDbHeterogeneousType::class.qualifiedName!!)
    private val entities = fakeClassDeclaration(
        "org.example.Entities",
        ClassKind.OBJECT,
        annotations = listOf(fakeAnnotation(familyAnnotationClass, mapOf("typeAttribute" to "kind"))),
    )
    private val entitiesValueConverter = TypeRef("org.example.dynamodbmapper.generatedschemas", "EntitiesValueConverter")
    private val any = fakeKsType("kotlin.Any")

    private fun heterogeneousAttribute(family: com.google.devtools.ksp.symbol.KSClassDeclaration) = fakeAnnotation(
        fakeAnnotationClass(DynamoDbHeterogeneousAttribute::class.qualifiedName!!),
        mapOf("family" to fakeKsType(family.qualifiedName!!.asString(), declaration = family)),
    )

    private fun resolve(type: com.google.devtools.ksp.symbol.KSType, vararg annotations: com.google.devtools.ksp.symbol.KSAnnotation) = resolvePropertyConverter(fakeProperty("p", type, annotations.toList()), testRenderContext(), diagnostics)

    @Test
    fun testHeterogeneousAttributeReplacesEveryFamilyValue() {
        val listOfNullableAny = fakeKsType("kotlin.collections.List", args = listOf(fakeKsType("kotlin.Any", nullable = true)))

        assertEquals(ValueConverterSpec.NestedRef(entitiesValueConverter), resolve(any, heterogeneousAttribute(entities)))
        assertEquals(
            ValueConverterSpec.ListConverter(ValueConverterSpec.Nullable(ValueConverterSpec.NestedRef(entitiesValueConverter))),
            resolve(listOfNullableAny, heterogeneousAttribute(entities)),
        )
        assertEquals(emptyList(), messages)
    }

    @Test
    fun testHeterogeneousAttributeWithoutFamilyValueFails() {
        assertNull(resolve(fakeKsType("kotlin.String"), heterogeneousAttribute(entities)))
        assertEquals(
            listOf("Property 'p' is annotated with @DynamoDbHeterogeneousAttribute(org.example.Entities::class) but holds no value of type 'kotlin.Any'"),
            messages,
        )
    }

    @Test
    fun testHeterogeneousAttributeNamingNonFamilyFails() {
        val plain = fakeClassDeclaration("org.example.Plain")
        assertNull(resolve(any, heterogeneousAttribute(plain)))
        assertEquals(
            listOf("Family 'org.example.Plain' named by @DynamoDbHeterogeneousAttribute on property 'p' is not annotated with @DynamoDbHeterogeneousType"),
            messages,
        )
    }

    @Test
    fun testHeterogeneousAttributeWithAttributeConverterFails() {
        val converter = fakeAnnotation(
            fakeAnnotationClass(DynamoDbAttributeConverter::class.qualifiedName!!),
            mapOf("converter" to fakeKsType("org.example.SomeConverter")),
        )
        assertNull(resolve(any, heterogeneousAttribute(entities), converter))
        assertEquals(
            listOf("Property 'p' is annotated with both @DynamoDbHeterogeneousAttribute and @DynamoDbAttributeConverter; use only one"),
            messages,
        )
    }
}
