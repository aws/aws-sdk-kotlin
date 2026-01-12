/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.TypeVar
import aws.sdk.kotlin.hll.codegen.model.Types
import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * A container object for various DynamoDbMapper [Type] instances
 */
@InternalSdkApi
public object MapperTypes {
    // Low-level types
    public val AttributeValue: TypeRef = TypeRef(MapperPkg.Ll.Model, "AttributeValue")
    public val AttributeMap: TypeRef = Types.Kotlin.map(Types.Kotlin.String, AttributeValue)

    // High-level types
    public val DynamoDbMapper: TypeRef = TypeRef(MapperPkg.Hl.Base, "DynamoDbMapper")

    public object Annotations {
        public val ManualPagination: TypeRef = TypeRef(MapperPkg.Hl.Annotations, "ManualPagination")
    }

    public object Expressions {
        public val BooleanExpr: TypeRef = TypeRef(MapperPkg.Hl.Expressions.Base, "BooleanExpr")
        public val Filter: TypeRef = TypeRef(MapperPkg.Hl.Expressions.Base, "Filter")
        public val KeyFilter: TypeRef = TypeRef(MapperPkg.Hl.Expressions.Base, "KeyFilter")

        public object Internal {
            public val FilterImpl: TypeRef = TypeRef(MapperPkg.Hl.Expressions.Internal, "FilterImpl")
            public val ParameterizingExpressionVisitor: TypeRef =
                TypeRef(MapperPkg.Hl.Expressions.Internal, "ParameterizingExpressionVisitor")
            public val toExpression: TypeRef = TypeRef(MapperPkg.Hl.Expressions.Internal, "toExpression")
        }
    }

    public object Internal {
        public val withWrappedClient: TypeRef = TypeRef(MapperPkg.Hl.Internal, "withWrappedClient")
    }

    public object Items {
        public fun itemSchema(typeVar: String): TypeRef =
            TypeRef(MapperPkg.Hl.Items, "ItemSchema", genericArgs = listOf(TypeVar(typeVar)))

        public fun itemSchemaPartitionKey(objectType: TypeRef, pkTypes: List<TypeRef>): TypeRef =
            TypeRef(
                MapperPkg.Hl.Items,
                "ItemSchema.PartitionKey",
                genericArgs = listOf(
                    objectType,
                    keyType(pkTypes),
                ),
            )

        public fun itemSchemaCompositeKey(objectType: TypeRef, pkTypes: List<TypeRef>, skTypes: List<TypeRef>): TypeRef =
            TypeRef(
                MapperPkg.Hl.Items,
                "ItemSchema.CompositeKey",
                genericArgs = listOf(
                    objectType,
                    keyType(pkTypes),
                    keyType(skTypes),
                ),
            )

        public fun keySpec(keyTypes: List<TypeRef>): TypeRef {
            val keySize = keyTypes.size
            require(keySize in 1..4) { "KeySpec subtypes must have between 1 and 4 keys, found $keySize" }
            return TypeRef(MapperPkg.Hl.Items, "KeySpec.Key$keySize", keyTypes)
        }

        public val KeySpecByteArray: TypeRef = TypeRef(MapperPkg.Hl.Items, "KeySpec.byteArray")
        public fun keySpecNumber(numberTypeRef: TypeRef? = null): TypeRef = TypeRef(
            MapperPkg.Hl.Items,
            "KeySpec.number",
            genericArgs = listOfNotNull(numberTypeRef),
        )
        public val KeySpecString: TypeRef = TypeRef(MapperPkg.Hl.Items, "KeySpec.string")

        public val KeySpecThenByteArray: TypeRef = TypeRef(MapperPkg.Hl.Items, "thenByteArray")
        public fun keySpecThenNumber(numberTypeRef: TypeRef): TypeRef = TypeRef(
            MapperPkg.Hl.Items,
            "thenNumber",
            genericArgs = listOfNotNull(numberTypeRef),
        )
        public val KeySpecThenString: TypeRef = TypeRef(MapperPkg.Hl.Items, "thenString")

        public fun keyType(keyTypes: List<TypeRef>): TypeRef {
            val keySize = keyTypes.size
            require(keySize in 1..4) { "KeyType subtypes must have between 1 and 4 keys, found $keySize" }
            return TypeRef(MapperPkg.Hl.Items, "KeyType.Key$keySize", keyTypes)
        }

        public val AttributeDescriptor: TypeRef = TypeRef(MapperPkg.Hl.Items, "AttributeDescriptor")

        public fun itemConverter(objectType: TypeRef): TypeRef =
            TypeRef(MapperPkg.Hl.Items, "ItemConverter", genericArgs = listOf(objectType))

        public val SimpleItemConverter: TypeRef = TypeRef(MapperPkg.Hl.Items, "SimpleItemConverter")
    }

    public object Model {
        public val intersectKeys: TypeRef = TypeRef(MapperPkg.Hl.Model, "intersectKeys")

        public fun tablePartitionKey(objectType: TypeRef, pkTypes: List<TypeRef>): TypeRef = TypeRef(
            MapperPkg.Hl.Model,
            "Table.PartitionKey",
            genericArgs = listOf(objectType, Items.keyType(pkTypes)),
        )

        public fun tableCompositeKey(
            objectType: TypeRef,
            pkTypes: List<TypeRef>,
            skTypes: List<TypeRef>,
        ): TypeRef {
            require(pkTypes.size in 1..4) { "Partition keys must have between 1 and 4 attributes, found ${pkTypes.size}" }
            require(skTypes.size in 1..4) { "Sort keys must have between 1 and 4 attributes, found ${skTypes.size}" }

            return TypeRef(
                MapperPkg.Hl.Model,
                "Table.CompositeKey",
                genericArgs = listOf(objectType, Items.keyType(pkTypes), Items.keyType(skTypes)),
            )
        }

        public val toItem: TypeRef = TypeRef(MapperPkg.Hl.Model, "toItem")
        public val SchemaAttributes: TypeRef = TypeRef(MapperPkg.Hl.Model, "SchemaAttributes")
    }

    public object Values {
        public fun valueConverter(value: Type): TypeRef = TypeRef(MapperPkg.Hl.Values, "ValueConverter", genericArgs = listOf(value))
        public val ItemToValueConverter: TypeRef = TypeRef(MapperPkg.Hl.Values, "ItemToValueConverter")
        public val NullableValueConverter: TypeRef = TypeRef(MapperPkg.Hl.Values, "NullableValueConverter")

        public object Collections {
            public val ListValueConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "ListValueConverter")
            public val MapValueConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "MapValueConverter")

            public val StringSetValueConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "StringSetValueConverter")
            public val CharSetValueConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "CharSetValueConverter")
            public val CharArraySetValueConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "CharArraySetValueConverter")

            public val ByteSetValueConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "NumberSetValueConverters.Byte")
            public val DoubleSetValueConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "NumberSetValueConverters.Double")
            public val FloatSetValueConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "NumberSetValueConverters.Float")
            public val IntSetValueConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "NumberSetValueConverters.Int")
            public val LongSetValueConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "NumberSetValueConverters.Long")
            public val ShortSetValueConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "NumberSetValueConverters.Short")

            public val UByteSetValueConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "NumberSetValueConverters.UByte")
            public val UIntSetValueConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "NumberSetValueConverters.UInt")
            public val ULongSetValueConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "NumberSetValueConverters.ULong")
            public val UShortSetValueConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "NumberSetValueConverters.UShort")
        }

        public object Scalars {
            public fun enumValueConverter(enumType: Type): TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "EnumValueConverter", genericArgs = listOf(enumType))

            public val BooleanValueConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "BooleanValueConverter")
            public val ByteArrayValueConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "ByteArrayValueConverter")
            public val StringValueConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "StringValueConverter")
            public val CharValueConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "CharValueConverter")
            public val CharArrayValueConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "CharArrayValueConverter")

            public val ByteValueConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberValueConverters.Byte")
            public val DoubleValueConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberValueConverters.Double")
            public val FloatValueConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberValueConverters.Float")
            public val IntValueConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberValueConverters.Int")
            public val LongValueConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberValueConverters.Long")
            public val ShortValueConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberValueConverters.Short")
            public val UByteValueConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberValueConverters.UByte")
            public val UIntValueConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberValueConverters.UInt")
            public val ULongValueConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberValueConverters.ULong")
            public val UShortValueConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberValueConverters.UShort")

            public val BooleanToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "BooleanToStringConverter")
            public val CharArrayToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "TextConverters.CharArrayToString")
            public val CharToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "TextConverters.CharToString")
            public val StringToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "TextConverters.String")
            public val ByteToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberToStringConverters.Byte")
            public val DoubleToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberToStringConverters.Double")
            public val FloatToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberToStringConverters.Float")
            public val IntToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberToStringConverters.Int")
            public val LongToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberToStringConverters.Long")
            public val ShortToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberToStringConverters.Short")
            public val UByteToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberToStringConverters.UByte")
            public val UIntToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberToStringConverters.UInt")
            public val ULongToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberToStringConverters.ULong")
            public val UShortToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberToStringConverters.UShort")
        }

        public object SmithyTypes {
            public val DefaultInstantValueConverter: TypeRef = TypeRef(MapperPkg.Hl.SmithyTypeValues, "InstantValueConverter.Default")
            public val UrlValueConverter: TypeRef = TypeRef(MapperPkg.Hl.SmithyTypeValues, "UrlValueConverter")
            public val DefaultDocumentValueConverter: TypeRef = TypeRef(MapperPkg.Hl.SmithyTypeValues, "DocumentValueConverter.Default")
        }
    }

    public object PipelineImpl {
        public val HReqContextImpl: TypeRef = TypeRef(MapperPkg.Hl.PipelineImpl, "HReqContextImpl")
        public val MapperContextImpl: TypeRef = TypeRef(MapperPkg.Hl.PipelineImpl, "MapperContextImpl")
        public val Operation: TypeRef = TypeRef(MapperPkg.Hl.PipelineImpl, "Operation")
    }
}
