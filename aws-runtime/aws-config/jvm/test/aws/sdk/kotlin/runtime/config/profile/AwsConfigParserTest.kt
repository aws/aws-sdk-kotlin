/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.smithy.kotlin.runtime.telemetry.logging.Logger
import aws.smithy.kotlin.runtime.util.OperatingSystem
import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.PlatformProvider
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

class AwsProfileParserTest {

    @Test
    fun canPassTestSuite() {
        val testList = Json.parseToJsonElement(PARSER_TEST_SUITE_JSON).jsonObject["tests"]!!.jsonArray

        testList
            .map { TestCase.fromJson(it.jsonObject) }
            // .filter { testCase ->
            //     testCase.name == "SSO Session in credentials file is invalid"
            // }
            .forEachIndexed { index, testCase ->
                when (testCase) {
                    is TestCase.MatchConfigOutputCase -> {
                        val actual = parse(Logger.None, FileType.CONFIGURATION, testCase.configInput).toJsonElement()
                        val expectedJson = Json.parseToJsonElement(testCase.expectedOutput)
                        assertEquals(expectedJson, actual, message = "[idx=$index]: $testCase")
                    }
                    is TestCase.MatchCredentialOutputCase -> {
                        val actual = parse(Logger.None, FileType.CREDENTIAL, testCase.credentialInput).toJsonElement()
                        assertEquals(testCase.expectedOutput, actual.toString(), message = "[idx=$index]: $testCase")
                    }
                    is TestCase.MatchConfigAndCredentialOutputCase -> {
                        val actual = loadConfiguration({ testCase.configInput }, { testCase.credentialInput }).toJsonElement()
                        assertEquals(testCase.expectedOutput, actual.toString(), message = "[idx=$index]: $testCase")
                    }
                    is TestCase.MatchErrorCase -> {
                        val ex = assertFailsWith<AwsConfigParseException>("[idx=$index]: $testCase") {
                            loadConfiguration({ testCase.configInput }, { testCase.credentialInput })
                        }
                        ex.message.shouldContain(testCase.expectedErrorMessage)
                    }
                }
            }
    }

    @Test
    fun itCanBeUsedInTests() = runTest {
        // NOTE: This is the minimal mock of the Platform type needed to support aws configuration loading of a specific kvp.
        val testPlatform = mockk<PlatformProvider>()
        val propKeyParam = slot<String>()
        val filePath = slot<String>()

        every { testPlatform.getenv(any()) } answers { null }
        every { testPlatform.getProperty(capture(propKeyParam)) } answers { if (propKeyParam.captured == "user.home") "/home" else null }
        every { testPlatform.filePathSeparator } returns "/"
        every { testPlatform.osInfo() } returns OperatingSystem(OsFamily.Linux, null)
        coEvery { testPlatform.readFileOrNull(capture(filePath)) } answers {
            if (filePath.captured == "/home/.aws/config") "[profile default]\nboo = hoo".encodeToByteArray() else null
        }

        assertEquals("hoo", fnThatLoadsConfiguration(testPlatform))
    }

    /**
     * Example function that reads the active profile and returns true if a key "boo" exists.
     */
    private suspend fun fnThatLoadsConfiguration(platform: PlatformProvider): String? {
        val profile = loadAwsSharedConfig(platform).activeProfile
        return profile.getOrNull("boo")
    }

    private sealed class TestCase {
        abstract val name: String
        companion object {
            fun fromJson(json: JsonObject): TestCase {
                val name = (json["name"] as JsonPrimitive).content
                val configIn = (json["input"]!!.jsonObject["configFile"] as JsonPrimitive?)?.content
                val credentialIn = (json["input"]!!.jsonObject["credentialsFile"] as JsonPrimitive?)?.content
                val expected = json["output"]!!.toString()
                val errorContaining = (json["output"]!!.jsonObject["errorContaining"] as JsonPrimitive?)?.content

                check(configIn != null || credentialIn != null) { "Unexpected output: $json" }

                val isErrorCase = errorContaining != null

                return if (!isErrorCase) {
                    when {
                        configIn != null && credentialIn != null -> MatchConfigAndCredentialOutputCase(name, configIn, credentialIn, expected)
                        configIn != null -> MatchConfigOutputCase(name, configIn, expected)
                        credentialIn != null -> MatchCredentialOutputCase(name, credentialIn, expected)
                        else -> error("Unexpected branch from $json")
                    }
                } else {
                    MatchErrorCase(name, configIn, credentialIn, errorContaining!!)
                }
            }
        }

        data class MatchConfigOutputCase(override val name: String, val configInput: String, val expectedOutput: String) : TestCase()

        data class MatchCredentialOutputCase(override val name: String, val credentialInput: String, val expectedOutput: String) : TestCase()

        data class MatchConfigAndCredentialOutputCase(override val name: String, val configInput: String, val credentialInput: String, val expectedOutput: String) : TestCase()

        data class MatchErrorCase(
            override val name: String,
            val configInput: String?,
            val credentialInput: String?,
            val expectedErrorMessage: String,
        ) : TestCase()
    }

    /**
     * Produces a merged AWS configuration based on optional configuration and credential file contents.
     * @param configurationFn a function that will retrieve a configuration file as a UTF-8 string.
     * @param credentialsFn a function that will retrieve a configuration file as a UTF-8 string.
     * @return A map containing all specified profiles defined in configuration and credential files.
     */
    private fun loadConfiguration(configurationFn: () -> String?, credentialsFn: () -> String?): TypedSectionMap = mergeFiles(
        parse(Logger.None, FileType.CONFIGURATION, configurationFn()),
        parse(Logger.None, FileType.CREDENTIAL, credentialsFn()),
    )
}

// See https://youtrack.jetbrains.com/issue/KTOR-3063
private fun TypedSectionMap.toJsonElement(): JsonElement {
    val map: MutableMap<String, JsonElement> = mutableMapOf()
    this.forEach { (key, value) ->
        val sectionKey = when (key) {
            ConfigSectionType.PROFILE -> "profiles"
            ConfigSectionType.SSO_SESSION -> "sso-sessions"
            ConfigSectionType.SERVICES -> "services"
            ConfigSectionType.UNKNOWN -> "unknown"
        }
        if (value.isNotEmpty()) {
            map[sectionKey] = sectionMapToJsonElement(value)
        }
    }
    return JsonObject(map)
}

private fun sectionMapToJsonElement(sectionMap: SectionMap): JsonElement {
    val map: MutableMap<String, JsonElement> = mutableMapOf()
    sectionMap.forEach { (key, value) ->
        map[key] = configValuesToJsonElement(value.properties)
    }
    return JsonObject(map)
}

private fun configValuesToJsonElement(values: Map<String, AwsConfigValue>): JsonElement {
    val map: MutableMap<String, JsonElement> = mutableMapOf()
    values.forEach { (key, value) ->
        when (value) {
            is AwsConfigValue.String -> map[key] = JsonPrimitive(value.value)
            is AwsConfigValue.Map -> map[key] = JsonObject(value.value.mapValues { JsonPrimitive(it.value) })
        }
    }
    return JsonObject(map)
}
