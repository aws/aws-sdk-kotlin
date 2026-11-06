/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.dynamodbmapper.plugins

import aws.sdk.kotlin.hll.codegen.rendering.Visibility
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.DestinationPackage
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.GenerateBuilderClasses

internal const val SCHEMA_GENERATOR_PLUGIN_EXTENSION = "dynamoDbMapper"

public open class SchemaGeneratorPluginExtension {
    /**
     * Determines when a builder class should be generated for user classes. Defaults to "WHEN_REQUIRED".
     * With this setting, builder classes will not be generated for user classes which consist of only public mutable members
     * and have a zero-arg constructor.
     */
    public var generateBuilderClasses: GenerateBuilderClasses = GenerateBuilderClasses.WHEN_REQUIRED

    /**
     * Determines the visibility of code-generated classes / objects. Defaults to [Visibility.MATCH_SOURCE], which makes
     * the code generated for each annotated class `public` only if every user type its generated signatures expose is
     * effectively public: the class itself (or, for a heterogeneous family, its base type), the property types of its
     * generated builder, and, for a table item, its user-provided item converter. Otherwise, that code is `internal`.
     * Code generated for an ad-hoc family (named by an `object`) exposes only `Any`, so it is always `public`.
     */
    public var visibility: Visibility = Visibility.MATCH_SOURCE

    /**
     * Determines the package where code-generated classes / objects will be placed.
     * Defaults to [DestinationPackage.Relative] from the package of the class being processed, suffixed with `dynamodbmapper.generatedschemas`.
     */
    public var destinationPackage: DestinationPackage = DestinationPackage.Relative()

    /**
     * Determines whether a `DynamoDbMapper.get<CLASS>Table` convenience extension function will be generated. Defaults to true.
     */
    public var generateGetTableExtension: Boolean = true
}
