/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.model

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.collections.MutableAttributes

/**
 * Identifies an invocation style for a DSL declaration. This style will be used when code-generating a usage of some
 * DSL implementation.
 */
@InternalSdkApi
public sealed interface DslInvocationStyle {
    @InternalSdkApi
    public companion object {
        /**
         * Indicates that the DSL declaration has a constructor that takes zero arguments
         */
        public val NoArgsConstructor: DslInvocationStyle = Constructor()
    }

    /**
     * The string which should appear after the DSL name to correctly invoke it. For instance, a string of `"()"` would
     * be appended to the DSL name (e.g., `MyDslImpl`) to form a complete invocation (e.g., `MyDslImpl()`).
     */
    public val invocationString: String

    /**
     * Indicates that the DSL is a single value, object, or other declaration which does not require any further
     * invocation syntax besides its name
     */
    public data object Singleton : DslInvocationStyle {
        override val invocationString: String = ""
    }

    /**
     * Indicates that the DSL is invoked with a constructor containing zero or more arguments
     * @param args The arguments to pass to the constructor
     */
    public data class Constructor(val args: List<String>) : DslInvocationStyle {
        /**
         * Instantiate a new [Constructor] style
         * @param args The arguments to pass to the constructor
         */
        public constructor(vararg args: String) : this(args.toList())

        override val invocationString: String = args.joinToString(", ", "(", ")")
    }
}

/**
 * Contains information about types relevant to generating DSL methods
 * @param interfaceType The interface type used as the receiver for a DSL block. This should generally be a `public`
 * type.
 * @param implType The implementation type used to actually invoke the DSL block. This should generally be an `internal`
 * type.
 * @param implInvocationStyle The style of invocation used to access the DSL implementation. Defaults to
 * [DslInvocationStyle.NoArgsConstructor] if unspecified.
 * @param implFinalizer Any code string necessary to "finalize" a DSL implementation type and retrieve the value
 * @param nameOverride An optional override for the name of the generated DSL method. If unspecified, this will default
 * to the name of the member itself.
 * @param dslMethodParams Any additional parameters which should be part of the DSL method signature
 */
@InternalSdkApi
public data class DslInfo(
    val interfaceType: TypeRef,
    val implType: TypeRef,
    val implInvocationStyle: DslInvocationStyle = DslInvocationStyle.NoArgsConstructor,
    val implFinalizer: String? = null,
    val nameOverride: String? = null,
    val dslMethodParams: List<Member> = listOf(),
)

/**
 * Gets or sets the list of [DslInfo] instances associated with this attribute collection
 */
@InternalSdkApi
public var MutableAttributes.dsls: List<DslInfo>
    get() = getOrNull(ModelAttributes.Dsls).orEmpty()
    set(value) = set(ModelAttributes.Dsls, value)

/**
 * Gets the list of [DslInfo] instances associated with this [Member]
 */
@InternalSdkApi
public val Member.dsls: List<DslInfo>
    get() = attributes.getOrNull(ModelAttributes.Dsls).orEmpty()
