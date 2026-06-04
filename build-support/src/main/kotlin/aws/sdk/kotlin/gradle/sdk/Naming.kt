/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.sdk

// The root namespace prefix for SDKs
const val SDK_PACKAGE_NAME_PREFIX: String = "aws.sdk.kotlin.services."

/**
 * Get the package name to use for a service from it's `sdkId`
 */
fun packageNameForService(sdkId: String): String = sdkId.replace(" ", "")
    .replace("-", "")
    .lowercase()
    .kotlinNamespace()

/**
 * Get the package namespace for a service from it's `sdkId`
 */
fun packageNamespaceForService(sdkId: String): String = "$SDK_PACKAGE_NAME_PREFIX${packageNameForService(sdkId)}"

/**
 * Remove characters invalid for Kotlin package namespace identifier
 */
fun String.kotlinNamespace(): String = split(".")
    .joinToString(separator = ".") { segment -> segment.filter { it.isLetterOrDigit() } }

/**
 * Convert an sdkId to the module/artifact name to use
 */
internal fun sdkIdToArtifactName(sdkId: String): String = sdkId.replace(" ", "").replace("-", "").lowercase()

/**
 * Maps an sdkId from a model to the local filename to use. This logic has to match the logic used by
 * catapult! See AwsSdkCatapultWorkspaceTools:lib/source/merge/smithy-model-handler.ts
 */
private val whitespacePattern = """\s+""".toRegex()
fun sdkIdToModelFilename(sdkId: String): String = sdkId.trim().replace(whitespacePattern, "-").lowercase()

// FIXME - replace with case utils from smithy-kotlin once we verify we can change the implementation
private fun String.lowercaseAndCapitalize() = lowercase().replaceFirstChar(Char::uppercaseChar)
private val wordBoundary = "[^a-zA-Z0-9]+".toRegex()
private fun String.pascalCase(): String = split(wordBoundary).pascalCase()
fun List<String>.pascalCase() = joinToString(separator = "") { it.lowercaseAndCapitalize() }

private const val BRAZIL_GROUP_NAME = "AwsSdkKotlin"

/**
 * Maps an sdkId from a model to the brazil package name to use
 */
fun sdkIdToBrazilName(sdkId: String): String = "${BRAZIL_GROUP_NAME}${sdkId.pascalCase()}"
