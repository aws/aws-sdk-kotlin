/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsRefreshBehavior
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsRefreshBehaviorKey
import aws.smithy.kotlin.runtime.auth.awscredentials.copy
import aws.smithy.kotlin.runtime.collections.toMutableAttributes

/**
 * Declares the refresh behavior a provider attaches to the credentials it resolves.
 *
 * Expected values in these tests are compared with `assertEquals`, and the declaration is carried in [attributes],
 * which participates in equality - so an expected value has to state it just as the provider does.
 */
internal fun Credentials.withRefreshBehavior(behavior: CredentialsRefreshBehavior): Credentials = copy(
    attributes = attributes.toMutableAttributes().apply { set(CredentialsRefreshBehaviorKey, behavior) },
)

/**
 * Drops the refresh-behavior declaration, for comparison against an expected value that does not model it.
 */
internal fun Credentials.withoutRefreshBehavior(): Credentials = copy(
    attributes = attributes.toMutableAttributes().apply { remove(CredentialsRefreshBehaviorKey) },
)
