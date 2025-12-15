/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.endpoints.AccountIdEndpointMode
import aws.sdk.kotlin.runtime.http.AWS_APP_ID_ENV
import aws.sdk.kotlin.runtime.http.AWS_APP_ID_PROP
import aws.smithy.kotlin.runtime.client.config.RequestHttpChecksumConfig
import aws.smithy.kotlin.runtime.client.config.ResponseHttpChecksumConfig
import aws.smithy.kotlin.runtime.client.config.RetryMode
import aws.smithy.kotlin.runtime.config.*
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.util.PlatformProvider

// NOTE: The JVM property names MUST match the ones defined in the Java SDK for any setting added.
// see: https://github.com/aws/aws-sdk-java-v2/blob/master/core/sdk-core/src/main/java/software/amazon/awssdk/core/SdkSystemSetting.java
// see: https://github.com/aws/aws-sdk-java-v2/blob/master/docs/LaunchChangelog.md#61-environment-variables-and-system-properties

/**
 * Settings to configure SDK runtime behavior
 */
@InternalSdkApi
public object AwsSdkSetting {
    /**
     * Configure the AWS access key ID.
     *
     * This value will not be ignored if the [AwsSecretAccessKey] is not specified.
     */
    public val AwsAccessKeyId: EnvironmentSetting<String> = strEnvSetting("aws.accessKeyId", "AWS_ACCESS_KEY_ID")

    /**
     * Configure the AWS secret access key.
     *
     * This value will not be ignored if the [AwsAccessKeyId] is not specified.
     */
    public val AwsSecretAccessKey: EnvironmentSetting<String> =
        strEnvSetting("aws.secretAccessKey", "AWS_SECRET_ACCESS_KEY")

    /**
     * Configure the AWS session token.
     */
    public val AwsSessionToken: EnvironmentSetting<String> = strEnvSetting("aws.sessionToken", "AWS_SESSION_TOKEN")

    /**
     * Configure the default region.
     */
    public val AwsRegion: EnvironmentSetting<String> = strEnvSetting("aws.region", "AWS_REGION")

    /**
     * Configure the AWS account ID
     */
    public val AwsAccountId: EnvironmentSetting<String> = strEnvSetting("aws.accountId", "AWS_ACCOUNT_ID")

    /**
     * Configure the user agent app ID
     */
    public val AwsAppId: EnvironmentSetting<String> = strEnvSetting(AWS_APP_ID_PROP, AWS_APP_ID_ENV)

    /**
     * Configure the default path to the shared config file.
     */
    public val AwsConfigFile: EnvironmentSetting<String> = strEnvSetting("aws.configFile", "AWS_CONFIG_FILE")

    /**
     * Configure the default path to the shared credentials profile file.
     */
    public val AwsSharedCredentialsFile: EnvironmentSetting<String> =
        strEnvSetting("aws.sharedCredentialsFile", "AWS_SHARED_CREDENTIALS_FILE")

    /**
     * The execution environment of the SDK user. This is automatically set in certain environments by the underlying
     * AWS service. For example, AWS Lambda will automatically specify a runtime indicating that the SDK is being used
     * within Lambda.
     */
    public val AwsExecutionEnv: EnvironmentSetting<String> =
        strEnvSetting("aws.executionEnvironment", "AWS_EXECUTION_ENV")

    /**
     *  The name of the default profile that should be loaded from config
     */
    public val AwsProfile: EnvironmentSetting<String> = strEnvSetting("aws.profile", "AWS_PROFILE").orElse("default")

    /**
     * Whether to load information such as credentials, regions from EC2 Metadata instance service.
     */
    public val AwsEc2MetadataDisabled: EnvironmentSetting<Boolean> =
        boolEnvSetting("aws.disableEc2Metadata", "AWS_EC2_METADATA_DISABLED").orElse(false)

    /**
     * The EC2 instance metadata service endpoint.
     *
     * This allows a service running in EC2 to automatically load its credentials and region without needing to
     * configure them directly.
     */
    public val AwsEc2MetadataServiceEndpoint: EnvironmentSetting<String> =
        strEnvSetting("aws.ec2MetadataServiceEndpoint", "AWS_EC2_METADATA_SERVICE_ENDPOINT")

    /**
     * The endpoint mode to use when connecting to the EC2 metadata service endpoint
     */
    public val AwsEc2MetadataServiceEndpointMode: EnvironmentSetting<String> =
        strEnvSetting("aws.ec2MetadataServiceEndpointMode", "AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE")

    // TODO Currently env/system properties around role ARN, role session name, etc are restricted to the STS web
    //  identity provider. They should be applied more broadly but this needs fleshed out across AWS SDKs before we can
    //  do so.

    /**
     * The ARN of a role to assume
     */
    public val AwsRoleArn: EnvironmentSetting<String> = strEnvSetting("aws.roleArn", "AWS_ROLE_ARN")

    /**
     * The session name to use for assumed roles
     */
    public val AwsRoleSessionName: EnvironmentSetting<String> =
        strEnvSetting("aws.roleSessionName", "AWS_ROLE_SESSION_NAME")

    /**
     * The AWS web identity token file path
     */
    public val AwsWebIdentityTokenFile: EnvironmentSetting<String> =
        strEnvSetting("aws.webIdentityTokenFile", "AWS_WEB_IDENTITY_TOKEN_FILE")

    /**
     * The elastic container metadata service path that should be called by the
     * [aws.sdk.kotlin.runtime.auth.credentials.EcsCredentialsProvider] when loading credentials from the container
     * metadata service.
     */
    public val AwsContainerCredentialsRelativeUri: EnvironmentSetting<String> =
        strEnvSetting("aws.containerCredentialsPath", "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI")

    /**
     * The full URI path to a localhost metadata service to be used. This is ignored if
     * [AwsContainerCredentialsRelativeUri] is set.
     */
    public val AwsContainerCredentialsFullUri: EnvironmentSetting<String> =
        strEnvSetting("aws.containerCredentialsFullUri", "AWS_CONTAINER_CREDENTIALS_FULL_URI")

    /**
     * An authorization token to pass to a container metadata service.
     */
    public val AwsContainerAuthorizationToken: EnvironmentSetting<String> =
        strEnvSetting("aws.containerAuthorizationToken", "AWS_CONTAINER_AUTHORIZATION_TOKEN")

    /**
     * A path to a file which contains an authorization token to pass to a container metadata service.
     */
    public val AwsContainerAuthorizationTokenFile: EnvironmentSetting<String> =
        strEnvSetting("aws.containerAuthorizationTokenFile", "AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE")

    /**
     * The maximum number of request attempts to perform. This is one more than the number of retries, so
     * aws.maxAttempts = 1 will have 0 retries.
     */
    public val AwsMaxAttempts: EnvironmentSetting<Int> = intEnvSetting("aws.maxAttempts", "AWS_MAX_ATTEMPTS")

    /**
     * Which RetryMode to use for the default RetryPolicy, when one is not specified at the client level.
     */
    public val AwsRetryMode: EnvironmentSetting<RetryMode> = enumEnvSetting("aws.retryMode", "AWS_RETRY_MODE")

    /**
     * Whether to use FIPS endpoints when making requests.
     */
    public val AwsUseFipsEndpoint: EnvironmentSetting<Boolean> =
        boolEnvSetting("aws.useFipsEndpoint", "AWS_USE_FIPS_ENDPOINT")

    /**
     * Whether to use dual-stack endpoints when making requests.
     */
    public val AwsUseDualStackEndpoint: EnvironmentSetting<Boolean> =
        boolEnvSetting("aws.useDualstackEndpoint", "AWS_USE_DUALSTACK_ENDPOINT")

    /**
     * The globally-configured endpoint URL that applies to all services.
     */
    public val AwsEndpointUrl: EnvironmentSetting<Url> =
        EnvironmentSetting(Url::parse)("aws.endpointUrl", "AWS_ENDPOINT_URL")

    /**
     * Whether to ignore configured endpoint URLs.
     */
    public val AwsIgnoreEndpointUrls: EnvironmentSetting<Boolean> =
        boolEnvSetting("aws.ignoreConfiguredEndpointUrls", "AWS_IGNORE_CONFIGURED_ENDPOINT_URLS")

    /**
     * The mode to use when resolving endpoints that make use of the AWS account ID.
     */
    public val AwsAccountIdEndpointMode: EnvironmentSetting<AccountIdEndpointMode> = enumEnvSetting("aws.accountIdEndpointMode", "AWS_ACCOUNT_ID_ENDPOINT_MODE")

    /**
     * Determines if a request should be compressed or not
     */
    public val AwsDisableRequestCompression: EnvironmentSetting<Boolean> =
        boolEnvSetting("aws.disableRequestCompression", "AWS_DISABLE_REQUEST_COMPRESSION")

    /**
     * The threshold used to determine if a request should be compressed. Is only checked if request compression is enabled
     */
    public val AwsRequestMinCompressionSizeBytes: EnvironmentSetting<Long> =
        longEnvSetting("aws.requestMinCompressionSizeBytes", "AWS_REQUEST_MIN_COMPRESSION_SIZE_BYTES")

    /**
     * The set of regions to use when signing a request with sigV4a.
     */
    public val AwsSigV4aSigningRegionSet: EnvironmentSetting<String> =
        strEnvSetting("aws.sigV4aSigningRegionSet", "AWS_SIGV4A_SIGNING_REGION_SET")

    /**
     * A flag indicating whether endpoint discovery is enabled for AWS services that support it. The implicit default
     * value for this setting is:
     * * `true` for services which _require_ EP discovery (e.g., Timestream)
     * * `false` for services which _allow but do not require_ EP discovery (e.g., DynamoDB)
     */
    public val AwsEndpointDiscoveryEnabled: EnvironmentSetting<Boolean> =
        boolEnvSetting("aws.endpointDiscoveryEnabled", "AWS_ENABLE_ENDPOINT_DISCOVERY")

    /**
     * Configures request checksum calculation
     */
    public val AwsRequestChecksumCalculation: EnvironmentSetting<RequestHttpChecksumConfig> =
        enumEnvSetting<RequestHttpChecksumConfig>("aws.requestChecksumCalculation", "AWS_REQUEST_CHECKSUM_CALCULATION")

    /**
     * Configures response checksum validation
     */
    public val AwsResponseChecksumValidation: EnvironmentSetting<ResponseHttpChecksumConfig> =
        enumEnvSetting<ResponseHttpChecksumConfig>("aws.responseChecksumValidation", "AWS_RESPONSE_CHECKSUM_VALIDATION")

    /**
     * Configures an ordered preference of auth schemes
     */
    public val AwsAuthSchemePreference: EnvironmentSetting<String> = strEnvSetting("aws.authSchemePreference", "AWS_AUTH_SCHEME_PREFERENCE")
}

/**
 * Resolves an endpoint url for a given service.
 */
@InternalSdkApi
public fun AwsSdkSetting.resolveEndpointUrl(
    provider: PlatformProvider,
    sysPropSuffix: String,
    envSuffix: String,
): Url? {
    val serviceSetting = EnvironmentSetting(Url::parse)("aws.endpointUrl", "AWS_ENDPOINT_URL_")
        .withCaseInsensitiveSuffixes(sysPropSuffix, envSuffix)
    return serviceSetting.resolve(provider) ?: AwsEndpointUrl.resolve(provider)
}
