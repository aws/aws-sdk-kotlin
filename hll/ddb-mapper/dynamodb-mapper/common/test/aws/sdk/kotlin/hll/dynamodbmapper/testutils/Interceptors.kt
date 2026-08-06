/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.testutils

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.model.PersistenceSpec
import aws.sdk.kotlin.hll.dynamodbmapper.model.buildItem
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.MapperContext
import aws.sdk.kotlin.hll.mapping.core.converters.ConverterImpl
import aws.smithy.kotlin.runtime.collections.AttributesBuilder
import aws.smithy.kotlin.runtime.collections.attributesOf

fun testConverter(): ItemConverter<Any> = ConverterImpl(convertRight = { buildItem { } }, convertLeft = { "" })

private val testMapperContext = object : MapperContext<Any> {
    override val persistenceSpec: PersistenceSpec<Any>
        get() = error("Not needed for test")
    override val operation: String
        get() = error("Not needed for test")
}

fun testMapperContext(): MapperContext<Any> = testMapperContext

fun testSchema(attributesBlock: AttributesBuilder.() -> Unit) = ItemSchema(
    testConverter(),
    KeySpec.string("id"),
    attributesOf(attributesBlock),
)
