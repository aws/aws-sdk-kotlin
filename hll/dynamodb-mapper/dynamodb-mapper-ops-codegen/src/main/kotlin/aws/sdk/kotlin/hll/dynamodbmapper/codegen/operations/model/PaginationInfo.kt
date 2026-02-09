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
 * @param request The request/input [Structure] for the paginated operation
 * @param requestBuilder The builder [Structure] for [request]
 * @param response The response/output [Structure] for the paginated operation
 * @param tokens A collection of paired input/output token fields which are used to pass the pagination cursor between
 * the caller and the service. This list may contain multiple members if multiple fields are used for pagination control
 * (e.g., on a composite-key projected type).
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
    companion object {
        private fun KeyProjection.findMemberByLowLevelName(name: String): Member? =
            interfaceStruct.members.find { it.lowLevel.name == name }

        private fun KeyProjection.findMembersByLowLevelName(name: String): List<Member>? =
            interfaceStruct.members.filter { it.lowLevel.name == name }

        /**
         * Derive [PaginationInfo] for the given request and response projections, or `null` if the given
         * request/response aren't used in a paginated operation
         */
        fun forRequestResponse(requestProjection: KeyProjection, responseProjection: KeyProjection): PaginationInfo? {
            val inputTokens = requestProjection.findMembersByLowLevelName("exclusiveStartKey") ?: return null
            val outputTokens = responseProjection.findMembersByLowLevelName("lastEvaluatedKey") ?: return null
            require(inputTokens.size == outputTokens.size) {
                "Mismatched pagination: found ${inputTokens.size} input tokens but ${outputTokens.size} output tokens"
            }
            val tokens = (inputTokens zip outputTokens).map { (i, o) -> PaginationToken(i, o) }

            val limit = requestProjection.findMemberByLowLevelName("limit") ?: return null
            val items = responseProjection.findMemberByLowLevelName("items") ?: return null

            return PaginationInfo(
                requestProjection.interfaceStruct,
                requestProjection.builderStruct,
                responseProjection.interfaceStruct,
                tokens,
                limit,
                items,
            )
        }
    }
}

/**
 * A logical pagination token pair
 * @param input The input token member (e.g., `exclusiveStartKey`)
 * @param output The output token member (e.g., `lastEvaluatedKey`)
 */
internal data class PaginationToken(val input: Member, val output: Member)
