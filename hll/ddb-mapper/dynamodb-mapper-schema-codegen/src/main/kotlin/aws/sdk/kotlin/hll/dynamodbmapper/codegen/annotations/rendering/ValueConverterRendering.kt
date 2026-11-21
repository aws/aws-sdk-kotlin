/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.rendering

import aws.sdk.kotlin.hll.codegen.core.CodeGenerator
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.MapKeyConverterSpec
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.ValueConverterSpec
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes

/**
 * Renders the value converter described by [spec].
 *
 * Note: The value converter is rendered without a newline in order to support deep recursion. Callers are responsible
 * for adding a newline after the top-level invocation of this function.
 */
internal fun CodeGenerator.renderValueConverter(spec: ValueConverterSpec) {
    when (spec) {
        is ValueConverterSpec.Scalar -> writeInline("#T", spec.converter)

        is ValueConverterSpec.Nullable -> {
            writeInline("#T(", MapperTypes.Values.NullableValueConverter)
            renderValueConverter(spec.inner)
            writeInline(")")
        }

        is ValueConverterSpec.Enum -> writeInline("#T()", MapperTypes.Values.Scalars.enumValueConverter(spec.type))

        is ValueConverterSpec.ListConverter -> {
            writeInline("#T(", MapperTypes.Values.Collections.ListValueConverter)
            renderValueConverter(spec.element)
            writeInline(")")
        }

        is ValueConverterSpec.MapConverter -> {
            writeInline("#T(", MapperTypes.Values.Collections.MapValueConverter)
            renderMapKeyConverter(spec.key)
            writeInline(", ")
            renderValueConverter(spec.value)
            writeInline(")")
        }

        is ValueConverterSpec.SetConverter -> writeInline("#T", spec.converter)

        is ValueConverterSpec.EnumSetConverter ->
            writeInline("#T()", MapperTypes.Values.Collections.enumSetValueConverter(spec.type))

        is ValueConverterSpec.NestedRef -> writeInline("#T", spec.valueConverter)

        is ValueConverterSpec.Custom -> {
            writeInline("#T", spec.converter)
            if (spec.needsCtorInvoke) writeInline("()")
        }
    }
}

private fun CodeGenerator.renderMapKeyConverter(spec: MapKeyConverterSpec): Unit = when (spec) {
    is MapKeyConverterSpec.Scalar -> writeInline("#T", spec.converter)
    is MapKeyConverterSpec.Enum -> writeInline("#T()", MapperTypes.Values.Scalars.enumToStringConverter(spec.type))
}
