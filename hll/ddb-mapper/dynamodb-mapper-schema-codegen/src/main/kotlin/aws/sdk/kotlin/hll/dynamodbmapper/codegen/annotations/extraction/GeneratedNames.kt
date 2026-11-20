/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RenderOptions
import aws.sdk.kotlin.hll.codegen.rendering.Visibility
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.DestinationPackage
import aws.smithy.kotlin.runtime.collections.get
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter

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
 * The visibility of the code generated for one mapped item. [Visibility.MATCH_SOURCE] resolves to [Visibility.PUBLIC]
 * only if every declaration in [exposed] is effectively public (it and every declaration enclosing it are `public`),
 * and to [Visibility.INTERNAL] otherwise.
 * @param exposed The user declarations named in the signatures of the generated code
 */
internal fun generatedVisibility(exposed: Sequence<KSDeclaration>, ctx: RenderContext): Visibility = when (
    val visibility = ctx.attributes[RenderOptions.VisibilityAttribute]
) {
    Visibility.MATCH_SOURCE -> if (exposed.all { it.isEffectivelyPublic() }) Visibility.PUBLIC else Visibility.INTERNAL
    else -> visibility
}

private fun KSDeclaration.isEffectivelyPublic(): Boolean = generateSequence(this) { it.parentDeclaration }.all { it.isPublic() }

/**
 * Yields the declarations of this type and all of its type arguments, recursively, excluding type parameters
 */
internal fun KSType.referencedDeclarations(): Sequence<KSDeclaration> = sequence {
    if (declaration !is KSTypeParameter) yield(declaration)
    arguments.forEach { arg -> arg.type?.resolve()?.let { yieldAll(it.referencedDeclarations()) } }
}

/**
 * The item-level value converter generated for mapped class [decl]
 */
internal fun generatedValueConverter(decl: KSDeclaration, ctx: RenderContext): TypeRef = TypeRef(destinationPackage(decl, ctx), "${decl.simpleName.asString()}ValueConverter")

/**
 * The item converter generated for mapped class [decl]
 */
internal fun generatedItemConverter(decl: KSDeclaration, ctx: RenderContext): TypeRef = TypeRef(destinationPackage(decl, ctx), "${decl.simpleName.asString()}Converter")
