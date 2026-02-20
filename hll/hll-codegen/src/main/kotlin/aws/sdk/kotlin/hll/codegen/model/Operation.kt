/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.model

import aws.sdk.kotlin.hll.codegen.util.capitalizeFirstChar
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.emptyAttributes
import aws.smithy.kotlin.runtime.collections.get
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/**
 * Describes a service operation (i.e., API method)
 * @param methodName The name of the operation as a code-suitable method name. For example, `getItem` is a suitable
 * method name in Kotlin, whereas `GetItem` is not (improperly cased) nor is `get item` (contains a space).
 * @param request The [Structure] for requests/inputs to this operation
 * @param response The [Structure] for responses/output from this operation
 * @param attributes An [Attributes] collection for associating typed attributes with this operation
 */
@InternalSdkApi
public data class Operation(
    val methodName: String,
    val request: Structure,
    val response: Structure,
    val attributes: Attributes = emptyAttributes(),
) {
    /**
     * The capitalized name of this operation's [methodName]. For example, if [methodName] is `getItem` then [name]
     * would be `GetItem`.
     */
    public val name: String = methodName.capitalizeFirstChar // e.g., "GetItem" vs "getItem"

    @InternalSdkApi
    public companion object {
        /**
         * Derive an [Operation] from a [KSFunctionDeclaration]
         */
        public fun from(declaration: KSFunctionDeclaration): Operation = Operation(
            methodName = declaration.simpleName.getShortName(),
            request = Structure.from(declaration.parameters.single().type),
            response = Structure.from(declaration.returnType!!),
        )
    }
}

/**
 * Gets the low-level [Operation] equivalent for this high-level operation
 */
@InternalSdkApi
public val Operation.lowLevel: Operation
    get() = attributes[ModelAttributes.LowLevelOperation]
