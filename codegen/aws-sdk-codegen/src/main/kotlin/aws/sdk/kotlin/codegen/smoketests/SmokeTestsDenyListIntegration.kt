package aws.sdk.kotlin.codegen.smoketests

import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.rendering.smoketests.SmokeTestsRunner
import software.amazon.smithy.model.Model

/**
 * Will wipe the smoke test runner file for services that are deny listed.
 *
 * Some services model smoke tests incorrectly and the code generated file will not compile.
 */
class SmokeTestsDenyListIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        settings.sdkId in smokeTestDenyList

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(SmokeTestsRunner, smokeTestDenyListSectionWriter),
        )

    private val smokeTestDenyListSectionWriter = SectionWriter { writer, _ ->
        writer.write("// Smoke tests for service deny listed until model is fixed")
    }
}

/**
 * SDK ID's of services that model smoke tests incorrectly
 */
val smokeTestDenyList = setOf(
    "Application Auto Scaling",
    "SWF",
    "WAFV2",
    "IoT Data Plane"
)
