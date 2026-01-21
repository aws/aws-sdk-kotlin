/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.smithy.kotlin.runtime.config.EnvironmentSetting
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * This type specifies the behavior of each configuration file type.
 *
 * @property setting The [AwsSdkSetting] from which the file location can be determined.
 * @property lineParsers A list of line parsers associated with the configuration file type.
 *
 * Two files are used for SDK configuration: a configuration file and a credentials file. Both files are UTF-8 encoded
 * and support all SDK configuration parameters, but have slight format differences.
 *
 * File	            Default Location	            Location Environment Variable
 * ------------------------------------------------------------------------------
 * Configuration	~/.aws/config	                AWS_CONFIG_FILE
 * Credentials	    ~/.aws/credentials	            AWS_SHARED_CREDENTIALS_FILE
 */
internal enum class FileType(
    private val setting: EnvironmentSetting<String>,
    private val lineParsers: List<ParseFn>,
    private val pathSegments: List<String>,
) {
    CONFIGURATION(
        AwsSdkSetting.AwsConfigFile,
        listOf(::configurationSection, ::property, ::continuation, ::subProperty),
        listOf("~", ".aws", "config"),
    ),
    CREDENTIAL(
        AwsSdkSetting.AwsSharedCredentialsFile,
        listOf(::credentialProfile, ::property, ::continuation, ::subProperty),
        listOf("~", ".aws", "credentials"),
    ),
    ;

    /**
     * Determine the absolute path of the configuration file based on environment and policy
     * @return the absolute path of the configuration file. This does not imply the file exists or is otherwise valid
     */
    fun path(platform: PlatformProvider): String = setting.resolve(platform)?.trim() ?: pathSegments.joinToString(separator = platform.filePathSeparator)

    /**
     * Parse a line into a token. See [FileLine] extensions for the types of config elements a file can contain.
     *
     * The tokenizer is context-aware and will handle all the critical failure cases defined in the configuration-file
     * SEP (by returning null):
     * 1. profile definitions without brackets
     * 2. property definitions without an = sign
     * 3. property definitions defined before any profile definition
     * 4. property definitions without a value before the = sign
     * 5. property continuations defined before any property in the current profile
     * 6. lines that cannot be determined to be a profile definition, empty line, comment line, property definition or property continuation
     * 7. property continuations that follow an empty property definition that do not have an = sign
     * 8. property continuations that follow an empty property definition that do not have a value before the = sign
     *
     * The consumer of the result should fail parsing immediately if a null token is encountered.
     */
    fun tokenOf(input: FileLine, currentSection: Token.Section?, lastProperty: Token.Property?): Token = lineParsers.firstNotNullOfOrNull { parseFunction -> parseFunction(input, currentSection, lastProperty) }
        ?: throw AwsConfigParseException("Encountered unexpected token", input.lineNumber)
}
