package aws.sdk.kotlin.hll.dynamodbmapper.plugins

import aws.sdk.kotlin.hll.codegen.rendering.Visibility
import aws.sdk.kotlin.hll.codegen.rendering.Visibility.PUBLIC
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
     * Determines the visibility of code-generated classes / objects. Defaults to [Visibility.PUBLIC].
     */
    public var visibility: Visibility = Visibility.PUBLIC

    /**
     * Determines the package where code-generated classes / objects will be placed.
     * Defaults to [DestinationPackage.Relative] from the package of the class being processed, suffixed with `aws.sdk.kotlin.hll.dynamodbmapper.generatedschemas`.
     */
    public var destinationPackage: DestinationPackage = DestinationPackage.Relative()

    /**
     * Determines whether a `DynamoDbMapper.get<CLASS>Table` convenience extension function will be generated. Defaults to true.
     */
    public var generateGetTableExtension: Boolean = true
}
