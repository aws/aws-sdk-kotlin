/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.model

import aws.sdk.kotlin.hll.codegen.util.requireAllDistinct
import aws.sdk.kotlin.runtime.InternalSdkApi
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import software.amazon.smithy.codegen.core.Symbol

/**
 * Describes a Kotlin data type
 */
@InternalSdkApi
public sealed interface Type {
    @InternalSdkApi
    public companion object {
        /**
         * Derives a [TypeRef] from a [KSClassDeclaration]
         */
        public fun from(ksClassDecl: KSClassDeclaration): TypeRef = from(ksClassDecl.asStarProjectedType())

        /**
         * Derives a [TypeRef] from a [KSTypeReference]
         */
        public fun from(ksTypeRef: KSTypeReference): TypeRef = from(ksTypeRef.resolve())

        /**
         * Derives a [TypeRef] from a [KSType]
         */
        public fun from(ksType: KSType): TypeRef {
            val name = ksType.declaration.qualifiedName!!
            return TypeRef(
                pkg = name.getQualifier(),
                shortName = name.getShortName(),
                genericArgs = ksType.arguments.map { from(it.type!!) },
                nullable = ksType.isMarkedNullable,
            )
        }

        /**
         * Derives a [TypeRef] from a Smithy [Symbol]
         */
        public fun from(symbol: Symbol, nullable: Boolean = false): TypeRef = TypeRef(
            pkg = symbol.namespace,
            shortName = symbol.name,
            nullable = nullable
        )
    }

    /**
     * Gets the short name (i.e., not including the Kotlin package) for this type
     */
    public val shortName: String

    /**
     * Indicates whether instances of this type allow nullable references
     */
    public val nullable: Boolean
}

/**
 * A reference to a specific, named type (e.g., [kotlin.String]).
 *
 * This type reference may have generic arguments, which are themselves instances of a [Type]. For instance, a [TypeRef]
 * representing [kotlin.collections.List] would have a single generic argument, which may either be a concrete [TypeRef]
 * itself (e.g., `List<String>`) or a generic [TypeVar] (e.g., `List<T>`).
 * @param pkg The Kotlin package for this type
 * @param shortName The short name (i.e., not including the Kotlin package) for this type
 * @param genericArgs Zero or more [Type] generic arguments to this type
 * @param nullable Indicates whether instances of this type allow nullable references
 */
@InternalSdkApi
public data class TypeRef(
    val pkg: String,
    override val shortName: String,
    val genericArgs: List<Type> = listOf(),
    override val nullable: Boolean = false,
) : Type {
    /**
     * The full name of this type, including the Kotlin package
     */
    val fullName: String = "$pkg.$shortName"

    /**
     * The base name of this type. In most cases, this will be the same as the short name, but for nested types, this
     * will only include the top-level name. For example, the base name of a type Foo.Bar.Baz is Foo.
     */
    val baseName: String = shortName.substringBefore(".")
}

/**
 * A generic type variable (e.g., `T`)
 * @param shortName The name of this type variable
 * @param nullable Indicates whether instances of this type allow nullable references
 */
@InternalSdkApi
public data class TypeVar(override val shortName: String, override val nullable: Boolean = false) : Type

/**
 * Derives a nullable [Type] equivalent for this type
 */
@InternalSdkApi
public fun Type.nullable(value: Boolean = true): Type = when {
    nullable == value -> this
    this is TypeRef -> copy(nullable = value)
    this is TypeVar -> copy(nullable = value)
    else -> error("Unknown Type ${this::class}") // Should be unreachable, only here to make compiler happy
}

/**
 * Derives a nullable [TypeRef] equivalent for this type reference
 */
@InternalSdkApi
public fun TypeRef.nullable(value: Boolean = true): TypeRef = when {
    nullable == value -> this
    else -> copy(nullable = value)
}

/**
 * Derives a nullable [TypeVar] equivalent for this type variable
 */
@InternalSdkApi
public fun TypeVar.nullable(value: Boolean = true): TypeVar = when {
    nullable == value -> this
    else -> copy(nullable = value)
}

/**
 * Gets a collection of all generic variables referenced by this [Type]
 */
@InternalSdkApi
public fun Type.genericVars(): List<TypeVar> = buildList {
    when (val type = this@genericVars) {
        is TypeVar -> add(type)
        is TypeRef -> type.genericArgs.flatMap { it.genericVars() }
    }
}

/**
 * Formats a collection of [TypeVar] into a Kotlin generics list (e.g., `<A, B, C>`)
 * @param postfix An optional string to include at the end of the generated string. This can be useful when the intended
 * destination for the string is codegen and additional spacing may be required.
 */
@InternalSdkApi
public fun List<TypeVar>.asParamsList(postfix: String = ""): String =
    takeUnless { isEmpty() }
        ?.map { it.shortName }
        ?.requireAllDistinct()
        ?.joinToString(", ", "<", ">$postfix")
        ?: ""

/**
 * Returns whether this [TypeRef] is generic for an [other]
 * For example, List<Boolean>.isGenericFor(List<Int>) returns true.
 */
@InternalSdkApi
public fun TypeRef.isGenericFor(other: TypeRef): Boolean = fullName == other.fullName
