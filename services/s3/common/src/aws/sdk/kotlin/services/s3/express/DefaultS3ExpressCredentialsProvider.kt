/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.express

import aws.sdk.kotlin.services.s3.*
import aws.sdk.kotlin.services.s3.S3Attributes
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.ExperimentalApi
import aws.smithy.kotlin.runtime.auth.awscredentials.CloseableCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.simpleClassName
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.io.SdkManagedBase
import aws.smithy.kotlin.runtime.telemetry.logging.getLogger
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.time.until
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/**
 * The default implementation of a credentials provider for S3 Express One Zone. Performs best-effort asynchronous refresh
 * if the cached credentials are expiring within a [refreshBuffer] during a call to [resolve].
 * Otherwise, performs synchronous refresh.
 *
 * @param timeSource the time source to use. defaults to [TimeSource.Monotonic]
 * @param clock the clock to use. defaults to [Clock.System]. note: the clock is only used to get an initial [Duration]
 * until credentials expiration, [timeSource] is used as the source of truth for credentials expiration.
 * @param credentialsCache an [S3ExpressCredentialsCache] to be used for caching session credentials, defaults to
 * [S3ExpressCredentialsCache].
 * @param refreshBuffer an optional [Duration] representing the duration before expiration that [Credentials]
 * are considered refreshable, defaults to 1 minute.
 */
internal class DefaultS3ExpressCredentialsProvider(
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
    private val clock: Clock = Clock.System,
    private val credentialsCache: S3ExpressCredentialsCache = S3ExpressCredentialsCache(),
    private val refreshBuffer: Duration = 1.minutes,
) : SdkManagedBase(),
    CloseableCredentialsProvider,
    CoroutineScope {
    override val coroutineContext: CoroutineContext = Job() + CoroutineName("DefaultS3ExpressCredentialsProvider")

    override suspend fun resolve(attributes: Attributes): Credentials {
        val client = attributes[S3Attributes.ExpressClient] as S3Client

        val key = S3ExpressCredentialsCacheKey(attributes[S3Attributes.Bucket], client.config.credentialsProvider.resolve(attributes))

        return credentialsCache.get(key)
            ?.takeIf { !it.expiringCredentials.isExpired }
            ?.also {
                if (it.expiringCredentials.isExpiringWithin(refreshBuffer)) {
                    client.logger.trace { "Credentials for ${key.bucket} are expiring in ${it.expiringCredentials.expiresAt} and are within their refresh window, performing asynchronous refresh..." }
                    launch(coroutineContext) {
                        try {
                            it.sfg.singleFlight {
                                // This coroutine/SFG may have started _after_ prior instances(s) finished, replacing
                                // the cached value already. To prevent re-refreshing it, we need to re-acquire the
                                // current cached value and verify whether it's expiring soon.
                                val currentCreds = credentialsCache.get(key)

                                if (currentCreds?.expiringCredentials?.isExpiringWithin(refreshBuffer) == true) {
                                    createSessionCredentials(key, client)
                                } else {
                                    it.expiringCredentials
                                }
                            }
                        } catch (e: Exception) {
                            client.logger.warn(e) { "Asynchronous refresh for ${key.bucket} failed." }
                        }
                    }
                }
            }
            ?.expiringCredentials
            ?.value
            ?: createSessionCredentials(key, client).value
    }

    override fun close() {
        coroutineContext.cancel(null)
    }

    /**
     * Create a new set of session credentials by calling s3:CreateSession and then store them in the cache.
     */
    internal suspend fun createSessionCredentials(key: S3ExpressCredentialsCacheKey, client: S3Client): ExpiringValue<Credentials> = client.createSession { bucket = key.bucket }.credentials!!.let {
        ExpiringValue(
            Credentials(it.accessKeyId, it.secretAccessKey, it.sessionToken, it.expiration),
            expiresAt = timeSource.markNow() + clock.now().until(it.expiration),
        )
    }.also {
        credentialsCache.put(key, S3ExpressCredentialsCacheValue(it))
    }

    @OptIn(ExperimentalApi::class)
    internal val S3Client.logger get() = config.telemetryProvider.loggerProvider.getLogger<DefaultS3ExpressCredentialsProvider>()

    override fun toString(): String = this.simpleClassName
}
