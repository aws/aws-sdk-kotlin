/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.ksp

import aws.sdk.kotlin.runtime.InternalSdkApi
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias

/**
 * Resolves aliases without type parameters, preserving nullability from both the alias and its use site.
 * Parameterized aliases are left unchanged because expanding them requires substituting their type arguments.
 */
@InternalSdkApi
public fun KSType.resolveTypeAliases(): KSType {
    val alias = declaration as? KSTypeAlias ?: return this
    if (alias.typeParameters.isNotEmpty()) return this

    val resolved = alias.type.resolve().resolveTypeAliases()
    return if (isMarkedNullable) resolved.makeNullable() else resolved
}
