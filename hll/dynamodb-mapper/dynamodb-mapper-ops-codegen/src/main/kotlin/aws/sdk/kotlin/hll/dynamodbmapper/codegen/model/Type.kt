/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.util.Pkg
import com.google.devtools.ksp.symbol.KSTypeReference

/**
 * Describes a Kotlin data type
 */
sealed interface Type {
    companion object {
        /**
         * Derives a [TypeRef] from a [KSTypeReference]
         */
        fun from(ksTypeRef: KSTypeReference): TypeRef {
            val resolved = ksTypeRef.resolve()
            val name = resolved.declaration.qualifiedName!!
            return TypeRef(
                pkg = name.getQualifier(),
                shortName = name.getShortName(),
                genericArgs = resolved.arguments.map { from(it.type!!) },
                nullable = resolved.isMarkedNullable,
            )
        }

        /**
         * Creates a [TypeRef] for a generic [List]
         * @param element The type of elements in the list
         */
        fun list(element: Type) = TypeRef(Pkg.Kotlin.Collections, "List", listOf(element))

        /**
         * Creates a [TypeRef] for a named Kotlin type (e.g., `String`)
         */
        fun kotlin(name: String) = TypeRef(Pkg.Kotlin.Base, name)

        /**
         * Creates a [TypeRef] for a generic [Map]
         * @param key The type of keys in the map
         * @param value The type of values in the map
         */
        fun map(key: Type, value: Type) = TypeRef(Pkg.Kotlin.Collections, "Map", listOf(key, value))

        /**
         * Creates a [TypeRef] for a generic [Map] with [String] keys
         * @param value The type of values in the map
         */
        fun stringMap(value: Type) = map(Types.String, value)
    }

    /**
     * Gets the short name (i.e., not including the Kotlin package) for this type
     */
    val shortName: String

    /**
     * Indicates whether instances of this type allow nullable references
     */
    val nullable: Boolean
}

/**
 * A reference to a specific, named type (e.g., [kotlin.String]).
 *
 * This type reference may have generic arguments, which are themselves instances of a [Type]. For instance, a [TypeRef]
 * representing [kotlin.collections.List] would have a single generic argument, which may either be a concrete [TypeRef]
 * itself (e.g., `List<String>`) or a generic [TypeVar] (e.g., `List<T>`).
 * @param pkg The Kotlin package for this type
 * @param shortName The short name (i.e., not include the kotlin package) for this type
 * @param genericArgs Zero or more [Type] generic arguments to this type
 * @param nullable Indicates whether instances of this type allow nullable references
 */
data class TypeRef(
    val pkg: String,
    override val shortName: String,
    val genericArgs: List<Type> = listOf(),
    override val nullable: Boolean = false,
) : Type {
    /**
     * The full name of this type, including the Kotlin package
     */
    val fullName: String = "$pkg.$shortName"
}

/**
 * A generic type variable (e.g., `T`)
 * @param shortName The name of this type variable
 * @param nullable Indicates whether instances of this type allow nullable references
 */
data class TypeVar(override val shortName: String, override val nullable: Boolean = false) : Type

/**
 * Derives a nullable [Type] equivalent for this type
 */
fun Type.nullable() = when {
    nullable -> this
    this is TypeRef -> copy(nullable = true)
    this is TypeVar -> copy(nullable = true)
    else -> error("Unknown Type ${this::class}") // Should be unreachable, only here to make compiler happy
}

/**
 * A container/factory object for various [Type] instances
 */
object Types {
    // Kotlin standard types
    val String = TypeRef("kotlin", "String")
    val StringNullable = String.nullable()

    // Low-level types
    val AttributeValue = TypeRef(Pkg.Ll.Model, "AttributeValue")
    val AttributeMap = Type.map(String, AttributeValue)

    // High-level types
    val HReqContextImpl = TypeRef(Pkg.Hl.PipelineImpl, "HReqContextImpl")
    fun itemSchema(typeVar: String) = TypeRef(Pkg.Hl.Items, "ItemSchema", listOf(TypeVar(typeVar)))
    val MapperContextImpl = TypeRef(Pkg.Hl.PipelineImpl, "MapperContextImpl")
    val Operation = TypeRef(Pkg.Hl.PipelineImpl, "Operation")
    val toItem = TypeRef(Pkg.Hl.Model, "toItem")
}
