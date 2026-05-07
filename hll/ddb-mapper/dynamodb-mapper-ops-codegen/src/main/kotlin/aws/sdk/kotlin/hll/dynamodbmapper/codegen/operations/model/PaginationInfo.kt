/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.Member
import aws.sdk.kotlin.hll.codegen.model.Operation
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
        /**
         * Derive [PaginationInfo] for the given request and response projections, or `null` if the given
         * request/response aren't used in a paginated operation
         */
        fun forRequestResponse(requestProjection: KeyProjection, responseProjection: KeyProjection): PaginationInfo? {
            val inputTokens = requestProjection.findMembersByLowLevelName("exclusiveStartKey")
            val outputTokens = responseProjection.findMembersByLowLevelName("lastEvaluatedKey")
            assertTokensMatch(inputTokens, outputTokens)

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

private fun Structure.findMemberByLowLevelName(name: String): Member? = members.find { it.lowLevel.name == name }
private fun Structure.findMembersByLowLevelName(name: String): List<Member> = members.filter { it.lowLevel.name == name }
private fun KeyProjection.findMemberByLowLevelName(name: String): Member? = interfaceStruct.findMemberByLowLevelName(name)
private fun KeyProjection.findMembersByLowLevelName(name: String): List<Member> = interfaceStruct.findMembersByLowLevelName(name)

internal val Operation.isPaginated: Boolean
    get() {
        val inputTokens = request
            .keyProjections[KeyProjectionType.PARTITION_KEY]
            .findMembersByLowLevelName("exclusiveStartKey")

        val outputTokens = response
            .keyProjections[KeyProjectionType.PARTITION_KEY]
            .findMembersByLowLevelName("lastEvaluatedKey")

        assertTokensMatch(inputTokens, outputTokens)
        return inputTokens.isNotEmpty()
    }

private fun assertTokensMatch(inputTokens: List<Member>, outputTokens: List<Member>) {
    require(inputTokens.size == outputTokens.size) {
        buildString {
            append("Found mismatched input/output tokens for pagination. Input tokens: ")
            inputTokens.joinTo(this) { it.name }

            append(". Output tokens: ")
            outputTokens.joinTo(this) { it.name }
        }
    }
}
