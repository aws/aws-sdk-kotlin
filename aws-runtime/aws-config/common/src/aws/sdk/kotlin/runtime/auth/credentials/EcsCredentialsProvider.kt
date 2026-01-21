/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetric
import aws.smithy.kotlin.runtime.ErrorMetadata
import aws.smithy.kotlin.runtime.auth.awscredentials.*
import aws.smithy.kotlin.runtime.client.endpoints.Endpoint
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.engine.DefaultHttpEngine
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.http.operation.*
import aws.smithy.kotlin.runtime.http.request.HttpRequestBuilder
import aws.smithy.kotlin.runtime.http.request.header
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.io.closeIfCloseable
import aws.smithy.kotlin.runtime.net.*
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import aws.smithy.kotlin.runtime.serde.json.JsonDeserializer
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import aws.smithy.kotlin.runtime.time.TimestampFormat
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlin.coroutines.coroutineContext

/**
 * The elastic container metadata service endpoint that should be called by the [aws.sdk.kotlin.runtime.auth.credentials.EcsCredentialsProvider]
 * when loading data from the container metadata service.
 *
 * This is not used if the [AwsContainerCredentialsRelativeUri] is not specified.
 */
private const val AWS_CONTAINER_SERVICE_ENDPOINT = "http://169.254.170.2"

private const val PROVIDER_NAME = "EcsContainer"

/**
 * A [CredentialsProvider] that sources credentials from a local metadata service.
 *
 * This provider is frequently used with an AWS-provided credentials service such as Amazon Container Service (ECS).
 * However, it is possible to use environment variables to configure this provider to use any local metadata service.
 *
 * For more information on configuring ECS credentials see [IAM Roles for tasks](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html)
 *
 * @param platformProvider the platform provider
 * @param httpClient the [HttpClientEngine] instance to use to make requests. NOTE: This engine's resources and lifetime
 * are NOT managed by the provider. Caller is responsible for closing.
 * @param hostResolver used to resolve hostname from AwsContainerCredentialsFullUri env setting. If not specified falls back to default.
 */
public class EcsCredentialsProvider(
    public val platformProvider: PlatformProvider = PlatformProvider.System,
    httpClient: HttpClientEngine? = null,
    private val hostResolver: HostResolver = HostResolver.Default,
) : CloseableCredentialsProvider {

    // Keeping previous constructor as secondary due to backwards compatibility.
    public constructor(
        platformProvider: PlatformProvider = PlatformProvider.System,
        httpClient: HttpClientEngine? = null,
    ) : this(
        platformProvider,
        httpClient,
        HostResolver.Default,
    )

    private val manageEngine = httpClient == null
    private val httpClient: HttpClientEngine = httpClient ?: DefaultHttpEngine()

    override suspend fun resolve(attributes: Attributes): Credentials {
        val logger = coroutineContext.logger<EcsCredentialsProvider>()

        val authToken = loadAuthToken()
        val relativeUri = AwsSdkSetting.AwsContainerCredentialsRelativeUri.resolve(platformProvider)
        val fullUri = AwsSdkSetting.AwsContainerCredentialsFullUri.resolve(platformProvider)

        val url = when {
            relativeUri?.isBlank() == false -> validateRelativeUri(relativeUri)
            fullUri?.isBlank() == false -> validateFullUri(fullUri)
            else -> throw ProviderConfigurationException("Container credentials URI not set")
        }

        val op = SdkHttpOperation.build<Unit, Credentials> {
            serializeWith = EcsCredentialsSerializer(authToken)
            deserializeWith = EcsCredentialsDeserializer()
            operationName = "EcsCredentialsProvider"
            serviceName = "EcsContainerMetadata"
            execution.endpointResolver = EndpointResolver { Endpoint(url) }
        }

        logger.debug { "retrieving container credentials" }
        val client = SdkHttpClient(httpClient)
        val creds = try {
            op.roundTrip(client, Unit)
        } catch (ex: Exception) {
            logger.debug { "failed to obtain credentials from container metadata service" }
            throw when (ex) {
                is CredentialsProviderException -> ex
                else -> CredentialsProviderException("Failed to get credentials from container metadata service", ex)
            }
        }

        logger.debug { "obtained credentials from container metadata service; expiration=${creds.expiration?.format(TimestampFormat.ISO_8601)}" }

        return creds.withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_HTTP)
    }

    private suspend fun loadAuthToken(): String? {
        val token = AwsSdkSetting.AwsContainerAuthorizationTokenFile.resolve(platformProvider)?.let { loadAuthTokenFromFile(it) }
            ?: AwsSdkSetting.AwsContainerAuthorizationToken.resolve(platformProvider)
            ?: return null

        if (token.contains("\r\n")) {
            throw CredentialsProviderException("Token contains illegal line break sequence.")
        }

        return token
    }

    private suspend fun loadAuthTokenFromFile(path: String): String = platformProvider.readFileOrNull(path)?.decodeToString()
        ?: throw CredentialsProviderException("Could not read token file.")

    /**
     * Validate that the [relativeUri] can be combined with the static ECS endpoint to form a valid URL
     */
    private fun validateRelativeUri(relativeUri: String): Url = try {
        Url.parse("${AWS_CONTAINER_SERVICE_ENDPOINT}$relativeUri")
    } catch (ex: Exception) {
        throw ProviderConfigurationException("Invalid relativeUri `$relativeUri`", ex)
    }

    /**
     * Validate that [uri] is valid to be used as a full provider URI
     *
     * Either:
     * 1. The URL uses `https
     * 2. The URL refers to a loopback device. If a URL contains a domain name instead of an IP address a DNS lookup
     * will be performed. ALL resolved IP addresses MUST refer to a loopback interface.
     *
     * @return the validated URL
     */
    private suspend fun validateFullUri(uri: String): Url {
        val url = try {
            Url.parse(uri)
        } catch (ex: Exception) {
            throw ProviderConfigurationException("Invalid fullUri `$uri`", ex)
        }

        if (url.scheme == Scheme.HTTPS) return url

        when (url.host) {
            is Host.IpAddress -> {
                if (allowedAddrs.contains((url.host as Host.IpAddress).address)) {
                    return url
                }

                throw ProviderConfigurationException(
                    "The container credentials full URI ($uri) has an invalid host. Host can only be one of [${allowedAddrs.joinToString()}].",
                )
            }

            is Host.Domain -> {
                val hostAddresses = try {
                    hostResolver.resolve(url.host.toString())
                } catch (exception: Throwable) {
                    throw ProviderConfigurationException(
                        "The container credentials full URI ($uri) is specified via a hostname whose IP address(es) could not be resolved. ${exception.message}",
                        exception,
                    )
                }

                if (hostAddresses.isNotEmpty() && hostAddresses.all { it.address.isLoopBack }) {
                    return url
                } else {
                    throw ProviderConfigurationException(
                        "The container credentials full URI ($uri) is specified via a hostname whose IP address(es) do not resolve to the loopback device.",
                    )
                }
            }
        }
    }

    override fun close() {
        if (manageEngine) {
            httpClient.closeIfCloseable()
        }
    }

    override fun toString(): String = this.simpleClassName
}

private class EcsCredentialsDeserializer : HttpDeserializer.NonStreaming<Credentials> {
    override fun deserialize(context: ExecutionContext, call: HttpCall, payload: ByteArray?): Credentials {
        val response = call.response
        if (!response.status.isSuccess()) {
            throwCredentialsResponseException(response, payload)
        }

        if (payload == null) throw CredentialsProviderException("HTTP credentials response did not contain a payload")
        val deserializer = JsonDeserializer(payload)
        val resp = deserializeJsonCredentials(deserializer)
        if (resp !is JsonCredentialsResponse.SessionCredentials) {
            throw CredentialsProviderException("HTTP credentials response was not of expected format")
        }

        return credentials(
            resp.accessKeyId,
            resp.secretAccessKey,
            resp.sessionToken,
            resp.expiration,
            PROVIDER_NAME,
            resp.accountId,
        )
    }
}

private fun throwCredentialsResponseException(response: HttpResponse, payload: ByteArray?): Nothing {
    val errorResp = tryParseErrorResponse(response, payload)
    val messageDetails = errorResp?.run { "code=$code; message=$message" } ?: "HTTP ${response.status}"

    throw CredentialsProviderException("Error retrieving credentials from container service: $messageDetails").apply {
        sdkErrorMetadata.attributes[ErrorMetadata.ThrottlingError] = response.status == HttpStatusCode.TooManyRequests
        sdkErrorMetadata.attributes[ErrorMetadata.Retryable] =
            sdkErrorMetadata.isThrottling ||
            response.status.category() == HttpStatusCode.Category.SERVER_ERROR
    }
}

private fun tryParseErrorResponse(response: HttpResponse, payload: ByteArray?): JsonCredentialsResponse.Error? {
    if (response.headers["Content-Type"] != "application/json" || payload == null) return null
    return deserializeJsonCredentials(JsonDeserializer(payload)) as? JsonCredentialsResponse.Error
}

private class EcsCredentialsSerializer(
    private val authToken: String? = null,
) : HttpSerializer.NonStreaming<Unit> {
    override fun serialize(context: ExecutionContext, input: Unit): HttpRequestBuilder {
        val builder = HttpRequestBuilder()
        builder.url.path
        builder.header("Accept", "application/json")
        builder.header("Accept-Encoding", "identity")
        if (authToken != null) {
            builder.header("Authorization", authToken)
        }
        return builder
    }
}

private val ecsV4Addr = IpV4Addr(169u, 254u, 170u, 2u)

private val eksV4Addr = IpV4Addr(169u, 254u, 170u, 23u)

private val eksV6Addr = IpV6Addr(0xfd00u, 0xec2u, 0u, 0u, 0u, 0u, 0u, 0x23u)

private val allowedAddrs = setOf(IpV4Addr.LOCALHOST, IpV6Addr.LOCALHOST, ecsV4Addr, eksV4Addr, eksV6Addr)
