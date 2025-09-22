/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen

import software.amazon.smithy.aws.traits.ServiceTrait
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.core.KotlinDelegator
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.traits.TitleTrait

/**
 * Generates an `OVERVIEW.md` file that will provide a brief intro for each service module in API reference docs.
 */
class ModuleDocumentationIntegration : KotlinIntegration {
    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        val overview = buildString {
            val moduleName = ctx.settings.pkg.name.split(".").last()
            val service = ctx.model.expectShape<ServiceShape>(ctx.settings.service)
            val title = service.getTrait<TitleTrait>()?.value
                ?: service.getTrait<ServiceTrait>()?.cloudFormationName
                ?: moduleName

            appendLine("# Module $moduleName")
            appendLine()
            appendLine("This module contains the Kotlin SDK client for **$title**.")
        }

        delegator.fileManifest.writeFile("OVERVIEW.md", overview)
    }
}
