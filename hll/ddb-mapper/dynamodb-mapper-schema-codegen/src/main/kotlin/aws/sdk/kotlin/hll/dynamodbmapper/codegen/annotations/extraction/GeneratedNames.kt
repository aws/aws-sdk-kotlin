/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.DestinationPackage
import aws.smithy.kotlin.runtime.collections.get
import com.google.devtools.ksp.symbol.KSDeclaration

/*
 * The single source of generated declaration locations. Both a class's own generated code and every reference to it
 * from another mapped class derive their packages and names here, so a declaration and its references cannot drift.
 */

/**
 * The package into which code generated for [decl] is emitted
 */
internal fun destinationPackage(decl: KSDeclaration, ctx: RenderContext): String = when (
    val dstPkg = ctx.attributes[AnnotationsProcessorOptions.DestinationPackageAttribute]
) {
    is DestinationPackage.Relative -> "${decl.packageName.asString()}.${dstPkg.pkg}"
    is DestinationPackage.Absolute -> dstPkg.pkg
}

/**
 * The item-level value converter generated for mapped class [decl]
 */
internal fun generatedValueConverter(decl: KSDeclaration, ctx: RenderContext): TypeRef = TypeRef(destinationPackage(decl, ctx), "${decl.simpleName.asString()}ValueConverter")

/**
 * The item converter generated for mapped class [decl]
 */
internal fun generatedItemConverter(decl: KSDeclaration, ctx: RenderContext): TypeRef = TypeRef(destinationPackage(decl, ctx), "${decl.simpleName.asString()}Converter")
