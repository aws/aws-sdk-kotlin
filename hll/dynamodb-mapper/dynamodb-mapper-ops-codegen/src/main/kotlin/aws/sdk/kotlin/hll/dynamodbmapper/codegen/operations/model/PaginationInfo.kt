/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.Member
import aws.sdk.kotlin.hll.codegen.model.Structure
import aws.sdk.kotlin.hll.codegen.model.lowLevel

/**
 * Identifies the [Member] instances of an operation's request and response which control pagination
 * @param requestToken The field for passing a pagination token into a request
 * @param responseToken The field for receiving a pagination token from a request
 * @param limit The field for limiting the number of returned results
 * @param items The field for getting the low-level items from each page of results
 */
internal data class PaginationInfo(
    val request: Structure,
    val requestBuilder: Structure?,
    val response: Structure,
    val tokens: List<PaginationToken>,
    val limit: Member,
    val items: Member,
) {
    internal companion object {
        private fun TypeFamily.findMemberByLowLevelName(name: String): Member? =
            interfaceStruct.members.find { it.lowLevel.name == name }

        private fun TypeFamily.findMembersByLowLevelName(name: String): List<Member>? =
            interfaceStruct.members.filter { it.lowLevel.name == name }

        fun forRequestResponse(requestTypeFamily: TypeFamily, responseTypeFamily: TypeFamily): PaginationInfo? {
            val inputTokens = requestTypeFamily.findMembersByLowLevelName("exclusiveStartKey") ?: return null
            val outputTokens = responseTypeFamily.findMembersByLowLevelName("lastEvaluatedKey") ?: return null
            require(inputTokens.size == outputTokens.size) {
                "Mismatched pagination: found ${inputTokens.size} input tokens but ${outputTokens.size} output tokens"
            }
            val tokens = (inputTokens zip outputTokens).map { (i, o) -> PaginationToken(i, o) }

            val limit = requestTypeFamily.findMemberByLowLevelName("limit") ?: return null
            val items = responseTypeFamily.findMemberByLowLevelName("items") ?: return null

            return PaginationInfo(
                requestTypeFamily.interfaceStruct,
                (requestTypeFamily as? TypeFamily.Concrete)?.builderStruct,
                responseTypeFamily.interfaceStruct,
                tokens,
                limit,
                items,
            )
        }
    }
}

internal data class PaginationToken(val input: Member, val output: Member)
