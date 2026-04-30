/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "DynamoDbMapper annotations"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: DynamoDbMapper :: Annotations"
extra["moduleName"] = "aws.sdk.kotlin.hll.dynamodbmapper.annotations"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // For ValueConverter
                implementation(project(":hll:dynamodb-mapper:dynamodb-mapper"))
            }
        }
    }
}
