/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.gradle.kmp.kotlin

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "Common data mapping utilities used by AWS SDK for Kotlin's high level libraries"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: Mapping"
extra["moduleName"] = "aws.sdk.kotlin.hll.mapping.core"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.smithy.kotlin.runtime.core)
            }
        }
    }
}
