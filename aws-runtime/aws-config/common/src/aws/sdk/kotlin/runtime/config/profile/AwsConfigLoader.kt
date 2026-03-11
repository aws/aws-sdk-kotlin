/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.io.internal.SdkDispatchers
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import aws.smithy.kotlin.runtime.telemetry.trace.withSpan
import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Load the shared AWS configuration specified in local configuration files.
 *
 * This function performs no caching. File I/O will be performed with each call.
 *
 * @param platform Platform from which to resolve configuration environment
 * @param profileNameOverride Optional profile name to use as the active profile
 * @param configurationSource Optional configuration source to use
 * @return A [AwsSharedConfig] instance
 */
@InternalSdkApi
public suspend fun loadAwsSharedConfig(
    platform: PlatformProvider,
    profileNameOverride: String? = null,
    configurationSource: AwsConfigurationSource? = null,
): AwsSharedConfig = withSpan("AwsSharedConfig", "loadAwsSharedConfig") {
    // Determine active profile and location of configuration files
    val source = configurationSource ?: resolveConfigSource(platform, profileNameOverride)
    val logger = coroutineContext.logger("AwsConfigParser")

    // merged AWS configuration based on optional configuration and credential file contents
    withContext(SdkDispatchers.IO) {
        mergeFiles(
            parse(
                logger,
                FileType.CONFIGURATION,
                platform.readFileOrNull(source.configPath)?.decodeToString(),
            ),
            parse(
                logger,
                FileType.CREDENTIAL,
                platform.readFileOrNull(source.credentialsPath)?.decodeToString(),
            ),
        ).toSharedConfig(source)
    }
}

// Merge multiple independently parsed section maps
internal fun mergeFiles(vararg maps: TypedSectionMap): TypedSectionMap = buildMap {
    ConfigSectionType.values().forEach { sectionType ->
        val sectionMaps = maps.mapNotNull { map -> map[sectionType] }
        val merged = mergeSectionMaps(sectionMaps)
        put(sectionType, merged)
    }
}

private fun mergeSectionMaps(maps: List<SectionMap>): SectionMap = buildMap {
    maps.forEach { map ->
        map.entries.forEach { entry ->
            val existingProps = get(entry.key)?.properties ?: emptyMap()
            val section = ConfigSection(entry.key, existingProps + entry.value.properties, entry.value.sectionType)
            put(section.name, section)
        }
    }
}

/**
 * Specifies the active profile and configured (may not actually exist) locations of configuration files.
 */
@InternalSdkApi
public data class AwsConfigurationSource(val profile: String, val configPath: String, val credentialsPath: String)

/**
 * Determine the source of AWS configuration
 */
internal fun resolveConfigSource(
    platform: PlatformProvider,
    profileNameOverride: String? = null,
) = AwsConfigurationSource(
    // If the user does not specify the profile to be used, the default profile must be used by the SDK.
    // The default profile must be overridable using the AWS_PROFILE environment variable.
    profileNameOverride ?: AwsSdkSetting.AwsProfile.resolve(platform) ?: Literals.DEFAULT_PROFILE,
    normalizePath(FileType.CONFIGURATION.path(platform), platform),
    normalizePath(FileType.CREDENTIAL.path(platform), platform),
)

/**
 * Expands paths prefixed with '~' to the home directory under which the SDK is running.
 *
 * User Home Resolution: The user's home directory must be resolved when the file location starts with ~/ or ~
 * followed by the operating system's default path separator by checking the following variables, in order:
 *
 * 1. (All Platforms) The HOME environment variable.
 * 2. (Windows Platforms) The USERPROFILE environment variable.
 * 3. (Windows Platforms) The HOMEDRIVE environment variable prepended to the HOMEPATH environment variable (ie. $HOMEDRIVE$HOMEPATH).
 * 4. (Optional) A language-specific home path resolution function or variable.
 */
internal fun normalizePath(path: String, platform: PlatformProvider): String {
    if (!path.trim().startsWith('~')) return path

    val home = resolveHomeDir(platform) ?: error("Unable to determine user home directory")

    return home + path.substring(1)
}

/**
 * Load the user's home directory based on the priorities:
 *
 * If the implementation cannot determine the customer's platform, the USERPROFILE and HOMEDRIVE + HOMEPATH environment
 * variables must be checked for all platforms. If the implementation can determine the customer's platform, the
 * USERPROFILE and HOMEDRIVE + HOMEPATH environment variables must not be checked on non-windows platforms.
 *
 * @param
 * @return the absolute path of the home directory from which the SDK is running, or null if unspecified by environment.
 */
private fun resolveHomeDir(platform: PlatformProvider): String? = with(platform) {
    when (osInfo().family) {
        OsFamily.Unknown, OsFamily.Windows ->
            getenv("HOME")
                ?: getenv("USERPROFILE")
                ?: (getenv("HOMEDRIVE") to getenv("HOMEPATH")).concatOrNull()
                ?: getProperty("user.home")
        else ->
            getenv("HOME")
                ?: getProperty("user.home")
    }
}

private fun Pair<String?, String?>.concatOrNull() = if (first != null && second != null) first + second else null
