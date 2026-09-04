/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.sdk

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File

/**
 * Container for AWS service package settings.
 * Each service can optionally have a `services/<service>/package.json` file that is used
 * to control some aspect of code generation specific to that service
 */
@Serializable
data class PackageSettings(
    /**
     * The sdkId of the service. This is used as a check that the package settings are used on the correct service
     */
    @Required
    val sdkId: String,

    /**
     * Whether to enable generating an auth scheme resolver based on endpoint resolution (rare).
     */
    val enableEndpointAuthProvider: Boolean = false,
) {
    companion object {

        /**
         * Parse package settings from the given file path if it exists, otherwise return the default settings with
         * the given sdkId.
         */
        @OptIn(ExperimentalSerializationApi::class)
        fun fromFile(sdkId: String, packageSettingsFile: File): PackageSettings {
            if (!packageSettingsFile.exists()) return PackageSettings(sdkId)
            val settings = packageSettingsFile.inputStream().use { Json.decodeFromStream<PackageSettings>(it) }
            check(sdkId == settings.sdkId) { "${packageSettingsFile.absolutePath} `sdkId` from settings (${settings.sdkId}) does not match expected `$sdkId`" }
            return settings
        }
    }
}
