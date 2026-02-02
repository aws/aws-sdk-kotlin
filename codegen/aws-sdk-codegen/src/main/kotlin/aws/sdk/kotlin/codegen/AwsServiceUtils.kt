/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen

import aws.smithy.kotlin.codegen.model.expectTrait
import software.amazon.smithy.aws.traits.ServiceTrait
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Get the [sdkId](https://awslabs.github.io/smithy/1.0/spec/aws/aws-core.html#sdkid) from the (AWS) service shape
 */
val ServiceShape.sdkId: String
    get() = expectTrait<ServiceTrait>().sdkId
