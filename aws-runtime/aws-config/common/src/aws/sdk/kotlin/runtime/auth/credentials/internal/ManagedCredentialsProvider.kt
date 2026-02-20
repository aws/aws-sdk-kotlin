/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials.internal

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.auth.awscredentials.CloseableCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.io.SdkManagedCloseable

private class ManagedCredentialsProvider(
    private val delegate: CloseableCredentialsProvider,
) : SdkManagedCloseable(delegate),
    CloseableCredentialsProvider by delegate

/**
 * Wraps a [CredentialsProvider] for internal runtime management by the SDK.
 */
@InternalSdkApi
public fun CloseableCredentialsProvider.manage(): CredentialsProvider = if (this is ManagedCredentialsProvider) this else ManagedCredentialsProvider(this)
