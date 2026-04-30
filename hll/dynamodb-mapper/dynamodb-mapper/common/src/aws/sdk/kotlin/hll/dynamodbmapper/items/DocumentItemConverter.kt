/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.toItem
import aws.sdk.kotlin.hll.dynamodbmapper.util.NULL_ATTR
import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.hll.mapping.core.converters.reversedBy
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.content.Document
import aws.smithy.kotlin.runtime.util.toNumber

private val docToItem = MonoConverter<Document, Item> {
    require(it is Document.Map) // only
    it.mapValues { (_, value) -> toAttributeValue(value) }.toItem()
}

private val itemToDoc = MonoConverter<Item, Document> {
    it
        .mapValues { (_, attr) -> fromAttributeValue(attr) }
        .let(Document::Map)
}

// FIXME Combine with DocumentValueConverter or refactor to commonize as much code as possible
public val DocumentItemConverter: ItemConverter<Document> = docToItem reversedBy itemToDoc

@OptIn(InternalApi::class)
private fun fromAttributeValue(attr: AttributeValue): Document? = when (attr) {
    is AttributeValue.Null -> null
    is AttributeValue.N -> Document.Number(attr.value.toNumber()!!) // FIXME need better toNumber logic
    is AttributeValue.S -> Document.String(attr.value)
    is AttributeValue.Bool -> Document.Boolean(attr.value)
    is AttributeValue.L -> Document.List(attr.value.map(::fromAttributeValue))
    is AttributeValue.M -> Document.Map(attr.value.mapValues { (_, nestedValue) -> fromAttributeValue(nestedValue) })
    else -> error("Documents do not support ${attr::class.qualifiedName}")
}

private fun toAttributeValue(value: Document?): AttributeValue = when (value) {
    null -> NULL_ATTR
    is Document.Number -> AttributeValue.N(value.value.toString())
    is Document.String -> AttributeValue.S(value.value)
    is Document.Boolean -> AttributeValue.Bool(value.value)
    is Document.List -> AttributeValue.L(value.value.map(::toAttributeValue))
    is Document.Map -> AttributeValue.M(value.mapValues { (_, nestedValue) -> toAttributeValue(nestedValue) })
}
