/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.sdk

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File

/**
 * Manifest containing additional metadata about services.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PackageManifest(
    val packages: List<PackageMetadata>,
) {

    val bySdkId: Map<String, PackageMetadata> = packages.associateBy(PackageMetadata::sdkId)
    companion object {
        fun fromFile(file: File): PackageManifest = file.inputStream().use {
            Json.decodeFromStream<PackageManifest>(it)
        }
    }
}

/**
 * Validate the package manifest for errors throwing an exception if any exist.
 */
fun PackageManifest.validate() {
    val distinct = mutableMapOf<String, PackageMetadata>()
    val errors = mutableListOf<String>()
    packages.forEach {
        val existing = distinct[it.sdkId]
        if (existing != null) {
            errors.add("multiple packages with same sdkId `${it.sdkId}`: first: $existing; second: $it")
        }
        distinct[it.sdkId] = it
    }

    check(errors.isEmpty()) { errors.joinToString(separator = "\n") }
}

/**
 * Per/package metadata stored with the repository.
 *
 * @param sdkId the unique SDK ID from the model this metadata applies to
 * @param namespace the package namespace to use as the root namespace when generating code for this package
 * @param artifactName the Maven artifact name (i.e. the 'A' in 'GAV' coordinates)
 * @param brazilName the internal Brazil package name for this package
 */
@Serializable
data class PackageMetadata(
    public val sdkId: String,
    public val namespace: String,
    public val artifactName: String,
    public val brazilName: String,
) {
    companion object {

        /**
         * Create a new [PackageMetadata] from inferring values using the given sdkId
         */
        fun from(sdkId: String): PackageMetadata = PackageMetadata(
            sdkId,
            packageNamespaceForService(sdkId),
            sdkIdToArtifactName(sdkId),
            sdkIdToBrazilName(sdkId),
        )
    }
}
