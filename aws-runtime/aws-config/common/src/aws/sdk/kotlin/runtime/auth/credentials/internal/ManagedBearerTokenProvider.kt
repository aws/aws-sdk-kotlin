/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials.internal

import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.http.auth.BearerTokenProvider
import aws.smithy.kotlin.runtime.http.auth.CloseableBearerTokenProvider
import aws.smithy.kotlin.runtime.io.SdkManagedCloseable

private class ManagedTokenProvider(
    private val delegate: CloseableBearerTokenProvider,
) : SdkManagedCloseable(delegate),
    CloseableBearerTokenProvider by delegate

/**
 * Wraps a [TokenProvider] for internal runtime management by the SDK.
 */
@InternalApi
public fun CloseableBearerTokenProvider.manage(): BearerTokenProvider = if (this is ManagedTokenProvider) this else ManagedTokenProvider(this)
