/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * Identifies a set of restricted permissions to apply when writing cached auth files (e.g., during cache token refresh
 * for SSO or AWS Login credentials). These values have no effect on Windows.
 */
@InternalSdkApi
public enum class RestrictFilePermissions(public val posixOctal: String?) {
    /**
     * Specifies POSIX permissions `600`—the user has read/write permissions, everyone else has no permissions
     */
    USER_READ_WRITE("600"),

    /**
     * Specifies no restrictions for new files in the given directory. This typically means that OS-default permissions
     * will be applied.
     *
     * On POSIX-compliant OSes (e.g., Linux and Mac), this is defined by [`umask`](https://en.wikipedia.org/wiki/Umask).
     * Most modern OSes default to a umask of `022`, which results in default permissions `644`—the user has read/write
     * permissions, everyone else has read permission only.
     */
    UNRESTRICTED(null),
}
