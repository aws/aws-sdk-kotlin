/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.core.RuntimeTypePackage

/**
 * Commonly used AWS runtime types. Provides a single definition of a runtime symbol such that codegen isn't littered
 * with inline symbol creation which makes refactoring of the runtime more difficult and error-prone.
 */
object AwsRuntimeTypes {
    object Core : RuntimeTypePackage(AwsKotlinDependency.AWS_CORE) {
        val AwsErrorMetadata = symbol("AwsErrorMetadata")
        val ClientException = symbol("ClientException")
        val mergeSequential = symbol("mergeSequential")

        object Client : RuntimeTypePackage(AwsKotlinDependency.AWS_CORE, "client") {
            val AwsClientOption = symbol("AwsClientOption")
            val AwsSdkClientConfig = symbol("AwsSdkClientConfig")
        }
    }

    object Endpoint : RuntimeTypePackage(AwsKotlinDependency.AWS_ENDPOINT) {
        object Functions : RuntimeTypePackage(AwsKotlinDependency.AWS_ENDPOINT, "functions") {
            val partitionFn = symbol("partition")
            val Partition = symbol("Partition")
            val PartitionConfig = symbol("PartitionConfig")
            val parseArn = symbol("parseArn")
            val Arn = symbol("Arn")
            val isVirtualHostableS3Bucket = symbol("isVirtualHostableS3Bucket")
        }
    }

    object Config : RuntimeTypePackage(AwsKotlinDependency.AWS_CONFIG) {
        val AbstractAwsSdkClientFactory = symbol("AbstractAwsSdkClientFactory", "config")

        object Endpoints : RuntimeTypePackage(AwsKotlinDependency.AWS_CONFIG, "config.endpoints") {
            val AccountIdEndpointMode = symbol("AccountIdEndpointMode")
            val resolveEndpointUrl = symbol("resolveEndpointUrl")
            val resolveAccountId = symbol("resolveAccountId")
            val resolveAccountIdEndpointMode = symbol("resolveAccountIdEndpointMode")
        }

        object Profile : RuntimeTypePackage(AwsKotlinDependency.AWS_CONFIG, "config.profile") {
            val AwsSharedConfig = symbol("AwsSharedConfig")
            val AwsProfile = symbol("AwsProfile")
        }

        object Credentials : RuntimeTypePackage(AwsKotlinDependency.AWS_CONFIG, "auth.credentials") {
            val DefaultChainCredentialsProvider = symbol("DefaultChainCredentialsProvider")
            val DefaultChainBearerTokenProvider = symbol("DefaultChainBearerTokenProvider")
            val StaticCredentialsProvider = symbol("StaticCredentialsProvider")
            val manage = symbol("manage", "auth.credentials.internal", isExtension = true)
        }
    }

    object Http : RuntimeTypePackage(AwsKotlinDependency.AWS_HTTP) {
        object Interceptors : RuntimeTypePackage(AwsKotlinDependency.AWS_HTTP, "interceptors") {
            val AddUserAgentMetadataInterceptor = symbol("AddUserAgentMetadataInterceptor")
        }

        object Retries {
            val AwsRetryPolicy = symbol("AwsRetryPolicy", "retries")
        }

        object Middleware {
            val AwsRetryHeaderMiddleware = symbol("AwsRetryHeaderMiddleware", "middleware")
        }
    }
}
