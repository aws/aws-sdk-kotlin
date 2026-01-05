/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization

import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.codegen.core.CodegenException
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.AbstractShapeBuilder
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.DefaultTrait
import software.amazon.smithy.model.transform.ModelTransformer
import software.amazon.smithy.utils.ToSmithyBuilder

/**
 * Removes the default value of certain shapes, and any member that target
 * those shapes,for certain services. These default values may cause
 * serialization, validation, or other unexpected issues.
 */
class RemoveDefaults : KotlinIntegration {
    // Service shape id -> Shape id of each root shape to remove default values from.
    private val removeDefaultsFrom: Map<ShapeId, Set<ShapeId>> = mapOf(
        "com.amazonaws.amplifyuibuilder#AmplifyUIBuilder" to setOf(
            "com.amazonaws.amplifyuibuilder#ListComponentsLimit",
            "com.amazonaws.amplifyuibuilder#ListFormsLimit",
            "com.amazonaws.amplifyuibuilder#ListThemesLimit",
        ),
        "com.amazonaws.drs#ElasticDisasterRecoveryService" to setOf(
            "com.amazonaws.drs#Validity",
            "com.amazonaws.drs#CostOptimizationConfiguration\$burstBalanceThreshold",
            "com.amazonaws.drs#CostOptimizationConfiguration\$burstBalanceDeltaThreshold",
            "com.amazonaws.drs#ListStagingAccountsRequest\$maxResults",
            "com.amazonaws.drs#StrictlyPositiveInteger",
            "com.amazonaws.drs#MaxResultsType",
            "com.amazonaws.drs#MaxResultsReplicatingSourceServers",
            "com.amazonaws.drs#LaunchActionOrder",
        ),
        "com.amazonaws.evidently#Evidently" to setOf(
            "com.amazonaws.evidently#ResultsPeriod",
        ),
        "com.amazonaws.location#LocationService" to setOf(
            "com.amazonaws.location#ListPlaceIndexesRequest\$MaxResults",
            "com.amazonaws.location#SearchPlaceIndexForSuggestionsRequest\$MaxResults",
            "com.amazonaws.location#PlaceIndexSearchResultLimit",
        ),
        "com.amazonaws.paymentcryptographydata#PaymentCryptographyDataPlane" to setOf(
            "com.amazonaws.paymentcryptographydata#IntegerRangeBetween4And12",
        ),
        "com.amazonaws.emrserverless#AwsToledoWebService" to setOf(
            "com.amazonaws.emrserverless#WorkerCounts",
        ),
        "com.amazonaws.s3control#AWSS3ControlServiceV20180820" to setOf(
            "com.amazonaws.s3control#PublicAccessBlockConfiguration\$BlockPublicAcls",
            "com.amazonaws.s3control#PublicAccessBlockConfiguration\$IgnorePublicAcls",
            "com.amazonaws.s3control#PublicAccessBlockConfiguration\$BlockPublicPolicy",
            "com.amazonaws.s3control#PublicAccessBlockConfiguration\$RestrictPublicBuckets",
        ),
        "com.amazonaws.iot#AWSIotService" to setOf(
            "com.amazonaws.iot#ThingConnectivity\$connected",
            "com.amazonaws.iot#UpdateProvisioningTemplateRequest\$enabled",
            "com.amazonaws.iot#CreateProvisioningTemplateRequest\$enabled",
            "com.amazonaws.iot#DescribeProvisioningTemplateResponse\$enabled",
            "com.amazonaws.iot#ProvisioningTemplateSummary\$enabled",
        ),
    ).map { (k, v) -> ShapeId.from(k) to v.map { ShapeId.from(it) }.toSet() }.toMap()

    override val order: Byte = 0

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        settings.service in removeDefaultsFrom

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val serviceId = settings.service
        val removeDefaultsFromShapes = removeDefaultsFrom[serviceId]
            ?: throw CodegenException("expected $serviceId in removed defaults map")
        return removeDefaults(model, removeDefaultsFromShapes)
    }

    fun removeDefaults(model: Model, fromShapes: Set<ShapeId>): Model {
        val removedRootDefaults: MutableSet<ShapeId> = HashSet()
        val removedRootDefaultsModel = ModelTransformer.create().mapShapes(model) {
            if (shouldRemoveRootDefault(it, fromShapes)) {
                removedRootDefaults.add(it.id)
                removeDefault(it)
            } else {
                it
            }
        }

        return ModelTransformer.create().mapShapes(removedRootDefaultsModel) {
            if (shouldRemoveMemberDefault(it, removedRootDefaults, fromShapes)) {
                removeDefault(it)
            } else {
                it
            }
        }
    }

    private fun shouldRemoveRootDefault(shape: Shape, removeDefaultsFrom: Set<ShapeId>): Boolean =
        shape !is MemberShape && shape.id in removeDefaultsFrom && shape.hasTrait<DefaultTrait>()

    private fun shouldRemoveMemberDefault(
        shape: Shape,
        removedRootDefaults: Set<ShapeId>,
        removeDefaultsFrom: Set<ShapeId>,
    ): Boolean = shape is MemberShape &&
        // Check the original set of shapes to remove for this shape id, to remove members that were in that set.
        (shape.target in removedRootDefaults || shape.id in removeDefaultsFrom) &&
        shape.hasTrait<DefaultTrait>()

    private fun removeDefault(shape: Shape): Shape =
        ((shape as ToSmithyBuilder<*>).toBuilder() as AbstractShapeBuilder<*, *>)
            .removeTrait(DefaultTrait.ID)
            .build()
}
