/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import aws.smithy.kotlin.codegen.core.GradleConfiguration
import aws.smithy.kotlin.codegen.core.KotlinDependency
import aws.smithy.kotlin.codegen.core.isImplicit
import aws.smithy.kotlin.codegen.core.isValidVersion
import software.amazon.smithy.codegen.core.CodegenException

// root namespace for the AWS client-runtime
const val AWS_CLIENT_RT_ROOT_NS = "aws.sdk.kotlin.runtime"

private fun getDefaultRuntimeVersion(): String {
    // generated as part of the build, see smithy-kotlin/codegen/aws-codegen/build.gradle.kts
    try {
        val version = object {}.javaClass.getResource("sdk-version.txt")?.readText() ?: throw CodegenException("sdk-version.txt does not exist")
        check(isValidVersion(version)) { "Version parsed from sdk-version.txt '$version' is not a valid version string" }
        return version
    } catch (ex: Exception) {
        throw CodegenException("failed to load sdk-version.txt which sets the default aws-client-runtime version", ex)
    }
}

// publishing info
const val AWS_CLIENT_RT_GROUP: String = "aws.sdk.kotlin"

// note: this version doesn't really matter since we substitute it for project dependency notation
// when generating client build files (it is used by protocol tests though)
val AWS_CLIENT_RT_VERSION: String = getDefaultRuntimeVersion()

/**
 * Container object for AWS specific dependencies
 */
object AwsKotlinDependency {
    val AWS_CORE = KotlinDependency(GradleConfiguration.Api, AWS_CLIENT_RT_ROOT_NS, AWS_CLIENT_RT_GROUP, "aws-core", AWS_CLIENT_RT_VERSION)
    val AWS_CONFIG = KotlinDependency(GradleConfiguration.Api, AWS_CLIENT_RT_ROOT_NS, AWS_CLIENT_RT_GROUP, "aws-config", AWS_CLIENT_RT_VERSION)
    val AWS_ENDPOINT = KotlinDependency(GradleConfiguration.Api, "$AWS_CLIENT_RT_ROOT_NS.endpoint", AWS_CLIENT_RT_GROUP, "aws-endpoint", AWS_CLIENT_RT_VERSION)
    val AWS_HTTP = KotlinDependency(GradleConfiguration.Implementation, "$AWS_CLIENT_RT_ROOT_NS.http", AWS_CLIENT_RT_GROUP, "aws-http", AWS_CLIENT_RT_VERSION)
}

// remap aws-sdk-kotlin dependencies to project notation
// NOTE: the lazy wrapper is required here, see: https://github.com/awslabs/aws-sdk-kotlin/issues/95
private val sameProjectDeps: Map<KotlinDependency, String> by lazy {
    mapOf(
        AwsKotlinDependency.AWS_CORE to """project(":aws-runtime:aws-core")""",
        AwsKotlinDependency.AWS_CONFIG to """project(":aws-runtime:aws-config")""",
        AwsKotlinDependency.AWS_ENDPOINT to """project(":aws-runtime:aws-endpoint")""",
        AwsKotlinDependency.AWS_HTTP to """project(":aws-runtime:aws-http")""",
    )
}

internal fun KotlinDependency.dependencyNotation(allowProjectNotation: Boolean = true): String? {
    val dep = this
    return when {
        dep.isImplicit -> null
        allowProjectNotation && sameProjectDeps.contains(dep) -> "${dep.config.kmpName}(${sameProjectDeps[dep]})"
        else -> "${dep.config.kmpName}(\"${dep.group}:${dep.artifact}:${dep.version}\")"
    }
}
