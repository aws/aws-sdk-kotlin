/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.codegen.utils

import aws.sdk.kotlin.hll.codegen.model.Member
import aws.sdk.kotlin.hll.codegen.model.Operation
import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.MappingType
import aws.sdk.kotlin.services.s3.S3Client
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.Resolver

internal fun Resolver.operationMembers(
    operationName: String,
    type: MappingType,
    relevantMembers: Set<String>,
): List<Member> = Operation.from(
    this
        .getClassDeclarationByName<S3Client>()!!
        .getDeclaredFunctions()
        .find { it.simpleName.getShortName().equals(operationName, ignoreCase = true) }
        ?: throw S3TransferManagerCodegenException("Operation $operationName not found"),
)
    .let {
        if (type == MappingType.REQUEST) {
            it.request
        } else {
            it.response
        }
    }
    .members
    .filter { member ->
        relevantMembers.any { it.equals(member.name, ignoreCase = true) }
    }
