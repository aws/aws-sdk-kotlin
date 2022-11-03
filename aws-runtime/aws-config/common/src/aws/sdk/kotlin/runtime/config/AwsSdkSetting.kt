/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.retries.RetryMode
import aws.smithy.kotlin.runtime.util.PlatformEnvironProvider

// NOTE: The JVM property names MUST match the ones defined in the Java SDK for any setting added.
// see: https://github.com/aws/aws-sdk-java-v2/blob/master/core/sdk-core/src/main/java/software/amazon/awssdk/core/SdkSystemSetting.java
// see: https://github.com/aws/aws-sdk-java-v2/blob/master/docs/LaunchChangelog.md#61-environment-variables-and-system-properties

/**
 * Settings to configure SDK runtime behavior
 */
@InternalSdkApi
public sealed class AwsSdkSetting<T>(
    /**
     * The name of the corresponding environment variable that configures the setting
     */
    public val environmentVariable: String,

    /**
     * The name of the corresponding JVM system property that configures the setting
     */
    public val jvmProperty: String,

    /**
     * The default value (if one exists)
     */
    public val defaultValue: T? = null,
) {
    /**
     * Configure the AWS access key ID.
     *
     * This value will not be ignored if the [AwsSecretAccessKey] is not specified.
     */
    public object AwsAccessKeyId : AwsSdkSetting<String>("AWS_ACCESS_KEY_ID", "aws.accessKeyId")

    /**
     * Configure the AWS secret access key.
     *
     * This value will not be ignored if the [AwsAccessKeyId] is not specified.
     */
    public object AwsSecretAccessKey : AwsSdkSetting<String>("AWS_SECRET_ACCESS_KEY", "aws.secretAccessKey")

    /**
     * Configure the AWS session token.
     */
    public object AwsSessionToken : AwsSdkSetting<String>("AWS_SESSION_TOKEN", "aws.sessionToken")

    /**
     * Configure the default region.
     */
    public object AwsRegion : AwsSdkSetting<String>("AWS_REGION", "aws.region")

    /**
     * Configure the default path to the shared config file.
     */
    public object AwsConfigFile : AwsSdkSetting<String>("AWS_CONFIG_FILE", "aws.configFile")

    /**
     * Configure the default path to the shared credentials profile file.
     */
    public object AwsSharedCredentialsFile :
        AwsSdkSetting<String>("AWS_SHARED_CREDENTIALS_FILE", "aws.sharedCredentialsFile")

    /**
     * The execution environment of the SDK user. This is automatically set in certain environments by the underlying AWS service.
     * For example, AWS Lambda will automatically specify a runtime indicating that the SDK is being used within Lambda.
     */
    public object AwsExecutionEnv : AwsSdkSetting<String>("AWS_EXECUTION_ENV", "aws.executionEnvironment")

    /**
     *  The name of the default profile that should be loaded from config
     */
    public object AwsProfile : AwsSdkSetting<String>("AWS_PROFILE", "aws.profile", "default")

    /**
     * Whether to load information such as credentials, regions from EC2 Metadata instance service.
     */
    public object AwsEc2MetadataDisabled : AwsSdkSetting<Boolean>("AWS_EC2_METADATA_DISABLED", "aws.disableEc2Metadata", false)

    /**
     * The EC2 instance metadata service endpoint.
     *
     * This allows a service running in EC2 to automatically load its credentials and region without needing to configure them
     * directly.
     */
    public object AwsEc2MetadataServiceEndpoint : AwsSdkSetting<String>("AWS_EC2_METADATA_SERVICE_ENDPOINT", "aws.ec2MetadataServiceEndpoint")

    /**
     * The endpoint mode to use when connecting to the EC2 metadata service endpoint
     */
    public object AwsEc2MetadataServiceEndpointMode : AwsSdkSetting<String>("AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE", "aws.ec2MetadataServiceEndpointMode")

    // TODO - Currently env/system properties around role ARN, role session name, etc are restricted to the STS web identity provider.
    //        They should be applied more broadly but this needs fleshed out across AWS SDKs before we can do so.

    /**
     * The ARN of a role to assume
     */
    public object AwsRoleArn : AwsSdkSetting<String>("AWS_ROLE_ARN", "aws.roleArn")

    /**
     * The session name to use for assumed roles
     */
    public object AwsRoleSessionName : AwsSdkSetting<String>("AWS_ROLE_SESSION_NAME", "aws.roleSessionName")

    /**
     * The AWS web identity token file path
     */
    public object AwsWebIdentityTokenFile : AwsSdkSetting<String>("AWS_WEB_IDENTITY_TOKEN_FILE", "aws.webIdentityTokenFile")

    /**
     * The elastic container metadata service path that should be called by the [aws.sdk.kotlin.runtime.auth.credentials.EcsCredentialsProvider]
     * when loading credentials from the container metadata service.
     */
    public object AwsContainerCredentialsRelativeUri : AwsSdkSetting<String> ("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI", "aws.containerCredentialsPath", null)

    /**
     * The full URI path to a localhost metadata service to be used. This is ignored if
     * [AwsContainerCredentialsRelativeUri] is set.
     */
    public object AwsContainerCredentialsFullUri : AwsSdkSetting<String>("AWS_CONTAINER_CREDENTIALS_FULL_URI", "aws.containerCredentialsFullUri", null)

    /**
     * An authorization token to pass to a container metadata service.
     */
    public object AwsContainerAuthorizationToken : AwsSdkSetting<String>("AWS_CONTAINER_AUTHORIZATION_TOKEN", "aws.containerAuthorizationToken", null)

    /**
     * The maximum number of request attempts to perform. This is one more than the number of retries, so
     * aws.maxAttempts = 1 will have 0 retries.
     */
    public object AwsMaxAttempts : AwsSdkSetting<Int>("AWS_MAX_ATTEMPTS", "aws.maxAttempts")

    /**
     * Which RetryMode to use for the default RetryPolicy, when one is not specified at the client level.
     */
    public object AwsRetryMode : AwsSdkSetting<RetryMode>("AWS_RETRY_MODE", "aws.retryMode")
}

/**
 * Read the [AwsSdkSetting] by first checking JVM property, environment variable, and default value.
 * Property sources not available on a given platform will be ignored.
 *
 * @param platform A provider of platform-specific settings
 * @return the value of the [AwsSdkSetting] or null if undefined.
 */
@InternalSdkApi
public inline fun <reified T> AwsSdkSetting<T>.resolve(platform: PlatformEnvironProvider): T? {
    val strValue = platform.getProperty(jvmProperty) ?: platform.getenv(environmentVariable)
    if (strValue != null) {
        val typed: Any = when (T::class) {
            String::class -> strValue
            Int::class -> strValue.toInt()
            Long::class -> strValue.toLong()
            Boolean::class -> strValue.toBoolean()
            RetryMode::class -> RetryMode.values().firstOrNull { it.name.equals(strValue, ignoreCase = true) }
                ?: throw ConfigurationException("Retry mode $strValue is not supported, should be one of: ${RetryMode.values().joinToString(", ")}")
            else -> error("conversion to ${T::class} not implemented for AwsSdkSetting")
        }
        return typed as? T
    }
    return defaultValue
}
