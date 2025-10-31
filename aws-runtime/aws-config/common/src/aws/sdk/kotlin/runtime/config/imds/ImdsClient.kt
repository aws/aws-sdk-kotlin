/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.imds

import aws.sdk.kotlin.runtime.AwsServiceException
import aws.sdk.kotlin.runtime.http.ApiMetadata
import aws.sdk.kotlin.runtime.http.AwsUserAgentMetadata
import aws.sdk.kotlin.runtime.http.middleware.UserAgent
import aws.smithy.kotlin.runtime.client.LogMode
import aws.smithy.kotlin.runtime.client.SdkClientOption
import aws.smithy.kotlin.runtime.client.endpoints.Endpoint
import aws.smithy.kotlin.runtime.http.HttpCall
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.SdkHttpClient
import aws.smithy.kotlin.runtime.http.engine.DefaultHttpEngine
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.http.engine.ProxySelector
import aws.smithy.kotlin.runtime.http.isSuccess
import aws.smithy.kotlin.runtime.http.operation.*
import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.io.closeIfCloseable
import aws.smithy.kotlin.runtime.io.middleware.Phase
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Maximum time allowed by default (6 hours)
 */
internal const val DEFAULT_TOKEN_TTL_SECONDS: Int = 21_600
internal const val DEFAULT_MAX_RETRIES: Int = 3

private const val SERVICE = "imds"

/**
 * Represents a generic client that can fetch instance metadata.
 */
public interface InstanceMetadataProvider : Closeable {
    /**
     * Gets the specified instance metadata value by the given path.
     */
    public suspend fun get(path: String): String
}

/**
 * IMDSv2 Client
 *
 * This client supports fetching tokens, retrying failures, and token caching according to the specified TTL.
 *
 * NOTE: This client ONLY supports IMDSv2. It will not fallback to IMDSv1.
 * See [transitioning to IMDSv2](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-instance-metadata-service.html#instance-metadata-transition-to-version-2)
 * for more information.
 */
public class ImdsClient private constructor(builder: Builder) : InstanceMetadataProvider {
    public constructor() : this(Builder())

    private val maxRetries: Int = builder.maxRetries
    private val endpointConfiguration: EndpointConfiguration = builder.endpointConfiguration
    private val tokenTtl: Duration = builder.tokenTtl
    private val clock: Clock = builder.clock
    private val platformProvider: PlatformProvider = builder.platformProvider
    private val logMode: LogMode = builder.logMode
    private val engine: HttpClientEngine
    private val httpClient: SdkHttpClient
    private val manageEngine: Boolean = builder.engine == null

    init {
        require(maxRetries > 0) { "maxRetries must be greater than zero" }
        engine = builder.engine ?: DefaultHttpEngine {
            connectTimeout = 1.seconds
            socketReadTimeout = 1.seconds

            // don't proxy IMDS requests. https://github.com/awslabs/aws-sdk-kotlin/issues/1315
            proxySelector = ProxySelector.NoProxy
        }

        httpClient = SdkHttpClient(engine)
    }

    // cached middleware instances
    private val userAgentMiddleware = UserAgent(
        staticMetadata = AwsUserAgentMetadata.fromEnvironment(ApiMetadata(SERVICE, "unknown")),
    )

    private val imdsEndpointProvider = ImdsEndpointProvider(platformProvider, endpointConfiguration)
    private val tokenMiddleware = TokenMiddleware(httpClient, imdsEndpointProvider, tokenTtl, clock)

    public companion object {
        public operator fun invoke(block: Builder.() -> Unit): ImdsClient = ImdsClient(Builder().apply(block))
    }

    /**
     * Retrieve information from instance metadata service (IMDS).
     *
     * This method will combine [path] with the configured endpoint and return the response as a string.
     *
     * For more information about IMDSv2 methods and functionality, see
     * [Instance metadata and user data](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html)
     *
     * Example:
     *
     * ```kotlin
     * val client = EC2Metadata()
     * val amiId = client.get("/latest/meta-data/ami-id")
     * ```
     */
    public override suspend fun get(path: String): String {
        val op = SdkHttpOperation.build<Unit, String> {
            serializeWith = HttpSerializer.Unit
            deserializeWith = object : HttpDeserializer.NonStreaming<String> {
                override fun deserialize(context: ExecutionContext, call: HttpCall, payload: ByteArray?): String {
                    val response = call.response
                    if (response.status.isSuccess()) {
                        return payload!!.decodeToString()
                    } else {
                        throw EC2MetadataError(response.status, "error retrieving instance metadata: ${response.status.description}")
                    }
                }
            }
            operationName = path
            serviceName = "IMDS"
            context {
                // artifact of re-using ServiceEndpointResolver middleware
                set(SdkClientOption.LogMode, logMode)
            }

            execution.endpointResolver = imdsEndpointProvider
        }
        op.execution.retryPolicy = ImdsRetryPolicy(coroutineContext)

        op.install(userAgentMiddleware)
        op.install(tokenMiddleware)
        op.execution.mutate.intercept(Phase.Order.Before) { req, next ->
            req.subject.url.path.encoded = path
            next.call(req)
        }

        return op.roundTrip(httpClient, Unit)
    }

    override fun close() {
        if (manageEngine) {
            engine.closeIfCloseable()
        }
    }

    public class Builder {
        /**
         * The maximum number of retries for fetching tokens and metadata
         */
        public var maxRetries: Int = DEFAULT_MAX_RETRIES

        /**
         * The endpoint configuration to use when making requests
         */
        public var endpointConfiguration: EndpointConfiguration = EndpointConfiguration.Default

        /**
         * Override the time-to-live for the session token
         */
        public var tokenTtl: Duration = DEFAULT_TOKEN_TTL_SECONDS.seconds

        /**
         * Configure the [LogMode] used by the client
         */
        public var logMode: LogMode = LogMode.Default

        /**
         * The HTTP engine to use to make requests with
         */
        public var engine: HttpClientEngine? = null

        /**
         * The source of time for token refreshes. This is here to facilitate testing and can otherwise be ignored
         */
        internal var clock: Clock = Clock.System

        /**
         * The platform provider. This is here to facilitate testing and can otherwise be ignored
         */
        internal var platformProvider: PlatformProvider = PlatformProvider.System
    }
}

public sealed class EndpointConfiguration {
    /**
     * Detected from the execution environment
     */
    public object Default : EndpointConfiguration()

    /**
     * Override the endpoint to make requests to
     */
    public data class Custom(val endpoint: Endpoint) : EndpointConfiguration()

    /**
     * Override the [EndpointMode] to use
     */
    public data class ModeOverride(val mode: EndpointMode) : EndpointConfiguration()
}

public enum class EndpointMode(internal val defaultEndpoint: Endpoint) {
    /**
     * IPv4 mode. This is the default unless otherwise specified
     * e.g. `http://169.254.169.254'
     */
    IPv4(Endpoint("http://169.254.169.254")),

    /**
     * IPv6 mode
     * e.g. `http://[fd00:ec2::254]`
     */
    IPv6(Endpoint("http://[fd00:ec2::254]")),
    ;

    public companion object {
        public fun fromValue(value: String): EndpointMode = when (value.lowercase()) {
            "ipv4" -> IPv4
            "ipv6" -> IPv6
            else -> throw IllegalArgumentException("invalid EndpointMode: `$value`")
        }
    }
}

/**
 * Exception thrown when an error occurs retrieving metadata from IMDS
 * @param status The HTTP status code of the response
 * @param message The error message
 */
public class EC2MetadataError(public val status: HttpStatusCode, message: String) : AwsServiceException(message) {
    @Deprecated("This constructor passes HTTP status as an Int instead of as HttpStatusCode. This declaration will be removed in version 1.6.x.")
    public constructor(statusCode: Int, message: String) : this(HttpStatusCode.fromValue(statusCode), message)

    @Deprecated("This property is now deprecated and should be fetched from status.value. This declaration will be removed in version 1.6.x.")
    public val statusCode: Int = status.value
}
