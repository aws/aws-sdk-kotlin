/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.rendering

import aws.sdk.kotlin.hll.codegen.core.ImportDirective
import aws.sdk.kotlin.hll.codegen.model.*
import aws.sdk.kotlin.hll.codegen.rendering.BuilderRenderer
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RendererBase
import aws.sdk.kotlin.hll.codegen.util.generatedAnnotation
import aws.sdk.kotlin.hll.codegen.util.plus
import aws.sdk.kotlin.hll.codegen.util.visibility
import aws.sdk.kotlin.hll.dynamodbmapper.*
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.GenerateBuilderClasses
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import aws.smithy.kotlin.codegen.core.RuntimeTypes
import aws.smithy.kotlin.runtime.collections.get
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.*

/**
 * Renders the classes and objects required to make a class usable with the DynamoDbMapper such as schemas, builders, and converters.
 * @param classDeclaration the [KSClassDeclaration] of the class
 * @param ctx the [RenderContext] of the renderer
 */
@OptIn(KspExperimental::class)
internal class SchemaRenderer(
    private val classDeclaration: KSClassDeclaration,
    private val ctx: RenderContext,
) : RendererBase(ctx, "${classDeclaration.qualifiedName!!.getShortName()}Schema") {
    private val className = classDeclaration.qualifiedName!!.getShortName()
    private val classStructure = Structure.from(classDeclaration)
    private val classType = Type.from(classDeclaration)

    private val builderName = "${className}Builder"
    private val converterName = "${className}Converter"
    private val schemaName = "${className}Schema"

    // The fully qualified name of the user-specified ItemConverter, if set
    private val userItemConverterFqn: String? = (
        classDeclaration
            .annotations
            .single { it.annotationType.resolve().declaration.qualifiedName?.asString() == DynamoDbItem::class.qualifiedName }
            .arguments.single().value as? KSType
        )?.declaration?.qualifiedName?.asString()
        ?.takeIf { it != "aws.sdk.kotlin.hll.mapping.core.converters.Converter" } // filter default values

    private val itemConverter: Type = userItemConverterFqn?.let {
        val pkg = it.substringBeforeLast(".")
        val shortName = it.removePrefix("$pkg.")
        TypeRef(pkg, shortName)
    } ?: TypeRef(ctx.pkg, converterName)

    private val properties = classDeclaration
        .getAllProperties()
        .filterNot { it.modifiers.contains(Modifier.PRIVATE) || it.isAnnotationPresent(DynamoDbIgnore::class) }
        .toList()

    private val partitionKeyProps = properties.filter { it.isPk }.also {
        require(it.size in 1..4) {
            "Expected between 1 and 4 properties annotated with @DynamoDbPartitionKey, found ${it.size}"
        }
    }

    private val sortKeyProps = properties.filter { it.isSk }.also {
        require(it.size in 0..4) {
            "Expected between 0 and 4 properties annotated with @DynamoDbSortKey, found ${it.size}"
        }
    }

    private val partitionKeyTypeRefs = partitionKeyProps.map { it.typeRef }
    private val sortKeyTypeRefs = sortKeyProps.map { it.typeRef }

    /**
     * Skip rendering a class builder if:
     *   - the user has configured GenerateBuilders to WHEN_REQUIRED (default value) AND
     *   - the class has all mutable members AND
     *   - the class has a zero-arg constructor
     */
    private val shouldRenderBuilder: Boolean = run {
        val alwaysGenerateBuilders = ctx.attributes[AnnotationsProcessorOptions.GenerateBuilderClassesAttribute] == GenerateBuilderClasses.ALWAYS
        val hasAllMutableMembers = properties.all { it.isMutable }
        val hasZeroArgConstructor = classDeclaration.getConstructors().any { constructor -> constructor.parameters.all { it.hasDefault } }

        !(!alwaysGenerateBuilders && hasAllMutableMembers && hasZeroArgConstructor)
    }

    override fun generate() {
        if (shouldRenderBuilder) {
            renderBuilder()
        }

        if (userItemConverterFqn == null) {
            renderItemConverter()
        }

        if (ctx.attributes[SchemaAttributes.ShouldRenderValueConverterAttribute]) {
            renderValueConverter()
        }

        renderSchema()

        if (ctx.attributes[AnnotationsProcessorOptions.GenerateGetTableMethodAttribute]) {
            renderGetTable()
        }
    }

    private fun renderBuilder() {
        val builderCtx = ctx.copy(
            attributes = ctx.attributes + (ModelAttributes.GeneratedApi to true),
        )
        val members = properties.map(Member::from).toSet()
        BuilderRenderer(builderCtx, this, classStructure, classType, members).render()
    }

    private fun renderItemConverter() {
        generatedAnnotation()
        withBlock(
            "#Lobject #L : #T by #T(",
            ")",
            ctx.attributes.visibility,
            converterName,
            MapperTypes.Items.itemConverter(classType),
            MapperTypes.Items.SimpleItemConverter,
        ) {
            if (shouldRenderBuilder) {
                write("builderFactory = ::#L,", builderName)
                write("build = #L::build,", builderName)
            } else {
                write("builderFactory = { $className() },")
                write("build = { this },")
            }

            withBlock("descriptors = arrayOf(", "),") {
                properties.forEach {
                    renderAttributeDescriptor(it)
                }
            }
        }
        blankLine()
    }

    /**
     * Render a [ValueConverter] for the current class by wrapping the generated/user-provided [ItemConverter]
     * with our [ItemToValueConverter]
     */
    private fun renderValueConverter() {
        // TODO Offer alternate serialization options besides AttributeValue.M?
        generatedAnnotation()
        write(
            "#Lval #L : #T = #T.#T(#T)",
            ctx.attributes.visibility,
            "${className}ValueConverter",
            MapperTypes.Values.valueConverter(classType),
            itemConverter,
            TypeRef("aws.sdk.kotlin.hll.mapping.core.converters", "andThenTo"),
            MapperTypes.Values.ItemToValueConverter,
        )
        blankLine()
    }

    private fun renderAttributeDescriptor(prop: KSPropertyDeclaration) {
        ctx.logger.info("Rendering an attribute descriptor for ${prop.simpleName.asString()}")
        withBlock("#T(", "),", MapperTypes.Items.AttributeDescriptor) {
            write("#S,", prop.ddbName) // key
            write("#L,", "$className::${prop.name}") // getter

            // setter
            if (shouldRenderBuilder) {
                write("#L,", "$builderName::${prop.name}::set")
            } else {
                write("#L,", "$className::${prop.name}::set")
            }

            // converter
            // KSP requires extra work to get a class argument out of an annotation, can't just use getAnnotationsByType
            // https://slack-chats.kotlinlang.org/t/8480301/hello-again-how-do-you-get-a-kclass-out-from-an-annotation-a
            val attributeValueConverterFqn = prop.annotations
                .singleOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == DynamoDbAttributeConverter::class.qualifiedName }
                ?.arguments
                ?.single()
                ?.value
                ?.let { it as? KSType }
                ?.declaration
                ?.qualifiedName
                ?.asString()

            attributeValueConverterFqn?.let {
                imports += ImportDirective(it)
                write("$it(),")
            } ?: run {
                renderValueConverter(prop.type.resolve())
                write(",")
            }
        }
    }

    /**
     * Renders a ValueConverter for the [ksType].
     *
     * Note: The ValueConverter(s) will be rendered without a newline in order to support deep recursion.
     * Callers are responsible for adding a newline after the top-level invocation of this function.
     */
    private fun renderValueConverter(ksType: KSType) {
        val type = Type.from(ksType)

        when {
            type.nullable -> {
                writeInline("#T(", MapperTypes.Values.NullableValueConverter)
                renderValueConverter(ksType.makeNotNullable())
                writeInline(")")
            }

            ksType.isEnum -> writeInline("#T()", MapperTypes.Values.Scalars.enumValueConverter(type))

            // FIXME Handle multi-module codegen rather than assuming nested classes will be in the same [ctx.pkg]
            ksType.isUserClass -> writeInline("#T", TypeRef(ctx.pkg, "${ksType.declaration.simpleName.asString()}ValueConverter"))

            type.isGenericFor(Types.Kotlin.Collections.List) -> {
                val listElementType = ksType.singleArgument()
                writeInline("#T(", MapperTypes.Values.Collections.ListValueConverter)
                renderValueConverter(listElementType)
                writeInline(")")
            }

            type.isGenericFor(Types.Kotlin.Collections.Map) -> {
                check(ksType.arguments.size == 2) { "Expected map type ${ksType.declaration.qualifiedName?.asString()} to have 2 arguments, got ${ksType.arguments.size}" }

                val (keyType, valueType) = ksType.arguments.map {
                    checkNotNull(it.type?.resolve()) { "Failed to resolved argument type for $it" }
                }

                writeInline("#T(#T, ", MapperTypes.Values.Collections.MapValueConverter, keyType.mapKeyConverter)
                renderValueConverter(valueType)
                writeInline(")")
            }

            type.isGenericFor(Types.Kotlin.Collections.Set) -> writeInline("#T", ksType.singleArgument().setValueConverter)

            else -> writeInline(
                "#T",
                when (type) {
                    Types.Smithy.Instant -> MapperTypes.Values.SmithyTypes.DefaultInstantValueConverter
                    Types.Smithy.Url -> MapperTypes.Values.SmithyTypes.UrlValueConverter
                    Types.Smithy.Document -> MapperTypes.Values.SmithyTypes.DefaultDocumentValueConverter

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
                    else -> error("Unsupported attribute type $type")
                },
            )
        }
    }

    private val KSType.mapKeyConverter: Type
        get() = when (val type = Type.from(this)) {
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

            // Boolean
            Types.Kotlin.Boolean -> MapperTypes.Values.Scalars.BooleanToStringConverter
            else -> error("Unsupported key type: $type")
        }

    private fun KSType.singleArgument(): KSType = checkNotNull(arguments.single().type?.resolve()) {
        "Failed to resolve single argument type for ${this.declaration.qualifiedName?.asString()}"
    }

    private val KSType.setValueConverter: Type
        get() = when (Type.from(this)) {
            Types.Kotlin.String -> MapperTypes.Values.Collections.StringSetValueConverter
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
            else -> error("Unsupported set element $this")
        }

    private fun renderSchema() {
        val schemaType = if (sortKeyProps.isEmpty()) {
            MapperTypes.Items.itemSchemaPartitionKey(classType, partitionKeyTypeRefs)
        } else {
            MapperTypes.Items.itemSchemaCompositeKey(classType, partitionKeyTypeRefs, sortKeyTypeRefs)
        }

        generatedAnnotation()
        withBlock("#Lobject #L : #T {", "}", ctx.attributes.visibility, schemaName, schemaType) {
            write("override val converter: #1T = #1T", itemConverter)

            writeInline("override val partitionKey: #T = ", MapperTypes.Items.keySpec(partitionKeyTypeRefs))
            keySpecInstantiation(partitionKeyProps)
            newline()

            if (sortKeyProps.isNotEmpty()) {
                writeInline("override val sortKey: #T = ", MapperTypes.Items.keySpec(sortKeyTypeRefs))
                keySpecInstantiation(sortKeyProps)
                newline()
            }

            // Handle TTL annotations
            val ttlFields = properties.mapNotNull { prop ->
                prop.annotations
                    .singleOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == DynamoDbTtlSeconds::class.qualifiedName }
                    ?.let { annotation ->
                        val lifetime = annotation.arguments.single().value as? Long ?: error("@DynamoDbTtlSeconds annotation argument on property ${prop.ddbName} could not be evaluated at compile time. Use a literal value like @DynamoDbTtlSeconds(3600) instead of expressions like @DynamoDbTtlSeconds(1.hours.inWholeSeconds).")
                        require(lifetime > 0) { "@DynamoDbTtlSeconds must be positive, got $lifetime seconds on property ${prop.ddbName}" }
                        prop.simpleName.getShortName() to lifetime
                    }
            }

            // Handle Counter annotation
            val counterFields = properties.mapNotNull { prop ->
                prop.annotations
                    .singleOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == DynamoDbCounter::class.qualifiedName }
                    ?.let {
                        // Validate that counter properties are Int or Long
                        require(prop.typeRef == Types.Kotlin.Int || prop.typeRef == Types.Kotlin.Long) {
                            "Property '${prop.name}' annotated with @DynamoDbCounter must be of type Int or Long, but was ${prop.typeRef.shortName}"
                        }
                        prop.simpleName.getShortName()
                    }
            }.toSet()

            val hasAttributes = ttlFields.isNotEmpty() || counterFields.isNotEmpty()

            if (hasAttributes) {
                withBlock(
                    "override val attributes: #T = #T {",
                    "}",
                    Type.from(RuntimeTypes.Core.Collections.Attributes),
                    Type.from(RuntimeTypes.Core.Collections.attributesOf),
                ) {
                    if (ttlFields.isNotEmpty()) {
                        writeInline("#T.#L to #T(", MapperTypes.Model.SchemaAttributes, "TtlFields", Types.Kotlin.Collections.setOf)
                        ttlFields.forEachIndexed { index, (fieldName, lifetime) ->
                            if (index > 0) writeInline(", ")
                            writeInline("#T(#S, #LL)", Types.Kotlin.Pair, fieldName, lifetime)
                        }
                        write(")")
                    }
                    if (counterFields.isNotEmpty()) {
                        write(
                            "#T.#L to #T(#L)",
                            MapperTypes.Model.SchemaAttributes,
                            "CounterFields",
                            Types.Kotlin.Collections.setOf,
                            counterFields.joinToString(", ") { "\"$it\"" },
                        )
                    }
                }
            } else {
                write(
                    "override val attributes: #T = #T()",
                    Type.from(RuntimeTypes.Core.Collections.Attributes),
                    Type.from(RuntimeTypes.Core.Collections.emptyAttributes),
                )
            }
        }

        blankLine()
    }

    private fun keySpecInstantiation(keyProps: List<KSPropertyDeclaration>) {
        val first = keyProps.first()
        val rest = keyProps.drop(1)

        val firstTypeRef = when (first.typeRef) {
            Types.Kotlin.Byte -> MapperTypes.Items.KeySpecByte
            Types.Kotlin.ByteArray -> MapperTypes.Items.KeySpecByteArray
            Types.Kotlin.Int -> MapperTypes.Items.KeySpecInt
            Types.Kotlin.Long -> MapperTypes.Items.KeySpecLong
            Types.Kotlin.Short -> MapperTypes.Items.KeySpecShort
            Types.Kotlin.String -> MapperTypes.Items.KeySpecString
            else -> error("Unsupported key attribute type ${first.typeRef}")
        }

        writeInline("#T(#S)", firstTypeRef, first.ddbName)

        rest.forEach { prop ->
            val typeRef = when (prop.typeRef) {
                Types.Kotlin.Byte -> MapperTypes.Items.KeySpecThenByte
                Types.Kotlin.ByteArray -> MapperTypes.Items.KeySpecThenByteArray
                Types.Kotlin.Int -> MapperTypes.Items.KeySpecThenInt
                Types.Kotlin.Long -> MapperTypes.Items.KeySpecThenLong
                Types.Kotlin.Short -> MapperTypes.Items.KeySpecThenShort
                Types.Kotlin.String -> MapperTypes.Items.KeySpecThenString
                else -> error("Unsupported key attribute type ${prop.typeRef}")
            }

            writeInline(".#T(#S)", typeRef, prop.ddbName)
        }
    }

    private fun renderGetTable() {
        docs("Returns a reference to a table named [name] containing items representing [#T]", classType)

        val tableType = if (sortKeyProps.isEmpty()) {
            MapperTypes.Model.tablePartitionKey(classType, partitionKeyTypeRefs)
        } else {
            MapperTypes.Model.tableCompositeKey(classType, partitionKeyTypeRefs, sortKeyTypeRefs)
        }

        generatedAnnotation()
        val fnName = "get${className}Table"
        write(
            "#Lfun #T.#L(name: String): #T = #L(name, #L)",
            ctx.attributes.visibility,
            MapperTypes.DynamoDbMapper,
            fnName,
            tableType,
            "getTable",
            schemaName,
        )
    }
}

@OptIn(KspExperimental::class)
private val KSType.isUserClass: Boolean
    get() = declaration.isAnnotationPresent(DynamoDbItem::class)

private val KSPropertyDeclaration.typeName: String
    get() = checkNotNull(getter?.returnType?.resolve()?.declaration?.qualifiedName?.asString()) { "Failed to determine type name for $this" }

@OptIn(KspExperimental::class)
private val KSPropertyDeclaration.isPk: Boolean
    get() = isAnnotationPresent(DynamoDbPartitionKey::class)

@OptIn(KspExperimental::class)
private val KSPropertyDeclaration.isSk: Boolean
    get() = isAnnotationPresent(DynamoDbSortKey::class)

private val KSPropertyDeclaration.name: String
    get() = simpleName.getShortName()

private val KSPropertyDeclaration.typeRef: TypeRef
    get() = Type.from(type)

@OptIn(KspExperimental::class)
private val KSPropertyDeclaration.ddbName: String
    get() = getAnnotationsByType(DynamoDbAttribute::class).singleOrNull()?.name ?: name

private val KSType.isEnum: Boolean
    get() = (declaration as? KSClassDeclaration)?.classKind == ClassKind.ENUM_CLASS
