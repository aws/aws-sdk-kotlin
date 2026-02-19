/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.http

import aws.smithy.kotlin.runtime.util.PlatformProvider

// JVM language metadata extras, load these once
private val jvmMetadataExtras = lazy {
    val metadata = mutableMapOf(
        "javaVersion" to getSystemProperty("java.version"),
        "jvmName" to getSystemProperty("java.vm.name"),
        "jvmVersion" to getSystemProperty("java.vm.version"),
    )

    if (PlatformProvider.System.isAndroid) {
        // https://developer.android.com/reference/android/os/Build.VERSION
        val buildVersionCls = Class.forName("android.os.Build\$VERSION")
        val sdkIntField = buildVersionCls.getDeclaredField("SDK_INT")
        val sdkReleaseField = buildVersionCls.getDeclaredField("RELEASE")

        metadata["androidApiVersion"] = sdkIntField.getInt(null).toString()
        metadata["androidRelease"] = sdkReleaseField.get(null) as String
    }

    metadata
}

internal actual fun platformLanguageMetadata() = LanguageMetadata(extras = jvmMetadataExtras.value)

private fun getSystemProperty(name: String, defaultValue: String = "unknown"): String = runCatching { System.getProperty(name) }.getOrDefault(defaultValue)
