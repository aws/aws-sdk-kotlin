/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyAttrSpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.model.itemOf
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
    override fun toFields(value: KeyType.Key1<K1>) = itemOf(
        attr1.toField(value.value1),
    )
}

internal data class KeySpec2Impl<K1, K2>(
    override val attr1: KeyAttrSpec<K1>,
    override val attr2: KeyAttrSpec<K2>,
) : KeySpec.Key2<K1, K2> {
    override fun toFields(value: KeyType.Key2<K1, K2>) = itemOf(
        attr1.toField(value.value1),
        attr2.toField(value.value2),
    )
}

internal data class KeySpec3Impl<K1, K2, K3>(
    override val attr1: KeyAttrSpec<K1>,
    override val attr2: KeyAttrSpec<K2>,
    override val attr3: KeyAttrSpec<K3>,
) : KeySpec.Key3<K1, K2, K3> {
    override fun toFields(value: KeyType.Key3<K1, K2, K3>) = itemOf(
        attr1.toField(value.value1),
        attr2.toField(value.value2),
        attr3.toField(value.value3),
    )
}

internal data class KeySpec4Impl<K1, K2, K3, K4>(
    override val attr1: KeyAttrSpec<K1>,
    override val attr2: KeyAttrSpec<K2>,
    override val attr3: KeyAttrSpec<K3>,
    override val attr4: KeyAttrSpec<K4>,
) : KeySpec.Key4<K1, K2, K3, K4> {
    override fun toFields(value: KeyType.Key4<K1, K2, K3, K4>) = itemOf(
        attr1.toField(value.value1),
        attr2.toField(value.value2),
        attr3.toField(value.value3),
        attr4.toField(value.value4),
    )
}

internal data class KeyAttrSpecImpl<K>(
    override val name: String,
    override val type: ScalarAttributeType,
    private val attributeValueFactory: (K) -> AttributeValue,
) : KeyAttrSpec<K> {
    override fun toField(value: K): Pair<String, AttributeValue> = name to attributeValueFactory(value)
}
