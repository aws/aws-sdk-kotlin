/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.endpoints

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.core.withBlock
import aws.smithy.kotlin.codegen.lang.KotlinTypes
import aws.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.model.node.ObjectNode

/**
 * Generates the list of service partitions from the partitions.json resource.
 */
class PartitionsGenerator(
    private val writer: KotlinWriter,
    private val partitionsSpec: ObjectNode,
) {
    companion object {
        fun getSymbol(settings: KotlinSettings): Symbol = buildSymbol {
            name = "Partitions"
            namespace = "${settings.pkg.name}.endpoints.internal"
        }
    }

    fun render() {
        renderPartitionFn()
        writer.write("")
        renderDefaultPartitions()
    }

    private fun renderPartitionFn() {
        writer.withBlock(
            "internal fun partition(region: #T?): #T? {",
            "}",
            KotlinTypes.String,
            AwsRuntimeTypes.Endpoint.Functions.PartitionConfig,
        ) {
            write("return #T(defaultPartitions, region)", AwsRuntimeTypes.Endpoint.Functions.partitionFn)
        }
    }

    private fun renderDefaultPartitions() {
        writer.withBlock("private val defaultPartitions = listOf(", ")") {
            val partitions = partitionsSpec.expectArrayMember("partitions")

            partitions.elements.forEach {
                renderPartition(it.expectObjectNode())
            }
        }
    }

    private fun renderPartition(partition: ObjectNode) {
        val baseConfig = partition.expectObjectMember("outputs")

        writer.withBlock("#T(", "),", AwsRuntimeTypes.Endpoint.Functions.Partition) {
            write("id = #S,", partition.expectStringMember("id").value)
            write("regionRegex = Regex(#S),", partition.expectStringMember("regionRegex").value)
            withBlock("regions = mapOf(", "),") {
                partition.expectObjectMember("regions").stringMap.entries.forEach { (k, v) ->
                    val regionConfig = v.expectObjectNode()

                    withBlock("#S to #T(", "),", k, AwsRuntimeTypes.Endpoint.Functions.PartitionConfig) {
                        regionConfig.getStringMember("name").ifPresent {
                            write("name = #S,", it.value)
                        }
                        regionConfig.getStringMember("dnsSuffix").ifPresent {
                            write("dnsSuffix = #S,", it.value)
                        }
                        regionConfig.getStringMember("dualStackDnsSuffix").ifPresent {
                            write("dualStackDnsSuffix = #S,", it.value)
                        }
                        regionConfig.getBooleanMember("supportsFIPS").ifPresent {
                            write("supportsFIPS = #L,", it.value)
                        }
                        regionConfig.getBooleanMember("supportsDualStack").ifPresent {
                            write("supportsDualStack = #L,", it.value)
                        }
                        regionConfig.getStringMember("implicitGlobalRegion").ifPresent {
                            write("implicitGlobalRegion = #S,", it.value)
                        }
                    }
                }
            }
            withBlock("baseConfig = #T(", "),", AwsRuntimeTypes.Endpoint.Functions.PartitionConfig) {
                write("name = #S,", baseConfig.expectStringMember("name").value)
                write("dnsSuffix = #S,", baseConfig.expectStringMember("dnsSuffix").value)
                write("dualStackDnsSuffix = #S,", baseConfig.expectStringMember("dualStackDnsSuffix").value)
                write("supportsFIPS = #L,", baseConfig.expectBooleanMember("supportsFIPS").value)
                write("supportsDualStack = #L,", baseConfig.expectBooleanMember("supportsDualStack").value)
                write("implicitGlobalRegion = #S,", baseConfig.expectStringMember("implicitGlobalRegion").value)
            }
        }
    }
}
