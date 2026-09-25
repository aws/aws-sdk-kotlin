/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.CheckpointRestoreLifecycle
import aws.sdk.kotlin.runtime.config.CheckpointRestoreLock
import aws.sdk.kotlin.runtime.config.createCheckpointRestoreAwareDefaultHttpEngine
import aws.sdk.kotlin.runtime.config.createCheckpointRestoreLock
import aws.sdk.kotlin.runtime.config.imds.ImdsClient
import aws.sdk.kotlin.runtime.config.isCheckpointRestoreEnvironment
import aws.sdk.kotlin.runtime.config.withLock
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetric
import aws.smithy.kotlin.runtime.auth.awscredentials.*
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.io.closeIfCloseable
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Default AWS credential provider chain used by most AWS SDKs.
 *
 * Resolution order:
 *
 * 1. System properties ([SystemPropertyCredentialsProvider])
 * 2. Environment variables ([EnvironmentCredentialsProvider])
 * 3. Web Identity Tokens ([StsWebIdentityCredentialsProvider]]
 * 4. Profile ([ProfileCredentialsProvider])
 * 5. ECS (IAM roles for tasks) ([EcsCredentialsProvider])
 * 6. EC2 Instance Metadata (IMDSv2) ([ImdsCredentialsProvider])
 *
 * The chain is decorated with a [CachedCredentialsProvider].
 *
 * Closing the chain will close all child providers that implement [Closeable].
 *
 * @param profileName Override the profile name to use. If not provided it will be resolved internally
 * via environment (see [AwsSdkSetting.AwsProfile]) or defaulted to `default` if not configured.
 * @param platformProvider The platform API provider
 * @param httpClient the [HttpClientEngine] instance to use to make requests. NOTE: This engine's resources and lifetime
 * are NOT managed by the provider. Caller is responsible for closing.
 * @param region the region to make credentials requests to.
 * @return the newly-constructed credentials provider
 */
public class DefaultChainCredentialsProvider constructor(
    public val profileName: String? = null,
    public val platformProvider: PlatformProvider = PlatformProvider.System,
    httpClient: HttpClientEngine? = null,
    public val region: String? = null,
) : CloseableCredentialsProvider {

    private val manageEngine = httpClient == null
    private val engine = httpClient ?: createCheckpointRestoreAwareDefaultHttpEngine()

    private val lifecycleLock = createCheckpointRestoreLock()
    private var state: ProviderState? = createState()
    private var closed = false
    private var checkpointed = false

    @Suppress("unused")
    private val checkpointRestoreLifecycle = if (isCheckpointRestoreEnvironment()) {
        CheckpointRestoreLifecycle(::beforeCheckpoint, ::afterRestore).register()
    } else {
        null
    }

    override suspend fun resolve(attributes: Attributes): Credentials {
        val provider = lifecycleLock.withLock { state?.provider }
        return provider?.resolve(attributes)
            ?: throw CredentialsProviderException("Credentials provider is unavailable during a checkpoint")
    }

    override fun close() {
        val provider = lifecycleLock.withLock {
            if (closed) return
            closed = true
            state.also { state = null }?.provider
        }
        try {
            provider?.close()
        } finally {
            if (manageEngine) {
                engine.closeIfCloseable()
            }
        }
    }

    override fun toString(): String = this.simpleClassName + ": " + lifecycleLock.withLock { state?.chain }

    internal fun beforeCheckpoint() {
        val provider = lifecycleLock.withLock {
            if (closed || checkpointed) return
            checkpointed = true
            state.also { state = null }?.provider
        }
        try {
            provider?.close()
        } catch (ex: Exception) {
            recoverFromFailedCheckpoint()
            throw IllegalStateException("Failed to close the default credentials provider before the checkpoint", ex)
        }
    }

    internal fun afterRestore() {
        lifecycleLock.withLock {
            if (closed || !checkpointed) return
            state = createState()
            checkpointed = false
        }
    }

    private fun recoverFromFailedCheckpoint() {
        lifecycleLock.withLock {
            if (closed || !checkpointed) return
            state = createState()
            checkpointed = false
        }
    }

    private fun createState(): ProviderState {
        val chain = CredentialsProviderChain(
            SystemPropertyCredentialsProvider(platformProvider::getProperty),
            EnvironmentCredentialsProvider(platformProvider::getenv),
            // STS web identity provider can be constructed from either the profile OR 100% from the environment
            StsWebIdentityProvider(platformProvider = platformProvider, httpClient = engine, region = region),
            ProfileCredentialsProvider(profileName = profileName, platformProvider = platformProvider, httpClient = engine, region = region),
            EcsCredentialsProvider(platformProvider, engine),
            ImdsCredentialsProvider(
                client = lazy {
                    ImdsClient {
                        platformProvider = this@DefaultChainCredentialsProvider.platformProvider
                        engine = this@DefaultChainCredentialsProvider.engine
                    }
                },
                platformProvider = platformProvider,
            ),
        )
        return ProviderState(chain, CachedCredentialsProvider(chain))
    }

    private data class ProviderState(
        val chain: CredentialsProviderChain,
        val provider: CachedCredentialsProvider,
    )
}

/**
 * Wrapper around [StsWebIdentityCredentialsProvider] that delays any exceptions until [resolve] is invoked.
 * This allows it to be part of the default chain and any failures result in the chain to move onto the next provider.
 */
public class StsWebIdentityProvider(
    public val platformProvider: PlatformProvider = PlatformProvider.System,
    public val httpClient: HttpClientEngine? = null,
    public val region: String? = null,
) : CloseableCredentialsProvider {
    override suspend fun resolve(attributes: Attributes): Credentials {
        val wrapped = StsWebIdentityCredentialsProvider.fromEnvironment(platformProvider = platformProvider, httpClient = httpClient, region = region)
        return wrapped.resolve(attributes).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_ENV_VARS_STS_WEB_ID_TOKEN)
    }

    override fun close() { }
}
