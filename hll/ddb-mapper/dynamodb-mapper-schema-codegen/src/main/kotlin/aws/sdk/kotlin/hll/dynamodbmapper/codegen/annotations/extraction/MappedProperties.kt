/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbIgnore
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSortKey
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.MappedProperty
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier

/**
 * Returns the properties of an annotated class that are mapped to DynamoDB attributes. Properties which are `private`
 * or annotated with [DynamoDbIgnore] are excluded.
 *
 * This is the single source of truth for deriving mappable fields from an annotated class and must be used everywhere
 * fields are enumerated (e.g. schema/converter extraction and nested-reference analysis) so that all consumers agree on
 * which properties participate in mapping.
 */
@OptIn(KspExperimental::class)
internal fun KSClassDeclaration.getMappedProperties(): List<KSPropertyDeclaration> = getAllProperties()
    .filterNot { it.modifiers.contains(Modifier.PRIVATE) || it.isAnnotationPresent(DynamoDbIgnore::class) }
    .toList()

/**
 * Maps a KSP property to its IR form
 */
internal fun KSPropertyDeclaration.toMappedProperty(): MappedProperty = MappedProperty(
    name = name,
    ddbName = ddbName,
    type = Type.from(type),
)

internal val KSPropertyDeclaration.name: String
    get() = simpleName.getShortName()

@OptIn(KspExperimental::class)
internal val KSPropertyDeclaration.ddbName: String
    get() = getAnnotationsByType(DynamoDbAttribute::class).singleOrNull()?.name ?: name

@OptIn(KspExperimental::class)
internal val KSPropertyDeclaration.isPk: Boolean
    get() = isAnnotationPresent(DynamoDbPartitionKey::class)

@OptIn(KspExperimental::class)
internal val KSPropertyDeclaration.isSk: Boolean
    get() = isAnnotationPresent(DynamoDbSortKey::class)
