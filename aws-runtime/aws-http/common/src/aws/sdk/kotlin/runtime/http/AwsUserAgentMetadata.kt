/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.http

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.http.operation.ConfigMetadata
import aws.sdk.kotlin.runtime.http.operation.CustomUserAgentMetadata
import aws.sdk.kotlin.runtime.http.operation.FeatureMetadata
import aws.smithy.kotlin.runtime.util.*
import kotlin.jvm.JvmInline

internal const val AWS_EXECUTION_ENV = "AWS_EXECUTION_ENV"
public const val AWS_APP_ID_ENV: String = "AWS_SDK_UA_APP_ID"
private const val USER_AGENT_SPEC_VERSION = "2.1"
public const val BUSINESS_METRICS_MAX_LENGTH: Int = 1024

// non-standard environment variables/properties
public const val AWS_APP_ID_PROP: String = "aws.userAgentAppId"
internal const val FRAMEWORK_METADATA_ENV = "AWS_FRAMEWORK_METADATA"
internal const val FRAMEWORK_METADATA_PROP = "aws.frameworkMetadata"

/**
 * Metadata used to populate the `User-Agent` and `x-amz-user-agent` headers
 */
public data class AwsUserAgentMetadata(
    val sdkMetadata: SdkMetadata,
    val apiMetadata: ApiMetadata,
    val osMetadata: OsMetadata,
    val languageMetadata: LanguageMetadata,
    val execEnvMetadata: ExecutionEnvMetadata? = null,
    val frameworkMetadata: FrameworkMetadata? = null,
    val appId: String? = null,
    val customMetadata: CustomUserAgentMetadata? = null,
) {
    public companion object {
        /**
         * Load user agent configuration data from the current environment
         */
        public fun fromEnvironment(
            apiMeta: ApiMetadata,
            appId: String? = null,
        ): AwsUserAgentMetadata = loadAwsUserAgentMetadataFromEnvironment(PlatformProvider.System, apiMeta, appId)
    }

    /**
     * New-style user agent header value for `x-amz-user-agent`
     */
    val xAmzUserAgent: String
        /* ABNF for the user agent:
            ua-string:
              sdk-metadata RWS
              [internal-metadata RWS]
              ua-metadata
              [api-metadata RWS]
              os-metadata RWS
              language-metadata RWS
              [env-metadata RWS]
                     ; ordering is not strictly required in the following section
         *(config-metadata RWS)
              [appId]
         *(feat-metadata RWS)
         *(framework-metadata RWS)
         */
        get() = buildList {
            add(sdkMetadata)
            customMetadata?.extras?.takeIf { it.containsKey("internal") }?.let { add("md/internal") }
            add(uaPair("ua", USER_AGENT_SPEC_VERSION))
            add(apiMetadata)
            add(osMetadata)
            add(languageMetadata)
            execEnvMetadata?.let(::add)
            customMetadata?.typedExtras?.filterIsInstance<ConfigMetadata>()?.forEach(::add)
            appId?.let { add(uaPair("app", value = it)) }
            customMetadata?.typedExtras?.filterIsInstance<FeatureMetadata>()?.forEach(::add)
            frameworkMetadata?.let(::add)

            customMetadata?.extras?.let {
                val wrapper = AdditionalMetadata(it.filterKeys { it != "internal" })
                add("$wrapper")
            }
        }.joinToString(separator = " ")

    /**
     * Legacy user agent header value for `UserAgent`
     */
    val userAgent: String
        get() = "$sdkMetadata"
}

internal fun loadAwsUserAgentMetadataFromEnvironment(platform: PlatformProvider, apiMeta: ApiMetadata, appIdValue: String? = null): AwsUserAgentMetadata {
    val sdkMeta = SdkMetadata("kotlin", apiMeta.version)
    val osInfo = platform.osInfo()
    val osMetadata = OsMetadata(osInfo.family, osInfo.version)
    val langMeta = platformLanguageMetadata()
    val appId = appIdValue ?: platform.getProperty(AWS_APP_ID_PROP) ?: platform.getenv(AWS_APP_ID_ENV)

    val frameworkMetadata = FrameworkMetadata.fromEnvironment(platform)
    val customMetadata = CustomUserAgentMetadata.fromEnvironment(platform)

    return AwsUserAgentMetadata(
        sdkMeta,
        apiMeta,
        osMetadata,
        langMeta,
        detectExecEnv(platform),
        frameworkMetadata = frameworkMetadata,
        appId = appId,
        customMetadata = customMetadata,
    )
}

/**
 * Wrapper around additional metadata kv-pairs that handles formatting
 */
@JvmInline
internal value class AdditionalMetadata(private val extras: Map<String, String>) {
    override fun toString(): String = extras.entries.joinToString(separator = " ") { entry ->
        uaPair("md", entry.key, entry.value.takeUnless { it.equals("true", ignoreCase = true) })
    }
}

/**
 * SDK metadata
 * @property name The SDK (language) name
 * @property version The SDK version
 */
@InternalSdkApi
public data class SdkMetadata(val name: String, val version: String) {
    override fun toString(): String = uaPair("aws-sdk-$name", version)
}

/**
 * API metadata
 * @property serviceId The service ID (sdkId) in use (e.g. "Api Gateway")
 * @property version The version of the client (note this may be the same as [SdkMetadata.version] for SDK's
 * that don't independently version clients from one another.
 */
@InternalSdkApi
public data class ApiMetadata(val serviceId: String, val version: String) {
    override fun toString(): String {
        val formattedServiceId = serviceId.replace(" ", "-").lowercase()
        return uaPair("api", formattedServiceId, version)
    }
}

/**
 * Operating system metadata
 */
@InternalSdkApi
public data class OsMetadata(val family: OsFamily, val version: String? = null) {
    override fun toString(): String {
        // os-family = windows / linux / macos / android / ios / other
        val familyStr = when (family) {
            OsFamily.Unknown -> "other"
            else -> family.toString()
        }
        return uaPair("os", familyStr, version)
    }
}

/**
 * Programming language metadata
 * @property version The kotlin version in use
 * @property extras Additional key value pairs appropriate for the language/runtime (e.g.`jvmVm=OpenJdk`, etc)
 */
@InternalSdkApi
public data class LanguageMetadata(
    val version: String = KotlinVersion.CURRENT.toString(),
    // additional metadata key/value pairs
    val extras: Map<String, String> = emptyMap(),
) {
    override fun toString(): String = buildString {
        append(uaPair("lang", "kotlin", version))
        if (extras.isNotEmpty()) {
            val wrapper = AdditionalMetadata(extras)
            append(" $wrapper")
        }
    }
}

// provide platform specific metadata
internal expect fun platformLanguageMetadata(): LanguageMetadata

/**
 * Execution environment metadata
 * @property name The execution environment name (e.g. "lambda")
 */
@InternalSdkApi
public data class ExecutionEnvMetadata(val name: String) {
    override fun toString(): String = uaPair("exec-env", value = name)
}

/**
 * Framework metadata (e.g. name = "amplify" version = "1.2.3")
 * @property name The framework name
 * @property version The framework version
 */
@InternalSdkApi
public data class FrameworkMetadata(val name: String, val version: String) {
    internal companion object {
        internal fun fromEnvironment(provider: PlatformEnvironProvider): FrameworkMetadata? {
            val kvPair = provider.getProperty(FRAMEWORK_METADATA_PROP) ?: provider.getenv(FRAMEWORK_METADATA_ENV)
            return kvPair?.let {
                val kv = kvPair.split(':', limit = 2)
                check(kv.size == 2) { "Invalid value for FRAMEWORK_METADATA: $kvPair; must be of the form `name:version`" }
                FrameworkMetadata(kv[0], kv[1])
            }
        }
    }

    override fun toString(): String = uaPair("lib", name, version)
}

private fun detectExecEnv(platform: PlatformEnvironProvider): ExecutionEnvMetadata? =
    platform.getenv(AWS_EXECUTION_ENV)?.let {
        ExecutionEnvMetadata(it)
    }

// token_no_hash = 1*tchar_no_hash
// tchar_no_hash = "!" / "$" / "%" / "&" / "'" / "*" / "+" / "-" / "." /
//                 "^" / "_" / "`" / "|" / "~" / DIGIT / ALPHA
private val VALID_TCHAR_NO_HASH = setOf('!', '$', '%', '&', '\'', '*', '+', '-', '.', '^', '_', '`', '|', '~')
private val VALID_TCHAR = VALID_TCHAR_NO_HASH + '#'

internal fun uaPair(category: String, key: String? = null, value: String? = null): String = when {
    key == null && value == null -> category.encodeUaName()
    value == null -> "${category.encodeUaName()}/${key!!.encodeUaName()}"
    key == null -> "${category.encodeUaName()}/${value.encodeUaValue()}"
    else -> "${category.encodeUaName()}/${key.encodeUaName()}#${value.encodeUaValue()}"
}

private fun String.encodeUaName() = encodeUaString(VALID_TCHAR_NO_HASH)
private fun String.encodeUaValue() = encodeUaString(VALID_TCHAR)

/**
 * Encode a UA string, allowing only A-Z, a-z, 0-9, and characters in `charSet`.
 */
private fun String.encodeUaString(charSet: Set<Char>): String {
    val str = this
    return buildString(str.length) {
        for (chr in str) {
            when (chr) {
                ' ' -> append("_")
                in 'a'..'z', in 'A'..'Z', in '0'..'9', in charSet -> append(chr)
                else -> continue
            }
        }
    }
}
