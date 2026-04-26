/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.cachedAuthFilePermissions
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Identifies a set of permissions to apply when writing cached auth files (e.g., during cache token refresh for SSO or
 * AWS Login credentials). These values have no effect on Windows.
 */
@InternalSdkApi
public enum class CachedAuthFilePermissions(public val posixOctal: String?) {
    /**
     * Specifies POSIX permissions `600`—the user has read/write permissions, everyone else has no permissions
     */
    USER_READ_WRITE("600"),

    /**
     * Specifies the OS-default POSIX permissions for new files in the given directory.
     *
     * On POSIX-compliant OSes (e.g., Linux and Mac), this is defined by [`umask`](https://en.wikipedia.org/wiki/Umask).
     * Most modern OSes default to a umask of `022`, which results in default permissions `644`—the user has read/write
     * permissions, everyone else has read permission only.
     */
    OS_DEFAULT(null),
}

/**
 * Attempts to resolve cachedAuthFilePermissions from the specified sources
 * @return a cachedAuthFilePermissions setting if found; otherwise, [CachedAuthFilePermissions.USER_READ_WRITE]
 */
@InternalSdkApi
public suspend fun resolveCachedAuthFilePermissions(
    platform: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile>?,
): CachedAuthFilePermissions = AwsSdkSetting.AwsCachedAuthFilePermissions.resolve(platform)
    ?: profile?.get()?.cachedAuthFilePermissions
    ?: CachedAuthFilePermissions.USER_READ_WRITE
