/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetrics
import aws.sdk.kotlin.runtime.util.toAwsCredentialsBusinessMetric
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.copy
import aws.smithy.kotlin.runtime.httptest.TestConnection
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.Filesystem
import aws.smithy.kotlin.runtime.util.OperatingSystem
import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// TODO - refactor to make this work in common
class DefaultChainCredentialsProviderTest {

    class FsRootedAt(val root: File) : Filesystem {
        override val filePathSeparator: String = "/"
        override suspend fun readFileOrNull(path: String): ByteArray? {
            val realPath = Paths.get(root.path, path).toFile()
            return if (realPath.exists()) {
                withContext(Dispatchers.IO) {
                    realPath.readBytes()
                }
            } else {
                null
            }
        }

        override suspend fun writeFile(path: String, data: ByteArray) {
            error("not needed for test")
        }

        override fun fileExists(path: String): Boolean {
            error("not needed for test")
        }
    }

    class DefaultChainPlatformProvider(
        private val env: Map<String, String>,
        private val systemProperties: Map<String, String>,
        private val fs: Filesystem,
    ) : PlatformProvider,
        Filesystem by fs {
        override fun osInfo(): OperatingSystem = OperatingSystem(OsFamily.Linux, "test")
        override val isJvm: Boolean = true
        override val isAndroid: Boolean = false
        override val isBrowser: Boolean = false
        override val isNode: Boolean = false
        override val isNative: Boolean = false

        override fun getAllProperties(): Map<String, String> = systemProperties
        override fun getProperty(key: String): String? = systemProperties[key]
        override fun getAllEnvVars(): Map<String, String> = env
        override fun getenv(key: String): String? = env[key]
    }

    sealed class TestResult {
        abstract val name: String
        abstract val docs: String

        data class Ok(
            override val name: String,
            override val docs: String,
            val creds: Credentials,
        ) : TestResult()

        data class ErrorContains(
            override val name: String,
            override val docs: String,
            val message: String,
        ) : TestResult()
        companion object {

            fun fromJson(payload: String): TestResult {
                val obj = Json.parseToJsonElement(payload).jsonObject
                val name = checkNotNull(obj["name"]).jsonPrimitive.content
                val docs = checkNotNull(obj["docs"]).jsonPrimitive.content
                val result = checkNotNull(obj["result"]).jsonObject
                return when {
                    "Ok" in result -> {
                        val o = checkNotNull(result["Ok"]).jsonObject
                        val expectedBusinessMetrics = o["business_metrics"]?.jsonArray?.map { it.jsonPrimitive.content }?.toMutableSet() ?: mutableSetOf()
                        val expectedCreds = credentials(
                            checkNotNull(o["access_key_id"]).jsonPrimitive.content,
                            checkNotNull(o["secret_access_key"]).jsonPrimitive.content,
                            o["session_token"]?.jsonPrimitive?.content,
                            o["expiry"]?.jsonPrimitive?.longOrNull?.let { Instant.fromEpochSeconds(it) },
                            accountId = o["accountId"]?.jsonPrimitive?.content,
                        ).withBusinessMetrics(
                            expectedBusinessMetrics.map { it.toAwsCredentialsBusinessMetric() }.toSet(),
                        )
                        Ok(name, docs, expectedCreds)
                    }
                    "ErrorContains" in result -> ErrorContains(name, docs, checkNotNull(result["ErrorContains"]).jsonPrimitive.content)
                    else -> error("unrecognized result object: $result")
                }
            }
        }
    }

    data class TestCase(
        val expected: TestResult,
        val testPlatform: PlatformProvider,
        val testEngine: TestConnection,
    )

    fun makeTest(name: String): TestCase {
        val url = this::class.java.classLoader.getResource("default-provider-chain") ?: error("failed to load default-provider-chain test suite resource")
        val testSuiteDir = Paths.get(url.toURI()).toFile()
        val testDir = testSuiteDir.resolve(name)
        if (!testDir.exists()) error("$testDir does not exist")

        val testCaseFile = testDir.resolve("test-case.json")
        if (!testCaseFile.exists()) error("no test-case.json in $testDir")

        val testResult = TestResult.fromJson(testCaseFile.readText())

        val envFile = testDir.resolve("env.json")
        val env = if (envFile.exists()) {
            val el = Json.parseToJsonElement(envFile.readText())
            el.jsonObject.mapValues { it.value.jsonPrimitive.content }
        } else {
            emptyMap()
        }

        val systemPropertiesFile = testDir.resolve("system-properties.json")
        val systemProperties = if (systemPropertiesFile.exists()) {
            val el = Json.parseToJsonElement(systemPropertiesFile.readText())
            el.jsonObject.mapValues { it.value.jsonPrimitive.content }
        } else {
            emptyMap()
        }

        val httpTrafficFile = testDir.resolve("http-traffic.json")
        val testEngine = if (httpTrafficFile.exists()) {
            val traffic = httpTrafficFile.readText()
            TestConnection.fromJson(traffic)
        } else {
            TestConnection()
        }

        val fs = FsRootedAt(testDir.resolve("fs"))
        val testProvider = DefaultChainPlatformProvider(env, systemProperties, fs)

        return TestCase(testResult, testProvider, testEngine)
    }

    /**
     * Execute a test from the default chain test suite
     * @param name The name of root directory for the test (from common/test-resources/default-provider-chain)
     */
    fun executeTest(name: String) = runTest {
        val test = makeTest(name)
        val provider = DefaultChainCredentialsProvider(platformProvider = test.testPlatform, httpClient = test.testEngine)
        val actual = runCatching { provider.resolve() }
        val expected = test.expected
        when {
            expected is TestResult.Ok && actual.isFailure -> error("expected success, got error: $actual")
            expected is TestResult.ErrorContains && actual.isSuccess -> error("expected error, succeeded: $actual")
            expected is TestResult.Ok && actual.isSuccess -> {
                // if the expected creds have no expiration, use that, otherwise assert they are the same.
                // This is because the caching provider will expire even static creds after the given default timeout
                val actualCreds = actual.getOrThrow()

                val sanitizedExpiration = if (expected.creds.expiration == null) null else actualCreds.expiration
                val creds = actualCreds.copy(providerName = null, expiration = sanitizedExpiration)
                assertEquals(expected.creds, creds)

                // assert http traffic to the extent we can. These tests do not have specific timestamps they
                // were signed with and some lack enough context to even assert a body (e.g. incorrect content-type).
                // They would require additional metadata to make use of `testEngine.assertRequests()`.
                test.testEngine.requests().forEach { call ->
                    if (call.expected != null) {
                        assertEquals(call.expected!!.url.host, call.actual.url.host)
                    }
                }
            }
            expected is TestResult.ErrorContains && actual.isFailure -> {
                val ex = actual.exceptionOrNull()!!
                // the chain contains a generic exception with the list of providers tried but it
                // contains all of the suppressed exceptions along the way. Inspect them all and their causes.
                // In particular a test case only looks to verify a specific behavior and even though it
                // may fail at the correct spot, later providers may still be tried and also fail.
                val needle = expected.message
                val haystack = listOf(ex.message!!) + ex.suppressed.map { it.message!! } + ex.suppressed.mapNotNull { it.cause?.message }
                val expectedErrorFound = haystack.any { it.contains(needle) }
                assertTrue(expectedErrorFound, "`$needle` not found in any of the chain exception messages: $haystack")
            }
            else -> error("should not be able to get here")
        }
    }

    @Test
    fun testEcsAssumeRole() = executeTest("ecs_assume_role")

    @Test
    fun testEcsCredentials() = executeTest("ecs_credentials")

    @Test
    fun testImdsAssumeRole() = executeTest("imds_assume_role")

    @Test
    fun testImdsConfigWithNoCreds() = executeTest("imds_config_with_no_creds")

    @Test
    fun testImdsDefaultChainError() = executeTest("imds_default_chain_error")

    @Test
    fun testImdsDefaultChainRetries() = executeTest("imds_default_chain_retries")

    @Test
    fun testImdsDefaultChainSuccess() = executeTest("imds_default_chain_success")

    @Test
    fun testImdsDisabled() = executeTest("imds_disabled")

    @Test
    fun testImdsNoIamRole() = executeTest("imds_no_iam_role")

    @Test
    fun testImdsTokenFail() = executeTest("imds_token_fail")

    @Test
    fun testPreferEnvironment() = executeTest("prefer_environment")

    @Test
    fun testPreferSystemProperties() = executeTest("prefer_system_properties")

    @Test
    fun testProfileName() = executeTest("profile_name")

    @Test
    fun testProfileOverridesWebIdentity() = executeTest("profile_overrides_web_identity")

    @Test
    fun testProfileStaticKeys() = executeTest("profile_static_keys")

    @Test
    fun testWebIdentitySourceProfileNoEnv() = executeTest("web_identity_source_profile_no_env")

    @Test
    fun testWebIdentityTokenEnv() = executeTest("web_identity_token_env")

    // NOTE: the <Message> tag here in the HTTP traffic is correctly parsed by the hand-written deserializer
    // to match error code, etc. The model and generated deserializer uses lowercase `message` though so the
    // detailed message we would actually like to check for `No OpenIDConnect provider found in your account for...`
    // is only available on the suppressed exception->cause->sdkErrorMetadata.errorMessage.
    // See https://github.com/awslabs/aws-sdk-kotlin/issues/479
    @Test
    fun testWebIdentityTokenInvalidJwt() = executeTest("web_identity_token_invalid_jwt")

    @Test
    fun testWebIdentityTokenProfile() = executeTest("web_identity_token_profile")

    @Test
    fun testWebIdentityTokenSourceProfile() = executeTest("web_identity_token_source_profile")

    @Test
    fun testLegacySsoRole() = executeTest("legacy_sso_role")

    @Test
    fun testSsoSession() = executeTest("sso_session")

    @Test
    fun testStsRetryOnError() = executeTest("retry_on_error")
}
