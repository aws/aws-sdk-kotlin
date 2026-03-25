/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.GenericsSet
import aws.sdk.kotlin.hll.codegen.model.Operation
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperPkg
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes

/**
 * Identifies a type in the `ItemSource<T>` hierarchy
 * @param hoistedFields Which fields should be hoisted from the low-level request type for this item source kind (e.g.,
 * for `TableSpec<T>` the `tableName` field should be hoisted)
 * @param parent The parent type of this type (if any)
 * @param isAbstract Indicates whether this item source kind is purely abstract and should not have an implementation
 * class (e.g., `ItemSource<T>` should be abstract and non-instantiable)
 * @param isSchemaless Indicates whether instances of this item source kind use a schema (e.g., a table or index) or not
 * (e.g., the `DynamoDbMapper` interface)
 */
internal enum class ItemSourceKind(
    val hoistedFields: List<String>,
    val parent: ItemSourceKind? = null,
    val isAbstract: Boolean = false,
    val isSchemaless: Boolean = false,
) {
    /**
     * Indicates the `ItemSource<T>` interface
     */
    ItemSource(listOf(), isAbstract = true),

    /**
     * Indicates the `Index<T>` interface
     */
    Index(listOf("indexName", "tableName"), ItemSource),

    /**
     * Indicates the `Table<T>` interface
     */
    Table(listOf("tableName"), ItemSource),

    /**
     * Indicates the `DynamoDbMapper` interface
     */
    DynamoDbMapper(listOf(), isSchemaless = true),
}

/**
 * Forms the [TypeRef] for this [ItemSourceKind]'s operations type (e.g., `ItemSourceOperations<T>` or
 * `TableOperations.PartitionKey<T, PK>`)
 * @param keyType The type of keys to include in the [TypeRef]
 */
internal fun ItemSourceKind.opsType(keyType: KeyProjectionType): TypeRef {
    val name = "${name}Operations${keyType.nameSuffix}"
    val generics = if (isSchemaless) GenericsSet() else keyType.typeVars
    return TypeRef(MapperPkg.Hl.Ops.Base, name, generics)
}

/**
 * Forms the [TypeRef] for this [ItemSourceKind]'s specification type (e.g., `ItemSourceSpec<T>` or
 * `TableSpec.PartitionKey<T, PK>`)
 * @param keyType The type of keys to include in the [TypeRef]
 */
internal fun ItemSourceKind.specType(
    keyType: KeyProjectionType,
): TypeRef = when (isSchemaless) {
    true -> MapperTypes.Model.DynamoDbMapperSpec
    else -> TypeRef(MapperPkg.Hl.Model, "${name}Spec${keyType.nameSuffix}", keyType.typeVars)
}

/**
 * Identifies the types of `ItemSource` on which an operation can be invoked (e.g., `Scan` can be invoked on a table,
 * index, or any generic item source, whereas `GetItem` can only be invoked on a table)
 */
internal val Operation.itemSourceKinds: Set<ItemSourceKind>
    get() = when (name) {
        "BatchGetItem", "BatchWriteItem" -> setOf(ItemSourceKind.DynamoDbMapper)
        "Query", "Scan" -> setOf(ItemSourceKind.ItemSource, ItemSourceKind.Index, ItemSourceKind.Table)
        else -> setOf(ItemSourceKind.Table)
    }
