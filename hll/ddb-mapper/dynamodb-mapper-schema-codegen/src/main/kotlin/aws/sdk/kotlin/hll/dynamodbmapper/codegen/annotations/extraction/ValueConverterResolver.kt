/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.core.SymbolRef
import aws.sdk.kotlin.hll.codegen.ksp.asSymbolRef
import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.Types
import aws.sdk.kotlin.hll.codegen.model.isGenericFor
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbAttributeConverter
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.MapKeyConverterSpec
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.ValueConverterSpec
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType

/**
 * Resolves the value converter for a mapped property. A property annotated with [DynamoDbAttributeConverter] uses that
 * converter and its type is never resolved (so it may be of any type, including one [resolveValueConverter] rejects).
 * Otherwise, resolution is driven by the property's type via [resolveValueConverter].
 * @return the resolved converter, or `null` if resolution failed and an error was reported to [diagnostics]
 */
internal fun resolvePropertyConverter(
    prop: KSPropertyDeclaration,
    ctx: RenderContext,
    diagnostics: DiagnosticCollector,
): ValueConverterSpec? {
    // KSP requires extra work to get a class argument out of an annotation, can't just use getAnnotationsByType
    // https://slack-chats.kotlinlang.org/t/8480301/hello-again-how-do-you-get-a-kclass-out-from-an-annotation-a
    val customConverterKsType = prop
        .annotations
        .singleOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == DynamoDbAttributeConverter::class.qualifiedName }
        ?.arguments
        ?.single()
        ?.value
        ?.let { it as KSType }

    return if (customConverterKsType != null) {
        ValueConverterSpec.Custom(
            converter = Type.from(customConverterKsType),
            needsCtorInvoke = !customConverterKsType.declaration.isObject,
        )
    } else {
        resolveValueConverter(prop.type.resolve(), ctx, diagnostics, prop.asSymbolRef())
    }
}

/**
 * Resolves the value converter for [ksType], recursing through nullability and collection type arguments. This is the
 * sole detection site for unsupported attribute types, map keys and set elements, and for cross-module nesting.
 * @param symbol The source symbol that unsupported-type errors are reported against (typically the property)
 * @return the resolved converter, or `null` if resolution failed and an error was reported to [diagnostics]
 */
internal fun resolveValueConverter(
    ksType: KSType,
    ctx: RenderContext,
    diagnostics: DiagnosticCollector,
    symbol: SymbolRef? = null,
): ValueConverterSpec? {
    val type = Type.from(ksType)
    fun recurse(t: KSType) = resolveValueConverter(t, ctx, diagnostics, symbol)

    return when {
        type.nullable -> recurse(ksType.makeNotNullable())?.let(ValueConverterSpec::Nullable)

        ksType.isEnum -> ValueConverterSpec.Enum(type)

        // Nested @DynamoDbMappable / @DynamoDbItem type: reference its generated value converter.
        ksType.isUserClass -> {
            val decl = ksType.declaration

            // Multi-module nesting is out of scope for now. Declarations originating from binary dependencies have
            // no containing source file; fail rather than emitting a reference that won't resolve.
            if (decl.containingFile == null) {
                diagnostics.error(
                    "Nested type '${decl.qualifiedName?.asString()}' is defined in another module. Nesting " +
                        "@DynamoDbMappable types across modules is not yet supported; the nested type must be " +
                        "declared in the same module as the type that references it.",
                    // A binary declaration has no source location, so prefer the referencing property
                    symbol ?: decl.asSymbolRef(),
                )
                return null
            }

            ValueConverterSpec.NestedRef(generatedValueConverter(decl, ctx))
        }

        type.isGenericFor(Types.Kotlin.Collections.List) ->
            recurse(ksType.singleArgument())?.let(ValueConverterSpec::ListConverter)

        type.isGenericFor(Types.Kotlin.Collections.Map) -> {
            check(ksType.arguments.size == 2) { "Expected map type ${ksType.declaration.qualifiedName?.asString()} to have 2 arguments, got ${ksType.arguments.size}" }

            val (keyType, valueType) = ksType.arguments.map {
                checkNotNull(it.type?.resolve()) { "Failed to resolved argument type for $it" }
            }

            val keyConverter = if (keyType.isEnum) {
                MapKeyConverterSpec.Enum(Type.from(keyType))
            } else {
                val scalarKeyConverter = keyType.mapKeyConverter
                if (scalarKeyConverter == null) {
                    diagnostics.error("Unsupported key type: ${Type.from(keyType)}", symbol)
                    return null
                }
                MapKeyConverterSpec.Scalar(scalarKeyConverter)
            }

            recurse(valueType)?.let { ValueConverterSpec.MapConverter(keyConverter, it) }
        }

        type.isGenericFor(Types.Kotlin.Collections.Set) -> {
            val setElementType = ksType.singleArgument()
            if (setElementType.isEnum) {
                ValueConverterSpec.EnumSetConverter(Type.from(setElementType))
            } else {
                val setConverter = setElementType.setValueConverter
                if (setConverter == null) {
                    diagnostics.error("Unsupported set element $setElementType", symbol)
                    return null
                }
                ValueConverterSpec.SetConverter(setConverter)
            }
        }

        else -> {
            val scalarConverter = type.scalarValueConverter
            if (scalarConverter == null) {
                diagnostics.error(unsupportedAttributeTypeMessage(ksType, type), symbol)
                return null
            }
            ValueConverterSpec.Scalar(scalarConverter)
        }
    }
}

private val TypeRef.scalarValueConverter: TypeRef?
    get() = when (this) {
        Types.Smithy.Instant -> MapperTypes.Values.SmithyTypes.InstantValueConverter
        Types.Smithy.Url -> MapperTypes.Values.SmithyTypes.UrlValueConverter
        Types.Smithy.Document -> MapperTypes.Values.SmithyTypes.DocumentValueConverter
        Types.Smithy.BigDecimal -> MapperTypes.Values.SmithyTypes.BigDecimalValueConverter
        Types.Smithy.BigInteger -> MapperTypes.Values.SmithyTypes.BigIntegerValueConverter

        Types.Java.BigDecimal -> MapperTypes.Values.JavaMath.BigDecimalValueConverter
        Types.Java.BigInteger -> MapperTypes.Values.JavaMath.BigIntegerValueConverter

        Types.Kotlin.Boolean -> MapperTypes.Values.Scalars.BooleanValueConverter
        Types.Kotlin.String -> MapperTypes.Values.Scalars.StringValueConverter
        Types.Kotlin.CharArray -> MapperTypes.Values.Scalars.CharArrayValueConverter
        Types.Kotlin.Char -> MapperTypes.Values.Scalars.CharValueConverter
        Types.Kotlin.Byte -> MapperTypes.Values.Scalars.ByteValueConverter
        Types.Kotlin.ByteArray -> MapperTypes.Values.Scalars.ByteArrayValueConverter
        Types.Kotlin.Short -> MapperTypes.Values.Scalars.ShortValueConverter
        Types.Kotlin.Int -> MapperTypes.Values.Scalars.IntValueConverter
        Types.Kotlin.Long -> MapperTypes.Values.Scalars.LongValueConverter
        Types.Kotlin.Double -> MapperTypes.Values.Scalars.DoubleValueConverter
        Types.Kotlin.Float -> MapperTypes.Values.Scalars.FloatValueConverter
        Types.Kotlin.UByte -> MapperTypes.Values.Scalars.UByteValueConverter
        Types.Kotlin.UInt -> MapperTypes.Values.Scalars.UIntValueConverter
        Types.Kotlin.UShort -> MapperTypes.Values.Scalars.UShortValueConverter
        Types.Kotlin.ULong -> MapperTypes.Values.Scalars.ULongValueConverter
        else -> null
    }

/**
 * Builds an error message for an unsupported attribute type. When the type is a user-defined class, hints that it
 * may need a [DynamoDbMappable] annotation to be usable as a nested attribute.
 */
private fun unsupportedAttributeTypeMessage(ksType: KSType, type: TypeRef): String {
    val decl = ksType.declaration
    val isUserDefinedClass = (decl as? KSClassDeclaration)?.classKind == ClassKind.CLASS
    return if (isUserDefinedClass) {
        "Unsupported attribute type $type. If '${decl.qualifiedName?.asString()}' should be stored as a nested " +
            "item, annotate it with @DynamoDbMappable (or @DynamoDbItem)."
    } else {
        "Unsupported attribute type $type"
    }
}

private val KSType.mapKeyConverter: TypeRef?
    get() = when (Type.from(this)) {
        // String
        Types.Kotlin.ByteArray -> MapperTypes.Values.Scalars.CharArrayToStringConverter
        Types.Kotlin.Char -> MapperTypes.Values.Scalars.CharToStringConverter
        Types.Kotlin.String -> MapperTypes.Values.Scalars.StringToStringConverter

        // Number
        Types.Kotlin.Byte -> MapperTypes.Values.Scalars.ByteToStringConverter
        Types.Kotlin.Double -> MapperTypes.Values.Scalars.DoubleToStringConverter
        Types.Kotlin.Float -> MapperTypes.Values.Scalars.FloatToStringConverter
        Types.Kotlin.Int -> MapperTypes.Values.Scalars.IntToStringConverter
        Types.Kotlin.Long -> MapperTypes.Values.Scalars.LongToStringConverter
        Types.Kotlin.Short -> MapperTypes.Values.Scalars.ShortToStringConverter
        Types.Kotlin.UByte -> MapperTypes.Values.Scalars.UByteToStringConverter
        Types.Kotlin.UInt -> MapperTypes.Values.Scalars.UIntToStringConverter
        Types.Kotlin.ULong -> MapperTypes.Values.Scalars.ULongToStringConverter
        Types.Kotlin.UShort -> MapperTypes.Values.Scalars.UShortToStringConverter

        // Arbitrary-precision number
        Types.Smithy.BigDecimal -> MapperTypes.Values.Scalars.BigDecimalToStringConverter
        Types.Smithy.BigInteger -> MapperTypes.Values.Scalars.BigIntegerToStringConverter
        Types.Java.BigDecimal -> MapperTypes.Values.JavaMath.BigDecimalToStringConverter
        Types.Java.BigInteger -> MapperTypes.Values.JavaMath.BigIntegerToStringConverter

        // Boolean
        Types.Kotlin.Boolean -> MapperTypes.Values.Scalars.BooleanToStringConverter
        else -> null
    }

private fun KSType.singleArgument(): KSType = checkNotNull(arguments.single().type?.resolve()) {
    "Failed to resolve single argument type for ${this.declaration.qualifiedName?.asString()}"
}

private val KSType.setValueConverter: TypeRef?
    get() = when (Type.from(this)) {
        Types.Kotlin.String -> MapperTypes.Values.Collections.StringSetValueConverter
        Types.Kotlin.ByteArray -> MapperTypes.Values.Collections.ByteArraySetValueConverter
        Types.Kotlin.Char -> MapperTypes.Values.Collections.CharSetValueConverter
        Types.Kotlin.CharArray -> MapperTypes.Values.Collections.CharArraySetValueConverter
        Types.Kotlin.Byte -> MapperTypes.Values.Collections.ByteSetValueConverter
        Types.Kotlin.Double -> MapperTypes.Values.Collections.DoubleSetValueConverter
        Types.Kotlin.Float -> MapperTypes.Values.Collections.FloatSetValueConverter
        Types.Kotlin.Int -> MapperTypes.Values.Collections.IntSetValueConverter
        Types.Kotlin.Long -> MapperTypes.Values.Collections.LongSetValueConverter
        Types.Kotlin.Short -> MapperTypes.Values.Collections.ShortSetValueConverter
        Types.Kotlin.UByte -> MapperTypes.Values.Collections.UByteSetValueConverter
        Types.Kotlin.UInt -> MapperTypes.Values.Collections.UIntSetValueConverter
        Types.Kotlin.ULong -> MapperTypes.Values.Collections.ULongSetValueConverter
        Types.Kotlin.UShort -> MapperTypes.Values.Collections.UShortSetValueConverter
        Types.Smithy.BigDecimal -> MapperTypes.Values.Collections.BigDecimalSetValueConverter
        Types.Smithy.BigInteger -> MapperTypes.Values.Collections.BigIntegerSetValueConverter
        Types.Java.BigDecimal -> MapperTypes.Values.JavaMath.BigDecimalSetValueConverter
        Types.Java.BigInteger -> MapperTypes.Values.JavaMath.BigIntegerSetValueConverter
        else -> null
    }

private val KSType.isEnum: Boolean
    get() = (declaration as? KSClassDeclaration)?.classKind == ClassKind.ENUM_CLASS

private val KSType.isUserClass: Boolean
    get() = declaration.isDynamoDbMappable

private val KSDeclaration.isObject: Boolean
    get() = (this as? KSClassDeclaration)?.classKind == ClassKind.OBJECT

/**
 * True if this declaration is annotated with a type that is (or is itself meta-annotated with) [DynamoDbMappable].
 * This covers both nested-only types annotated directly with [DynamoDbMappable] and table items annotated with
 * `@DynamoDbItem` (which is meta-annotated with [DynamoDbMappable]).
 */
@OptIn(KspExperimental::class)
internal val KSDeclaration.isDynamoDbMappable: Boolean
    get() = annotations.any { ann ->
        val decl = ann.annotationType.resolve().declaration
        decl.qualifiedName?.asString() == DynamoDbMappable::class.qualifiedName ||
            decl.isAnnotationPresent(DynamoDbMappable::class)
    }
