/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials.internal

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.auth.awscredentials.CloseableCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.RefreshAwareCredentialsProvider
import aws.smithy.kotlin.runtime.io.SdkManagedCloseable

private open class ManagedCredentialsProvider(
    delegate: CloseableCredentialsProvider,
) : SdkManagedCloseable(delegate),
    CloseableCredentialsProvider by delegate

/**
 * A [ManagedCredentialsProvider] whose delegate implements the refresh lifecycle. It keeps the delegate's
 * [RefreshAwareCredentialsProvider] marker: the invalidation interceptor marks a request rejected for expired
 * credentials for retry only when the provider that resolved them is refresh-aware, and the clients' default provider
 * reaches it through this wrapper.
 */
private class ManagedRefreshAwareCredentialsProvider(
    delegate: CloseableCredentialsProvider,
) : ManagedCredentialsProvider(delegate),
    RefreshAwareCredentialsProvider

/**
 * Wraps a [CredentialsProvider] for internal runtime management by the SDK. A refresh-aware provider stays
 * refresh-aware.
 */
@InternalSdkApi
public fun CloseableCredentialsProvider.manage(): CredentialsProvider = when (this) {
    is ManagedCredentialsProvider -> this
    is RefreshAwareCredentialsProvider -> ManagedRefreshAwareCredentialsProvider(this)
    else -> ManagedCredentialsProvider(this)
}
