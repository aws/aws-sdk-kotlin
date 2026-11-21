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
import com.google.devtools.ksp.symbol.KSAnnotation
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
 * Like [getMappedProperties], but each property is paired with its [effectiveAnnotations], so an annotation applied
 * to an overridden base property (e.g. `@DynamoDbIgnore` on an interface property) applies to the override.
 * @param inherit Whether to include annotations from overridden properties. Only members of heterogeneous families
 * inherit annotations, so existing classes keep their current mapping.
 */
internal fun KSClassDeclaration.getSourceProperties(inherit: Boolean): List<SourceProperty> = getAllProperties()
    .filterNot { it.modifiers.contains(Modifier.PRIVATE) }
    .map { decl -> decl to decl.effectiveAnnotations(inherit) }
    .filterNot { (_, annotations) -> annotations.hasAnnotation(DynamoDbIgnore::class.qualifiedName!!) }
    .map { (decl, annotations) -> SourceProperty(decl, decl.toMappedProperty(annotations), annotations) }
    .toList()

/**
 * This property's annotations, plus (when [inherit] is set) those of every property it overrides. The nearest
 * declaration wins when the same annotation appears more than once along the override chain.
 */
@OptIn(KspExperimental::class)
internal fun KSPropertyDeclaration.effectiveAnnotations(inherit: Boolean): List<KSAnnotation> {
    if (!inherit) return annotations.toList()
    return generateSequence(this) { it.findOverridee() as? KSPropertyDeclaration }
        .flatMap { it.annotations }
        .distinctBy { it.qualifiedName }
        .toList()
}

internal val KSAnnotation.qualifiedName: String?
    get() = annotationType.resolve().declaration.qualifiedName?.asString()

internal fun List<KSAnnotation>.find(qualifiedName: String): KSAnnotation? = firstOrNull { it.qualifiedName == qualifiedName }

internal fun List<KSAnnotation>.hasAnnotation(qualifiedName: String): Boolean = find(qualifiedName) != null

/**
 * Reads the argument named [name] from this annotation, or `null` if it isn't present
 */
internal fun KSAnnotation.argument(name: String): Any? = arguments.firstOrNull { it.name?.asString() == name }?.value

/**
 * Maps a KSP property to its IR form
 */
internal fun KSPropertyDeclaration.toMappedProperty(): MappedProperty = MappedProperty(
    name = name,
    ddbName = ddbName,
    type = Type.from(type),
)

/**
 * Maps a KSP property to its IR form, reading the attribute name from [annotations]
 */
internal fun KSPropertyDeclaration.toMappedProperty(annotations: List<KSAnnotation>): MappedProperty = MappedProperty(
    name = name,
    ddbName = annotations.find(DynamoDbAttribute::class.qualifiedName!!)?.argument("name") as? String ?: name,
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
