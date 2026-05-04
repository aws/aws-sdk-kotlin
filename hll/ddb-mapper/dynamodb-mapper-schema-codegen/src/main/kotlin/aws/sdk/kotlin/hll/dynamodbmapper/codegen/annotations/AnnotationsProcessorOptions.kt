/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations

import aws.smithy.kotlin.runtime.collections.AttributeKey

/**
 * Options for configuring annotation processor codegen
 */
public object AnnotationsProcessorOptions {
    /**
     * Determines when a builder class should be generated for user classes. Defaults to [GenerateBuilderClasses.WHEN_REQUIRED].
     * With this setting, builder classes will not be generated for user classes which consist of only public mutable members
     * and have a zero-arg constructor.
     */
    public val GenerateBuilderClassesAttribute: AttributeKey<GenerateBuilderClasses> = AttributeKey("GenerateBuilderClasses")

    /**
     * Determines the package where code-generated classes / objects will be placed.
     * Defaults to [DestinationPackage.Relative] from the package of the class being processed, suffixed with `dynamodbmapper.generatedschemas`.
     */
    public val DestinationPackageAttribute: AttributeKey<DestinationPackage> = AttributeKey("DestinationPackage")

    /**
     * Determines whether a `DynamoDbMapper.get<CLASS>Table` convenience extension function will be generated. Defaults to true.
     */
    public val GenerateGetTableMethodAttribute: AttributeKey<Boolean> = AttributeKey("GenerateGetTableMethod")
}

/**
 * Determines when a builder class should be generated for user classes. Defaults to "WHEN_REQUIRED".
 * With this setting, builder classes will not be generated for user classes which consist of only public mutable members
 * and have a zero-arg constructor.
 */
public enum class GenerateBuilderClasses {
    /**
     * Builders will be generated when a buildable structure cannot be inferred for a class (e.g., the class has immutable members or is missing a zero-arg constructor)
     */
    WHEN_REQUIRED,

    /**
     * Builders will always be generated
     */
    ALWAYS,
}

/**
 * Determines the package where code-generated classes / objects will be placed.
 * Defaults to [DestinationPackage.Relative] from the package of the class being processed, suffixed with `dynamodbmapper.generatedschemas`.
 */
public sealed class DestinationPackage {
    /**
     * The package where code-generated classes / objects will be placed.
     */
    public abstract val pkg: String

    /**
     * Constructs should be code-generated into a package relative to the class being processed. Defaults to `aws.sdk.kotlin.hll.dynamodbmapper.generatedschemas`.
     */
    public class Relative(override val pkg: String = "dynamodbmapper.generatedschemas") : DestinationPackage()

    /**
     * Constructs should be code-generated into an absolute package. Defaults to `aws.sdk.kotlin.hll.dynamodbmapper.generatedschemas`.
     */
    public class Absolute(override val pkg: String = "aws.sdk.kotlin.hll.dynamodbmapper.generatedschemas") : DestinationPackage()

    override fun toString(): String = when (this) {
        is Relative -> "relative=$pkg"
        is Absolute -> "absolute=$pkg"
    }

    public companion object {
        public fun fromString(value: String): DestinationPackage {
            val (type, pkg) = value.split("=")

            return when (type.lowercase()) {
                "relative" -> Relative(pkg)
                "absolute" -> Absolute(pkg)
                else -> throw IllegalStateException("Expected DestinationPackage type to be `relative` or `absolute`, got $type")
            }
        }
    }
}
