/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyAttrSpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.itemOf
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ScalarAttributeType

internal val KeySpec<*>.attrs: List<KeyAttrSpec<*>>
    get() = when (this) {
        is KeySpec.Key1<*> -> listOf(attr1)
        is KeySpec.Key2<*, *> -> listOf(attr1, attr2)
        is KeySpec.Key3<*, *, *> -> listOf(attr1, attr2, attr3)
        is KeySpec.Key4<*, *, *, *> -> listOf(attr1, attr2, attr3, attr4)
    }

internal data class KeySpec1Impl<K1>(
    override val attr1: KeyAttrSpec<K1>,
) : KeySpec.Key1<K1> {
    override val converter: ItemConverter<KeyType.Key1<K1>> = Converter(
        { value ->
            itemOf(
                attr1.toField(value.value1),
            )
        },
        { item ->
            KeyType1Impl(
                attr1.fromFieldInItem(item),
            )
        },
    )
}

internal data class KeySpec2Impl<K1, K2>(
    override val attr1: KeyAttrSpec<K1>,
    override val attr2: KeyAttrSpec<K2>,
) : KeySpec.Key2<K1, K2> {
    override val converter: ItemConverter<KeyType.Key2<K1, K2>> = Converter(
        { value ->
            itemOf(
                attr1.toField(value.value1),
                attr2.toField(value.value2),
            )
        },
        { item ->
            KeyType2Impl(
                attr1.fromFieldInItem(item),
                attr2.fromFieldInItem(item),
            )
        }
    )
}

internal data class KeySpec3Impl<K1, K2, K3>(
    override val attr1: KeyAttrSpec<K1>,
    override val attr2: KeyAttrSpec<K2>,
    override val attr3: KeyAttrSpec<K3>,
) : KeySpec.Key3<K1, K2, K3> {
    override val converter: ItemConverter<KeyType.Key3<K1, K2, K3>> = Converter(
        { value ->
            itemOf(
                attr1.toField(value.value1),
                attr2.toField(value.value2),
                attr3.toField(value.value3),
            )
        },
        { item ->
            KeyType3Impl(
                attr1.fromFieldInItem(item),
                attr2.fromFieldInItem(item),
                attr3.fromFieldInItem(item),
            )
        },
    )
}

internal data class KeySpec4Impl<K1, K2, K3, K4>(
    override val attr1: KeyAttrSpec<K1>,
    override val attr2: KeyAttrSpec<K2>,
    override val attr3: KeyAttrSpec<K3>,
    override val attr4: KeyAttrSpec<K4>,
) : KeySpec.Key4<K1, K2, K3, K4> {
    override val converter: ItemConverter<KeyType.Key4<K1, K2, K3, K4>> = Converter(
        { value ->
            itemOf(
                attr1.toField(value.value1),
                attr2.toField(value.value2),
                attr3.toField(value.value3),
                attr4.toField(value.value4),
            )
        },
        { item ->
            KeyType4Impl(
                attr1.fromFieldInItem(item),
                attr2.fromFieldInItem(item),
                attr3.fromFieldInItem(item),
                attr4.fromFieldInItem(item),
            )
        },
    )
}

internal data class KeyAttrSpecImpl<K>(
    override val name: String,
    override val type: ScalarAttributeType,
    private val attributeValueFactory: (K) -> AttributeValue,
    private val keyTypeFactory: (AttributeValue) -> K,
) : KeyAttrSpec<K> {
    override fun fromFieldInItem(item: Item): K {
        val av = requireNotNull(item[name]) { "Cannot find required key attribute $name in item" }
        return keyTypeFactory(av)
    }

    override fun toField(value: K): Pair<String, AttributeValue> = name to attributeValueFactory(value)
}
