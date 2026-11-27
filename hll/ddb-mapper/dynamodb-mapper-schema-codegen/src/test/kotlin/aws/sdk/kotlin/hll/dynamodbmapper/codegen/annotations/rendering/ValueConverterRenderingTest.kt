/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.rendering

import aws.sdk.kotlin.hll.codegen.core.CodeGeneratorFactory
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.MapKeyConverterSpec
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.ValueConverterSpec
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import com.google.devtools.ksp.processing.CodeGenerator as KSCodeGenerator

/**
 * Renderer-only tests: specs are built by hand, so no KSP input is involved. KSP types appear only to capture the
 * generator's output through the real [CodeGeneratorFactory].
 */
class ValueConverterRenderingTest {
    private val animal = TypeRef("org.example", "Animal")

    /** Renders [spec] and returns just the converter expression (the line following the package/imports header). */
    private fun render(spec: ValueConverterSpec): String {
        val out = ByteArrayOutputStream()
        val ksCodeGenerator = mockk<KSCodeGenerator> {
            every { createNewFile(any(), any(), any(), any()) } returns out
        }
        val generator = CodeGeneratorFactory(ksCodeGenerator, mockk<KSPLogger>(relaxed = true), Dependencies(false))
            .generator("Test", "org.example.generated", "test")
        generator.renderValueConverter(spec)
        generator.persist()
        return out.toString().trimEnd().lines().last()
    }

    @Test
    fun testScalar() {
        assertEquals("NumberValueConverters.Int", render(ValueConverterSpec.Scalar(MapperTypes.Values.Scalars.IntValueConverter)))
    }

    @Test
    fun testNullableList() {
        assertEquals(
            "NullableValueConverter(ListValueConverter(StringValueConverter))",
            render(
                ValueConverterSpec.Nullable(
                    ValueConverterSpec.ListConverter(ValueConverterSpec.Scalar(MapperTypes.Values.Scalars.StringValueConverter)),
                ),
            ),
        )
    }

    @Test
    fun testEnum() {
        assertEquals("EnumValueConverter<Animal>()", render(ValueConverterSpec.Enum(animal)))
    }

    @Test
    fun testEnumKeyedMap() {
        assertEquals(
            "MapValueConverter(EnumToStringConverter<Animal>(), StringValueConverter)",
            render(
                ValueConverterSpec.MapConverter(
                    MapKeyConverterSpec.Enum(animal),
                    ValueConverterSpec.Scalar(MapperTypes.Values.Scalars.StringValueConverter),
                ),
            ),
        )
    }

    @Test
    fun testScalarKeyedMap() {
        assertEquals(
            "MapValueConverter(NumberToStringConverters.Int, StringValueConverter)",
            render(
                ValueConverterSpec.MapConverter(
                    MapKeyConverterSpec.Scalar(MapperTypes.Values.Scalars.IntToStringConverter),
                    ValueConverterSpec.Scalar(MapperTypes.Values.Scalars.StringValueConverter),
                ),
            ),
        )
    }

    @Test
    fun testSets() {
        assertEquals("StringSetValueConverter", render(ValueConverterSpec.SetConverter(MapperTypes.Values.Collections.StringSetValueConverter)))
        assertEquals("EnumSetValueConverter<Animal>()", render(ValueConverterSpec.EnumSetConverter(animal)))
    }

    @Test
    fun testNestedRef() {
        assertEquals(
            "NestedValueConverter",
            render(ValueConverterSpec.NestedRef(TypeRef("org.example.generated", "NestedValueConverter"))),
        )
    }

    @Test
    fun testCustomConverters() {
        val converter = TypeRef("org.example.generated", "MyConverter")
        assertEquals("MyConverter()", render(ValueConverterSpec.Custom(converter, needsCtorInvoke = true)))
        assertEquals("MyConverter", render(ValueConverterSpec.Custom(converter, needsCtorInvoke = false)))
    }
}
