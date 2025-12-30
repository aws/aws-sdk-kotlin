/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.util

import aws.sdk.kotlin.hll.codegen.core.CodeGenerator
import aws.sdk.kotlin.hll.codegen.model.HasAttributes
import aws.sdk.kotlin.hll.codegen.model.HllTypes
import aws.sdk.kotlin.hll.codegen.model.generatedApi
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.collections.Attributes

/**
 * Writes a `@GeneratedApi` annotation if the given [subject]'s attributes have [generatedApi] set
 */
@InternalSdkApi
public fun CodeGenerator.generatedAnnotation(vararg subjects: HasAttributes): Unit =
    generatedAnnotation(*subjects.map { it.attributes }.toTypedArray())

/**
 * Writes a `@GeneratedApi` annotation if the given [subjectAttributes] have [generatedApi] set
 */
@InternalSdkApi
public fun CodeGenerator.generatedAnnotation(vararg subjectAttributes: Attributes) {
    if (subjectAttributes.any { it.generatedApi }) {
        generatedAnnotation()
    }
}

/**
 * Writes a `@GeneratedApi` annotation
 */
@InternalSdkApi
public fun CodeGenerator.generatedAnnotation() {
    write("@#T", HllTypes.SmithyKotlin.RuntimeCore.GeneratedApi)
}
