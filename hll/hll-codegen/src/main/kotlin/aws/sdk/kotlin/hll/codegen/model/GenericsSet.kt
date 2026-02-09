/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.model

import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * Models a set of generic type variables used to parameterize a declaration such as a class or a function. Generics
 * sets only contain each named type variable one time but track the nullability of each variable and favor non-nullable
 * types over nullable ones.
 *
 * For instance:
 * * `<A?, B, C> + A? = <A?, B, C>` Adding a nullable variable to a set that already contains that nullable variable is
 *   a no-op.
 * * `<A?, B, C> + A = <A, B, C>` Adding a non-null variable to a set that already contains a nullable copy of that
 *   variable _replaces_ the existing copy with the non-null type.
 * * `<A, B, C> + A? = <A, B, C>` Adding a nullable variable to a set that already contains a non-null copy of that
 *   variable is a no-op.
 *
 * @param byName A map of [TypeVar] instances by their variable name
 */
@InternalSdkApi
public class GenericsSet internal constructor(internal val byName: Map<String, TypeVar>) : Set<TypeVar> {
    /**
     * Initializes a new generics set from the given collection of [TypeVar] elements. Variables with the same name will
     * be deduplicated, favoring non-null instances where possible.
     * @param elements The elements to use in this generics set
     */
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

    /**
     * Initializes a new generics set from the given [TypeVar] arguments. Variables with the same name will be
     * deduplicated, favoring non-null instances where possible.
     * @param elements The elements to use in this generics set
     */
    public constructor(vararg elements: TypeVar) : this(elements.toList())

    override val size: Int = byName.size
    override fun isEmpty(): Boolean = byName.isEmpty()
    override fun contains(element: TypeVar): Boolean = element.shortName in byName
    override fun iterator(): Iterator<TypeVar> = byName.values.iterator()

    override fun containsAll(elements: Collection<TypeVar>): Boolean =
        byName.keys.containsAll(elements.map { it.shortName })

    /**
     * Add the contents of this and another generics set, yielding a new generics set. Variables with the same name will
     * be deduplicated, favoring non-null instances where possible.
     * @param other The other set to add to this one
     */
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

    /**
     * Add the contents of this generics set and the given [TypeVar], yielding a new generics set. Variables with the
     * same name will be deduplicated, favoring non-null instances where possible.
     * @param other The new [TypeVar] to add
     */
    public operator fun plus(other: TypeVar): GenericsSet {
        val oldVar = byName[other.shortName]
        return when {
            oldVar == null || (oldVar.nullable && !other.nullable) -> GenericsSet(byName + (other.shortName to other))
            else -> this
        }
    }
}
