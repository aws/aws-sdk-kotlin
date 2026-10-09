/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials.internal

import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsRefreshBehavior
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsRefreshBehaviorKey
import aws.smithy.kotlin.runtime.collections.emptyAttributes
import aws.smithy.kotlin.runtime.collections.mutableAttributes
import aws.smithy.kotlin.runtime.collections.setIfValueNotNull
import aws.smithy.kotlin.runtime.identity.IdentityAttributes
import aws.smithy.kotlin.runtime.time.Instant

internal fun credentials(
    accessKeyId: String,
    secretAccessKey: String,
    sessionToken: String? = null,
    expiration: Instant? = null,
    providerName: String? = null,
    accountId: String? = null,
    refreshBehavior: CredentialsRefreshBehavior? = null,
): Credentials {
    val attributes = when {
        providerName != null || accountId != null || refreshBehavior != null -> mutableAttributes().apply {
            setIfValueNotNull(IdentityAttributes.ProviderName, providerName)
            setIfValueNotNull(AwsClientOption.AccountId, accountId)
            setIfValueNotNull(CredentialsRefreshBehaviorKey, refreshBehavior)
        }
        else -> emptyAttributes()
    }
    return Credentials(accessKeyId, secretAccessKey, sessionToken, expiration, attributes = attributes)
}
