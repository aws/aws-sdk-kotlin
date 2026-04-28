/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

import aws.sdk.kotlin.hll.codegen.model.*
import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * A container object for various DynamoDbMapper [Type] instances
 */
@InternalSdkApi
public object MapperTypes {
    // Low-level types
    public val AttributeValue: TypeRef = TypeRef(MapperPkg.Ll.Model, "AttributeValue")
    public val AttributeMap: TypeRef = Types.Kotlin.stringMap(AttributeValue)
    public val ItemResponse: TypeRef = TypeRef(MapperPkg.Ll.Model, "ItemResponse")
    public val KeysAndAttributes: TypeRef = TypeRef(MapperPkg.Ll.Model, "KeysAndAttributes")
    public val TransactGetItem: TypeRef = TypeRef(MapperPkg.Ll.Model, "TransactGetItem")
    public val TransactWriteItem: TypeRef = TypeRef(MapperPkg.Ll.Model, "TransactWriteItem")
    public val WriteRequest: TypeRef = TypeRef(MapperPkg.Ll.Model, "WriteRequest")

    // High-level types
    public val DynamoDbMapper: TypeRef = TypeRef(MapperPkg.Hl.Base, "DynamoDbMapper")

    public object Annotations {
        public val ManualPagination: TypeRef = TypeRef(MapperPkg.Hl.Annotations, "ManualPagination")
    }

    public object Expressions {
        public val BooleanExpr: TypeRef = TypeRef(MapperPkg.Hl.Expressions.Base, "BooleanExpr")
        public val FilterDsl: TypeRef = TypeRef(MapperPkg.Hl.Expressions.Base, "FilterDsl")
        public val KeyFilter: TypeRef = TypeRef(MapperPkg.Hl.Expressions.Base, "KeyFilter")
        public val UpdateDsl: TypeRef = TypeRef(MapperPkg.Hl.Expressions.Base, "UpdateDsl")
        public val UpdateExpr: TypeRef = TypeRef(MapperPkg.Hl.Expressions.Base, "UpdateExpr")

        public object Internal {
            public val FilterDslImpl: TypeRef = TypeRef(MapperPkg.Hl.Expressions.Internal, "FilterDslImpl")
            public val ParameterizingExpressionVisitor: TypeRef =
                TypeRef(MapperPkg.Hl.Expressions.Internal, "ParameterizingExpressionVisitor")
            public val UpdateDslImpl: TypeRef = TypeRef(MapperPkg.Hl.Expressions.Internal, "UpdateDslImpl")
            public val toExpression: TypeRef = TypeRef(MapperPkg.Hl.Expressions.Internal, "toExpression")
        }
    }

    public object Internal {
        public val withWrappedClient: TypeRef = TypeRef(MapperPkg.Hl.Internal, "withWrappedClient")
    }

    public object Items {
        public fun itemSchema(typeVar: String): TypeRef = TypeRef(MapperPkg.Hl.Items, "ItemSchema", genericArgs = listOf(TypeVar(typeVar)))

        public fun itemSchemaPartitionKey(objectType: TypeRef, pkTypes: List<TypeRef>): TypeRef = TypeRef(
            MapperPkg.Hl.Items,
            "ItemSchema.PartitionKey",
            genericArgs = listOf(
                objectType,
                keyType(pkTypes),
            ),
        )

        public fun itemSchemaCompositeKey(objectType: TypeRef, pkTypes: List<TypeRef>, skTypes: List<TypeRef>): TypeRef = TypeRef(
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

        public val KeySpecByte: TypeRef = TypeRef(MapperPkg.Hl.Items, "KeySpec.byte")
        public val KeySpecByteArray: TypeRef = TypeRef(MapperPkg.Hl.Items, "KeySpec.byteArray")
        public val KeySpecInt: TypeRef = TypeRef(MapperPkg.Hl.Items, "KeySpec.int")
        public val KeySpecLong: TypeRef = TypeRef(MapperPkg.Hl.Items, "KeySpec.long")
        public val KeySpecShort: TypeRef = TypeRef(MapperPkg.Hl.Items, "KeySpec.short")
        public val KeySpecString: TypeRef = TypeRef(MapperPkg.Hl.Items, "KeySpec.string")

        public val KeySpecThenByte: TypeRef = TypeRef(MapperPkg.Hl.Items, "thenByte")
        public val KeySpecThenByteArray: TypeRef = TypeRef(MapperPkg.Hl.Items, "thenByteArray")
        public val KeySpecThenInt: TypeRef = TypeRef(MapperPkg.Hl.Items, "thenInt")
        public val KeySpecThenLong: TypeRef = TypeRef(MapperPkg.Hl.Items, "thenLong")
        public val KeySpecThenShort: TypeRef = TypeRef(MapperPkg.Hl.Items, "thenShort")
        public val KeySpecThenString: TypeRef = TypeRef(MapperPkg.Hl.Items, "thenString")

        public val KeyType: TypeRef = TypeRef(MapperPkg.Hl.Items, "KeyType")
        public val KeyTypeAsPK: TypeVar = TypeVar("PK", false, KeyType)
        public val KeyTypeAsSK: TypeVar = TypeVar("SK", false, KeyType)

        public fun keyType(keyTypes: List<TypeRef>): TypeRef {
            val keySize = keyTypes.size
            require(keySize in 1..4) { "KeyType subtypes must have between 1 and 4 keys, found $keySize" }
            return TypeRef(MapperPkg.Hl.Items, "KeyType.Key$keySize", keyTypes)
        }

        public val keysToItem: TypeRef = TypeRef(MapperPkg.Hl.Items, "keysToItem")
        public val itemToPk: TypeRef = TypeRef(MapperPkg.Hl.Items, "itemToPk")
        public val itemToSk: TypeRef = TypeRef(MapperPkg.Hl.Items, "itemToSk")

        public val ItemSchema: TypeRef = TypeRef(
            MapperPkg.Hl.Items,
            "ItemSchema",
            genericArgs = listOf(TypeVar.T),
        )

        public val ItemSchemaPartitionKey: TypeRef = TypeRef(
            MapperPkg.Hl.Items,
            "ItemSchema.PartitionKey",
            genericArgs = listOf(TypeVar.T, KeyTypeAsPK),
        )

        public val ItemSchemaCompositeKey: TypeRef = TypeRef(
            MapperPkg.Hl.Items,
            "ItemSchema.CompositeKey",
            genericArgs = listOf(TypeVar.T, KeyTypeAsPK, KeyTypeAsSK),
        )

        public val AttributeDescriptor: TypeRef = TypeRef(MapperPkg.Hl.Items, "AttributeDescriptor")

        public fun itemConverter(objectType: TypeRef): TypeRef = TypeRef(MapperPkg.Hl.Items, "ItemConverter", genericArgs = listOf(objectType))

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

        public val DynamoDbMapperSpec: TypeRef = TypeRef(MapperPkg.Hl.Model, "DynamoDbMapperSpec")
        public val Item: TypeRef = TypeRef(MapperPkg.Hl.Model, "Item")
        public val toItem: TypeRef = TypeRef(MapperPkg.Hl.Model, "toItem")
        public val SchemaAttributes: TypeRef = TypeRef(MapperPkg.Hl.Model, "SchemaAttributes")

        public val TablePartitionKeyGeneric: TypeRef = TypeRef(
            MapperPkg.Hl.Model,
            "Table.PartitionKey",
            genericArgs = listOf(TypeVar.T, Items.KeyTypeAsPK),
        )

        public val TableCompositeKeyGeneric: TypeRef = TypeRef(
            MapperPkg.Hl.Model,
            "Table.CompositeKey",
            genericArgs = listOf(TypeVar.T, Items.KeyTypeAsPK, Items.KeyTypeAsSK),
        )
    }

    public object Operations {
        public val BatchGetItemRequestTable: TypeRef = TypeRef(
            MapperPkg.Hl.Ops.Base,
            "BatchGetItemRequestTable",
            genericArgs = listOf(StarProjection),
        )

        public val BatchGetItemRequestTableDslPartitionKey: TypeRef = TypeRef(
            MapperPkg.Hl.Ops.Base,
            "BatchGetItemRequestTableDsl.PartitionKey",
            genericArgs = listOf(TypeVar.T, Items.KeyTypeAsPK),
        )

        public val BatchGetItemRequestTableDslCompositeKey: TypeRef = TypeRef(
            MapperPkg.Hl.Ops.Base,
            "BatchGetItemRequestTableDsl.CompositeKey",
            genericArgs = listOf(TypeVar.T, Items.KeyTypeAsPK, Items.KeyTypeAsSK),
        )

        public val BatchGetItemResponseTable: TypeRef = TypeRef(
            MapperPkg.Hl.Ops.Base,
            "BatchGetItemResponseTable",
            genericArgs = listOf(StarProjection),
        )

        public val BatchWriteItemRequestTable: TypeRef = TypeRef(
            MapperPkg.Hl.Ops.Base,
            "BatchWriteItemRequestTable",
            genericArgs = listOf(StarProjection),
        )

        public val BatchWriteItemRequestTableDslPartitionKey: TypeRef = TypeRef(
            MapperPkg.Hl.Ops.Base,
            "BatchWriteItemRequestTableDsl.PartitionKey",
            genericArgs = listOf(TypeVar.T, Items.KeyTypeAsPK),
        )

        public val BatchWriteItemRequestTableDslCompositeKey: TypeRef = TypeRef(
            MapperPkg.Hl.Ops.Base,
            "BatchWriteItemRequestTableDsl.CompositeKey",
            genericArgs = listOf(TypeVar.T, Items.KeyTypeAsPK, Items.KeyTypeAsSK),
        )

        public val BatchWriteItemResponseTable: TypeRef = TypeRef(
            MapperPkg.Hl.Ops.Base,
            "BatchWriteItemResponseTable",
            genericArgs = listOf(StarProjection),
        )

        public val TransactGetItemsRequestTable: TypeRef = TypeRef(
            MapperPkg.Hl.Ops.Base,
            "TransactGetItemsRequestTable",
            genericArgs = listOf(StarProjection),
        )

        public val TransactGetItemsRequestTableDslPartitionKey: TypeRef = TypeRef(
            MapperPkg.Hl.Ops.Base,
            "TransactGetItemsRequestTableDsl.PartitionKey",
            genericArgs = listOf(TypeVar.T, Items.KeyTypeAsPK),
        )

        public val TransactGetItemsRequestTableDslCompositeKey: TypeRef = TypeRef(
            MapperPkg.Hl.Ops.Base,
            "TransactGetItemsRequestTableDsl.CompositeKey",
            genericArgs = listOf(TypeVar.T, Items.KeyTypeAsPK, Items.KeyTypeAsSK),
        )

        public val TransactGetItemsResponseTable: TypeRef = TypeRef(
            MapperPkg.Hl.Ops.Base,
            "TransactGetItemsResponseTable",
            genericArgs = listOf(StarProjection),
        )

        public val TransactWriteItemsRequestTable: TypeRef = TypeRef(
            MapperPkg.Hl.Ops.Base,
            "TransactWriteItemsRequestTable",
            genericArgs = listOf(StarProjection),
        )

        public val TransactWriteItemsRequestTableDslPartitionKey: TypeRef = TypeRef(
            MapperPkg.Hl.Ops.Base,
            "TransactWriteItemsRequestTableDsl.PartitionKey",
            genericArgs = listOf(TypeVar.T, Items.KeyTypeAsPK),
        )

        public val TransactWriteItemsRequestTableDslCompositeKey: TypeRef = TypeRef(
            MapperPkg.Hl.Ops.Base,
            "TransactWriteItemsRequestTableDsl.CompositeKey",
            genericArgs = listOf(TypeVar.T, Items.KeyTypeAsPK, Items.KeyTypeAsSK),
        )

        public object Internal {
            public val BatchGetItemRequestTableDslPartitionKeyImpl: TypeRef = TypeRef(
                MapperPkg.Hl.Ops.Internal,
                "BatchGetItemRequestTableDslPartitionKeyImpl",
                genericArgs = listOf(TypeVar.T, Items.KeyTypeAsPK),
            )

            public val BatchGetItemRequestTableDslCompositeKeyImpl: TypeRef = TypeRef(
                MapperPkg.Hl.Ops.Internal,
                "BatchGetItemRequestTableDslCompositeKeyImpl",
                genericArgs = listOf(TypeVar.T, Items.KeyTypeAsPK, Items.KeyTypeAsSK),
            )

            public val BatchWriteItemRequestTableDslPartitionKeyImpl: TypeRef = TypeRef(
                MapperPkg.Hl.Ops.Internal,
                "BatchWriteItemRequestTableDslPartitionKeyImpl",
                genericArgs = listOf(TypeVar.T, Items.KeyTypeAsPK),
            )

            public val BatchWriteItemRequestTableDslCompositeKeyImpl: TypeRef = TypeRef(
                MapperPkg.Hl.Ops.Internal,
                "BatchWriteItemRequestTableDslCompositeKeyImpl",
                genericArgs = listOf(TypeVar.T, Items.KeyTypeAsPK, Items.KeyTypeAsSK),
            )

            public val TransactGetItemsRequestTableDslPartitionKeyImpl: TypeRef = TypeRef(
                MapperPkg.Hl.Ops.Internal,
                "TransactGetItemsRequestTableDslPartitionKeyImpl",
                genericArgs = listOf(TypeVar.T, Items.KeyTypeAsPK),
            )

            public val TransactGetItemsRequestTableDslCompositeKeyImpl: TypeRef = TypeRef(
                MapperPkg.Hl.Ops.Internal,
                "TransactGetItemsRequestTableDslCompositeKeyImpl",
                genericArgs = listOf(TypeVar.T, Items.KeyTypeAsPK, Items.KeyTypeAsSK),
            )

            public val TransactWriteItemsRequestTableDslPartitionKeyImpl: TypeRef = TypeRef(
                MapperPkg.Hl.Ops.Internal,
                "TransactWriteItemsRequestTableDslPartitionKeyImpl",
                genericArgs = listOf(TypeVar.T, Items.KeyTypeAsPK),
            )

            public val TransactWriteItemsRequestTableDslCompositeKeyImpl: TypeRef = TypeRef(
                MapperPkg.Hl.Ops.Internal,
                "TransactWriteItemsRequestTableDslCompositeKeyImpl",
                genericArgs = listOf(TypeVar.T, Items.KeyTypeAsPK, Items.KeyTypeAsSK),
            )
        }
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
