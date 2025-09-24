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

        public fun itemSchemaPartitionKey(objectType: TypeRef, pkType: TypeRef): TypeRef =
            TypeRef(MapperPkg.Hl.Items, "ItemSchema.PartitionKey", genericArgs = listOf(objectType, pkType))

        public fun itemSchemaCompositeKey(objectType: TypeRef, pkType: TypeRef, skType: TypeRef): TypeRef =
            TypeRef(MapperPkg.Hl.Items, "ItemSchema.CompositeKey", genericArgs = listOf(objectType, pkType, skType))

        public fun keySpec(keyType: TypeRef): TypeRef = TypeRef(MapperPkg.Hl.Items, "KeySpec", genericArgs = listOf(keyType))
        public val KeySpecByteArray: TypeRef = TypeRef(MapperPkg.Hl.Items, "KeySpec.ByteArray")
        public val KeySpecNumber: TypeRef = TypeRef(MapperPkg.Hl.Items, "KeySpec.Number")
        public val KeySpecString: TypeRef = TypeRef(MapperPkg.Hl.Items, "KeySpec.String")
        public val AttributeDescriptor: TypeRef = TypeRef(MapperPkg.Hl.Items, "AttributeDescriptor")

        public fun itemConverter(objectType: TypeRef): TypeRef =
            TypeRef(MapperPkg.Hl.Items, "ItemConverter", genericArgs = listOf(objectType))

        public val SimpleItemConverter: TypeRef = TypeRef(MapperPkg.Hl.Items, "SimpleItemConverter")
    }

    public object Model {
        public val intersectKeys: TypeRef = TypeRef(MapperPkg.Hl.Model, "intersectKeys")
        public fun tablePartitionKey(objectType: TypeRef, pkType: TypeRef): TypeRef = TypeRef(
            MapperPkg.Hl.Model,
            "Table.PartitionKey",
            genericArgs = listOf(objectType, pkType),
        )
        public fun tableCompositeKey(objectType: TypeRef, pkType: TypeRef, skType: TypeRef): TypeRef = TypeRef(
            MapperPkg.Hl.Model,
            "Table.CompositeKey",
            genericArgs = listOf(objectType, pkType, skType),
        )
        public val toItem: TypeRef = TypeRef(MapperPkg.Hl.Model, "toItem")
    }

    public object Values {
        public fun valueConverter(value: Type): TypeRef = TypeRef(MapperPkg.Hl.Values, "ValueConverter", genericArgs = listOf(value))
        public val ItemToValueConverter: TypeRef = TypeRef(MapperPkg.Hl.Values, "ItemToValueConverter")
        public val NullableConverter: TypeRef = TypeRef(MapperPkg.Hl.Values, "NullableConverter")

        public object Collections {
            public val ListConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "ListConverter")
            public val MapConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "MapConverter")

            public val StringSetConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "StringSetConverter")
            public val CharSetConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "CharSetConverter")
            public val CharArraySetConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "CharArraySetConverter")

            public val ByteSetConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "ByteSetConverter")
            public val DoubleSetConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "DoubleSetConverter")
            public val FloatSetConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "FloatSetConverter")
            public val IntSetConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "IntSetConverter")
            public val LongSetConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "LongSetConverter")
            public val ShortSetConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "ShortSetConverter")

            public val UByteSetConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "UByteSetConverter")
            public val UIntSetConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "UIntSetConverter")
            public val ULongSetConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "ULongSetConverter")
            public val UShortSetConverter: TypeRef = TypeRef(MapperPkg.Hl.CollectionValues, "UShortSetConverter")
        }

        public object Scalars {
            public fun enumConverter(enumType: Type): TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "EnumConverter", genericArgs = listOf(enumType))

            public val BooleanConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "BooleanConverter")
            public val StringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "StringConverter")
            public val CharConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "CharConverter")
            public val CharArrayConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "CharArrayConverter")

            public val ByteConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "ByteConverter")
            public val ByteArrayConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "ByteArrayConverter")
            public val DoubleConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "DoubleConverter")
            public val FloatConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "FloatConverter")
            public val IntConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "IntConverter")
            public val LongConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "LongConverter")
            public val ShortConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "ShortConverter")
            public val UByteConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "UByteConverter")
            public val UIntConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "UIntConverter")
            public val ULongConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "ULongConverter")
            public val UShortConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "UShortConverter")

            public val BooleanToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "BooleanToStringConverter")
            public val CharArrayToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "TextConverters.CharArrayToStringConverter")
            public val CharToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "TextConverters.CharToStringConverter")
            public val StringToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "TextConverters.StringToStringConverter")
            public val ByteToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.ByteToStringConverter")
            public val DoubleToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.DoubleToStringConverter")
            public val FloatToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.FloatToStringConverter")
            public val IntToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.IntToStringConverter")
            public val LongToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.LongToStringConverter")
            public val ShortToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.ShortToStringConverter")
            public val UByteToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.UByteToStringConverter")
            public val UIntToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.UIntToStringConverter")
            public val ULongToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.ULongToStringConverter")
            public val UShortToStringConverter: TypeRef = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.UShortToStringConverter")
        }

        public object SmithyTypes {
            public val DefaultInstantConverter: TypeRef = TypeRef(MapperPkg.Hl.SmithyTypeValues, "InstantConverter.Default")
            public val UrlConverter: TypeRef = TypeRef(MapperPkg.Hl.SmithyTypeValues, "UrlConverter")
            public val DefaultDocumentConverter: TypeRef = TypeRef(MapperPkg.Hl.SmithyTypeValues, "DocumentConverter.Default")
        }
    }

    public object PipelineImpl {
        public val HReqContextImpl: TypeRef = TypeRef(MapperPkg.Hl.PipelineImpl, "HReqContextImpl")
        public val MapperContextImpl: TypeRef = TypeRef(MapperPkg.Hl.PipelineImpl, "MapperContextImpl")
        public val Operation: TypeRef = TypeRef(MapperPkg.Hl.PipelineImpl, "Operation")
    }
}
