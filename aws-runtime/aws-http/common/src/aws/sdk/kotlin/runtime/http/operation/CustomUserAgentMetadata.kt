/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.http.operation

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.http.uaPair
import aws.smithy.kotlin.runtime.collections.AttributeKey
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import aws.smithy.kotlin.runtime.util.PlatformEnvironProvider

private const val CUSTOM_METADATA_ENV_PREFIX = "AWS_CUSTOM_METADATA_"
private const val CUSTOM_METADATA_PROP_PREFIX = "aws.customMetadata."

/**
 * Operation context element for adding additional metadata to the `User-Agent` header string.
 *
 * Access via extension property [ExecutionContext.customUserAgentMetadata]
 */
public class CustomUserAgentMetadata(
    extras: Map<String, String> = mapOf(),
    typedExtras: List<TypedUserAgentMetadata> = listOf(),
) {
    internal val extras: MutableMap<String, String>
    internal val typedExtras: MutableList<TypedUserAgentMetadata>

    init {
        this.extras = extras.toMutableMap()
        this.typedExtras = typedExtras.toMutableList()
    }

    internal companion object {
        val ContextKey: AttributeKey<CustomUserAgentMetadata> = AttributeKey("CustomUserAgentMetadata")

        internal fun fromEnvironment(provider: PlatformEnvironProvider): CustomUserAgentMetadata {
            fun Map<String, String>.findAndStripKeyPrefix(prefix: String) = this
                .filterKeys { it.startsWith(prefix) }
                .mapKeys { (key, _) -> key.substring(prefix.length) }

            val envVarMap = provider.getAllEnvVars().findAndStripKeyPrefix(CUSTOM_METADATA_ENV_PREFIX)
            val propMap = provider.getAllProperties().findAndStripKeyPrefix(CUSTOM_METADATA_PROP_PREFIX)

            return CustomUserAgentMetadata(extras = envVarMap + propMap)
        }
    }

    /**
     * Add additional key-value pairs of metadata to the request. These will show up as `md/key#value` when sent.
     */
    public fun add(key: String, value: String) {
        extras[key] = value
    }

    @InternalSdkApi
    public fun add(metadata: TypedUserAgentMetadata) {
        typedExtras.add(metadata)
    }

    public operator fun plus(other: CustomUserAgentMetadata): CustomUserAgentMetadata = CustomUserAgentMetadata(extras + other.extras, typedExtras + other.typedExtras)
}

/**
 * Get the [CustomUserAgentMetadata] instance to append additional context to the generated `User-Agent` string.
 */
public val ExecutionContext.customUserAgentMetadata: CustomUserAgentMetadata
    get() = computeIfAbsent(CustomUserAgentMetadata.ContextKey) { CustomUserAgentMetadata() }

/**
 * Marker interface for addition of classified metadata types (e.g. [ConfigMetadata] or [FeatureMetadata]).
 */
@InternalSdkApi
public sealed interface TypedUserAgentMetadata

/**
 * Feature metadata
 * @property name The name of the feature
 * @property version Optional version of the feature (if independently versioned)
 */
@InternalSdkApi
public data class FeatureMetadata(val name: String, val version: String? = null) : TypedUserAgentMetadata {
    override fun toString(): String = uaPair("ft", name, version)
}

/**
 * Configuration metadata
 * @property name The configuration property name (e.g. "retry-mode")
 * @property value The property value (e.g. "standard")
 */
@InternalSdkApi
public data class ConfigMetadata(val name: String, val value: String) : TypedUserAgentMetadata {
    override fun toString(): String = uaPair("cfg", name, value.takeUnless { it.equals("true", ignoreCase = true) })
}
