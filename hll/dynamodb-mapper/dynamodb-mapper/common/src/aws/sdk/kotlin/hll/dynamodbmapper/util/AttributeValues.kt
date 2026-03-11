/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.util

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

internal val NULL_ATTR = AttributeValue.Null(true)

internal fun av(value: Boolean?) = value?.let(AttributeValue::Bool) ?: NULL_ATTR
internal fun av(value: ByteArray?) = value?.let(AttributeValue::B) ?: NULL_ATTR

@JvmName("attrListAny")
internal fun av(value: List<Any?>?) = av(value?.map(::dynamicAv))

internal fun av(value: List<AttributeValue>?) = value?.let(AttributeValue::L) ?: NULL_ATTR

@JvmName("attrMapStringAny")
internal fun av(value: Map<String, Any?>?) = av(value?.mapValues { (_, v) -> dynamicAv(v) })

internal fun av(value: Map<String, AttributeValue>?) = value?.let(AttributeValue::M) ?: NULL_ATTR

@Suppress("UNUSED_PARAMETER")
internal fun av(value: Nothing?) = NULL_ATTR

internal fun av(value: Number?) = value?.let { AttributeValue.N(it.toString()) } ?: NULL_ATTR

@JvmName("attrSetByteArray")
internal fun av(value: Set<ByteArray>?) = value?.let { AttributeValue.Bs(it.toList()) } ?: NULL_ATTR

@JvmName("attrSetNumber")
internal fun av(value: Set<Number>?) = value?.let { AttributeValue.Ns(it.map(Number::toString)) } ?: NULL_ATTR

@JvmName("attrSetString")
internal fun av(value: Set<String>?) = value?.let { AttributeValue.Ss(it.toList()) } ?: NULL_ATTR

internal fun av(value: String?) = value?.let(AttributeValue::S) ?: NULL_ATTR

@JvmName("attrSetUByte")
internal fun av(value: Set<UByte>?) = value?.let { AttributeValue.Ns(it.map(UByte::toString)) } ?: NULL_ATTR

@JvmName("attrSetUInt")
internal fun av(value: Set<UInt>?) = value?.let { AttributeValue.Ns(it.map(UInt::toString)) } ?: NULL_ATTR

@JvmName("attrSetULong")
internal fun av(value: Set<ULong>?) = value?.let { AttributeValue.Ns(it.map(ULong::toString)) } ?: NULL_ATTR

@JvmName("attrSetUShort")
internal fun av(value: Set<UShort>?) = value?.let { AttributeValue.Ns(it.map(UShort::toString)) } ?: NULL_ATTR

internal fun av(value: UByte?) = value?.let { AttributeValue.N(it.toString()) } ?: NULL_ATTR
internal fun av(value: UInt?) = value?.let { AttributeValue.N(it.toString()) } ?: NULL_ATTR
internal fun av(value: ULong?) = value?.let { AttributeValue.N(it.toString()) } ?: NULL_ATTR
internal fun av(value: UShort?) = value?.let { AttributeValue.N(it.toString()) } ?: NULL_ATTR

@Suppress("UNCHECKED_CAST")
internal fun dynamicAv(value: Any?): AttributeValue = when (value) {
    null -> NULL_ATTR
    is AttributeValue -> value
    is Boolean -> av(value)
    is ByteArray -> av(value)
    is List<*> -> av(value)
    is Map<*, *> -> av(value as Map<String, Any?>)
    is Number -> av(value)
    is Set<*> -> when (val type = value.firstOrNull()) { // Attempt to determine set type by first element
        null -> av(value as Set<String>) // FIXME Is this a bad idea for the empty set case?
        is ByteArray -> av(value as Set<ByteArray>)
        is Number -> av(value as Set<Number>)
        is String -> av(value as Set<String>)
        is UByte -> av(value as Set<UByte>)
        is UInt -> av(value as Set<UInt>)
        is ULong -> av(value as Set<ULong>)
        is UShort -> av(value as Set<UShort>)
        else -> error("Unsupported set element type $type")
    }
    is String -> av(value)
    else -> error("Unsupported attribute value type ${value::class}")
}
