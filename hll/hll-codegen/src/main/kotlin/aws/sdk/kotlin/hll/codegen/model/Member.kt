/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.model

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.collections.*
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/**
 * Describes a member (i.e., component field, attribute, property, etc.) of a [Structure]
 * @param name The name of the member inside its parent [Structure]
 * @param type The [Type] of the member
 * @param mutable Whether the member is a mutable (`var`) property or an immutable (`val`) property
 * @param attributes An [Attributes] collection for associating typed attributes with this member
 */
@InternalSdkApi
public data class Member(
    val name: String,
    val type: Type,
    val mutable: Boolean = false,
    override val attributes: Attributes = emptyAttributes(),
) : HasAttributes {
    @InternalSdkApi
    public companion object {
        /**
         * Derive a [Member] from a [KSPropertyDeclaration]
         */
        public fun from(
            prop: KSPropertyDeclaration,
            additionalAttributes: AttributesBuilder.() -> Unit = { },
        ): Member = Member(
            name = prop.simpleName.getShortName(),
            type = Type.from(prop.type),
            mutable = prop.isMutable,
            attributes = attributesOf { additionalAttributes() },
        )
    }
}

/**
 * Gets the low-level [Member] equivalent for this high-level member
 */
@InternalSdkApi
public val Member.lowLevel: Member
    get() = attributes[ModelAttributes.LowLevelMember]
