/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.CheckpointRestoreLifecycle
import aws.sdk.kotlin.runtime.config.CheckpointRestoreLock
import aws.sdk.kotlin.runtime.config.createCheckpointRestoreLock
import aws.sdk.kotlin.runtime.config.isCheckpointRestoreEnvironment
import aws.sdk.kotlin.runtime.config.withLock
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.http.auth.BearerToken
import aws.smithy.kotlin.runtime.http.auth.BearerTokenProviderChain
import aws.smithy.kotlin.runtime.http.auth.CloseableBearerTokenProvider
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.identity.IdentityProviderException
import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Default AWS bearer token provider chain used by services marked with [@httpBearerAuth](https://smithy.io/2.0/spec/authentication-traits.html#smithy-api-httpbearerauth-trait)
 *
 * Resolution order:
 *
 * 1. Profile ([ProfileBearerTokenProvider]
 *
 * Closing the chain will close all child providers that implement [Closeable].
 *
 * @param profileName Override the profile name to use. If not provided it will be resolved internally
 * via environment (see [AwsSdkSetting.AwsProfile]) or defaulted to `default` if not configured.
 * @param platformProvider The platform API provider
 * @param httpClient the [HttpClientEngine] instance to use to make requests. NOTE: This engine's resources and lifetime
 * are NOT managed by the provider. Caller is responsible for closing.
 */
public class DefaultChainBearerTokenProvider(
    public val profileName: String? = null,
    public val platformProvider: PlatformProvider = PlatformProvider.System,
    public val httpClient: HttpClientEngine? = null,
) : CloseableBearerTokenProvider {

    private val lifecycleLock = createCheckpointRestoreLock()
    private var chain: BearerTokenProviderChain? = createChain()
    private var closed = false
    private var checkpointed = false

    @Suppress("unused")
    private val checkpointRestoreLifecycle = if (isCheckpointRestoreEnvironment()) {
        CheckpointRestoreLifecycle(::beforeCheckpoint, ::afterRestore).register()
    } else {
        null
    }

    override suspend fun resolve(attributes: Attributes): BearerToken {
        val providerChain = lifecycleLock.withLock { chain }
        return providerChain?.resolve(attributes)
            ?: throw IdentityProviderException("Bearer token provider is unavailable during a checkpoint")
    }

    override fun close() {
        val providerChain = lifecycleLock.withLock {
            if (closed) return
            closed = true
            chain.also { chain = null }
        }
        providerChain?.close()
    }

    internal fun beforeCheckpoint() {
        val providerChain = lifecycleLock.withLock {
            if (closed || checkpointed) return
            checkpointed = true
            chain.also { chain = null }
        }
        try {
            providerChain?.close()
        } catch (ex: Exception) {
            recoverFromFailedCheckpoint()
            throw IllegalStateException("Failed to close the default bearer-token provider before the checkpoint", ex)
        }
    }

    internal fun afterRestore() {
        lifecycleLock.withLock {
            if (closed || !checkpointed) return
            chain = createChain()
            checkpointed = false
        }
    }

    private fun recoverFromFailedCheckpoint() {
        lifecycleLock.withLock {
            if (closed || !checkpointed) return
            chain = createChain()
            checkpointed = false
        }
    }

    private fun createChain(): BearerTokenProviderChain = BearerTokenProviderChain(
        ProfileBearerTokenProvider(profileName, platformProvider, httpClient),
    )
}
