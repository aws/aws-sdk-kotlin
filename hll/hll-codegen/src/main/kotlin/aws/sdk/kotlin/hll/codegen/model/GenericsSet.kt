/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.model

import aws.sdk.kotlin.runtime.InternalSdkApi

@InternalSdkApi
public class GenericsSet internal constructor(internal val byName: Map<String, TypeVar>) : Set<TypeVar> {
    public constructor(elements: Collection<TypeVar>) : this(
        buildMap {
            elements.forEach { newVar ->
                val oldVar = get(newVar.shortName)
                if (oldVar == null || oldVar.nullable && !newVar.nullable) {
                    put(newVar.shortName, newVar)
                }
            }
        },
    )

    public constructor(vararg elements: TypeVar) : this(elements.toList())

    override val size: Int = byName.size
    override fun isEmpty(): Boolean = byName.isEmpty()
    override fun contains(element: TypeVar): Boolean = element.shortName in byName
    override fun iterator(): Iterator<TypeVar> = byName.values.iterator()

    override fun containsAll(elements: Collection<TypeVar>): Boolean =
        byName.keys.containsAll(elements.map { it.shortName })

    public operator fun plus(other: GenericsSet): GenericsSet =
        GenericsSet(
            buildMap {
                putAll(byName)
                other.forEach { newVar ->
                    val oldVar = byName[newVar.shortName]
                    if (oldVar == null || (oldVar.nullable && !newVar.nullable)) {
                        put(newVar.shortName, newVar)
                    }
                }
            },
        )

    public operator fun plus(other: TypeVar): GenericsSet {
        val oldVar = byName[other.shortName]
        return when {
            oldVar == null || (oldVar.nullable && !other.nullable) -> GenericsSet(byName + (other.shortName to other))
            else -> this
        }
    }
}
