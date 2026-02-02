/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.model.traits.testing

import software.amazon.smithy.model.node.ObjectNode
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.AnnotationTrait

/**
 * Indicates the annotated service should always return a failed response.
 * IMPORTANT: This trait is intended for use in integration or E2E tests only, not in real-life smoke tests that run
 * against a service endpoint.
 */
class TestFailedResponseTrait(node: ObjectNode) : AnnotationTrait(ID, node) {
    companion object {
        val ID: ShapeId = ShapeId.from("smithy.kotlin.traits#failedResponseTrait")
    }
}

/**
 * Indicates the annotated service should always return a success response.
 * IMPORTANT: This trait is intended for use in integration or E2E tests only, not in real-life smoke tests that run
 * against a service endpoint.
 */
class TestSuccessResponseTrait(node: ObjectNode) : AnnotationTrait(ID, node) {
    companion object {
        val ID: ShapeId = ShapeId.from("smithy.kotlin.traits#successResponseTrait")
    }
}
