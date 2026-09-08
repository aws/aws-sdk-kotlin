/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsRefreshBehavior
import aws.smithy.kotlin.runtime.auth.awscredentials.simpleClassName
import aws.smithy.kotlin.runtime.collections.Attributes

private const val PROVIDER_NAME = "Static"

/**
 * A credentials provider for a fixed set of credentials
 *
 * @param credentials The set of static credentials this provider will return
 */
public class StaticCredentialsProvider(public val credentials: Credentials) : CredentialsProvider {

    private constructor(builder: Builder) : this(
        credentials(
            builder.accessKeyId!!,
            builder.secretAccessKey!!,
            builder.sessionToken,
            providerName = PROVIDER_NAME,
            accountId = builder.accountId,
            refreshBehavior = CredentialsRefreshBehavior.NonRefreshable,
        ),
    )

    override suspend fun resolve(attributes: Attributes): Credentials = credentials

    public companion object {
        /**
         * Construct a new [StaticCredentialsProvider] using the given [block]
         */
        public operator fun invoke(block: Builder.() -> Unit): StaticCredentialsProvider = Builder().apply(block).build()
    }

    public class Builder {
        public var accessKeyId: String? = null
        public var secretAccessKey: String? = null
        public var sessionToken: String? = null
        public var accountId: String? = null

        public fun build(): StaticCredentialsProvider {
            if (accessKeyId == null || secretAccessKey == null) {
                throw IllegalArgumentException("StaticCredentialsProvider - accessKeyId and secretAccessKey must not be null")
            }
            return StaticCredentialsProvider(this)
        }
    }

    override fun toString(): String = this.simpleClassName
}
