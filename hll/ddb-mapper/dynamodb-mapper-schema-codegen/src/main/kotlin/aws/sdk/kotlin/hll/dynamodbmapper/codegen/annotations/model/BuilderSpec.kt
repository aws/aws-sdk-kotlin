/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model

import aws.sdk.kotlin.hll.codegen.model.Member
import aws.sdk.kotlin.hll.codegen.model.Structure
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.rendering.BuilderRenderer

/**
 * The inputs to the shared [BuilderRenderer] for a [MappedItem]
 * @param builtStructure The structure being built. Only its (generic-argument-adjusted) type is relied upon; its
 * members are not, since they are derived from declared rather than mapped properties.
 * @param implementationType The type whose constructor the generated `build` method calls
 * @param members The builder's members, derived from the item's mapped properties
 */
internal data class BuilderSpec(
    val builtStructure: Structure,
    val implementationType: TypeRef,
    val members: Set<Member>,
) {
    /**
     * The name of the generated builder class, exactly as [BuilderRenderer] declares it
     */
    val name: String
        get() = BuilderRenderer.builderName(builtStructure)
}
