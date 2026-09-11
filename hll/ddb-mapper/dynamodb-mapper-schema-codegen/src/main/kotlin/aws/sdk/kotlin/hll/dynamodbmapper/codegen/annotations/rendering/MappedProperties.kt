/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.rendering

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbIgnore
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier

/**
 * Returns the properties of an annotated class that are mapped to DynamoDB attributes. Properties which are `private`
 * or annotated with [DynamoDbIgnore] are excluded.
 *
 * This is the single source of truth for deriving mappable fields from an annotated class and must be used everywhere
 * fields are enumerated (e.g. schema/converter rendering and nested-reference analysis) so that all consumers agree on
 * which properties participate in mapping.
 */
@OptIn(KspExperimental::class)
internal fun KSClassDeclaration.getMappedProperties(): List<KSPropertyDeclaration> = getAllProperties()
    .filterNot { it.modifiers.contains(Modifier.PRIVATE) || it.isAnnotationPresent(DynamoDbIgnore::class) }
    .toList()
