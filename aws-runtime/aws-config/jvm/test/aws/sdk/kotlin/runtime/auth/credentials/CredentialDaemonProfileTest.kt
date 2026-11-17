/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.FakeCredentialDaemon.Action
import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.StsClient
import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.assumeRole
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetrics
import aws.smithy.kotlin.runtime.httptest.TestConnection
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class CredentialDaemonProfileTest {
    private val failingReply = """{"jsonrpc":"2.0","id":1,"error":{"code":-32000,"message":"nope","data":{"code":"REAUTH_REQUIRED"}}}""" + "\n"

    private fun env(
        daemon: FakeCredentialDaemon?,
        config: String,
        credentials: String = "",
        extra: Map<String, String> = emptyMap(),
    ): Map<String, String> {
        val dir = FakeCredentialDaemon.tempDir()
        val configFile = File(dir, "config").apply { writeText(config.trimIndent()) }
        val credsFile = File(dir, "credentials").apply { writeText(credentials.trimIndent()) }
        return buildMap {
            put("AWS_CONFIG_FILE", configFile.absolutePath)
            put("AWS_SHARED_CREDENTIALS_FILE", credsFile.absolutePath)
            put("HOME", dir.absolutePath)
            put("AWS_EC2_METADATA_DISABLED", "true")
            if (daemon != null) put("AWS_CREDENTIAL_DAEMON_SOCKET", daemon.path)
            putAll(extra)
        }
    }

    private fun requestedCredential(daemon: FakeCredentialDaemon): String = Json.parseToJsonElement(daemon.requests.single()).jsonObject["params"]!!.jsonObject["credential"]!!.jsonPrimitive.content

    @Test
    fun testAcceptanceThroughHomeLink() = runTest {
        FakeCredentialDaemon().use { daemon ->
            val env = env(null, "[profile dev]\ndaemon_credential = dev", extra = mapOf("AWS_PROFILE" to "dev"))
            val aws = File(env.getValue("HOME"), ".aws").apply { mkdirs() }
            Files.createSymbolicLink(File(aws, "credential-daemon.sock").toPath(), Paths.get(daemon.path))
            DefaultChainCredentialsProvider(platformProvider = fakePlatform(env), region = "us-east-1").use { provider ->
                assertEquals("ASIADAEMON", provider.resolve().accessKeyId)
            }
            assertEquals("dev", requestedCredential(daemon))
        }
    }

    @Test
    fun testAcceptanceThroughDefaultChain() = runTest {
        FakeCredentialDaemon().use { daemon ->
            val env = env(daemon, "[profile dev]\ndaemon_credential = dev", extra = mapOf("AWS_PROFILE" to "dev"))
            DefaultChainCredentialsProvider(platformProvider = fakePlatform(env), region = "us-east-1").use { provider ->
                val creds = provider.resolve()
                assertEquals("ASIADAEMON", creds.accessKeyId)
                assertTrue(AwsBusinessMetric.Credentials.CREDENTIAL_DAEMON in creds.attributes.getOrNull(BusinessMetrics).orEmpty())
            }
            assertEquals("dev", requestedCredential(daemon))
        }
    }

    @Test
    fun testEnvWebIdentityWithMissingTokenFallsThroughToDaemonProfile() = runTest {
        FakeCredentialDaemon().use { daemon ->
            val env = env(
                daemon,
                "[profile dev]\ndaemon_credential = dev",
                extra = mapOf(
                    "AWS_ROLE_ARN" to StsTestUtils.ARN,
                    "AWS_WEB_IDENTITY_TOKEN_FILE" to "/nonexistent/aws-web-identity-token",
                    "AWS_PROFILE" to "dev",
                ),
            )
            val conn = TestConnection()
            DefaultChainCredentialsProvider(platformProvider = fakePlatform(env), httpClient = conn, region = "us-east-1").use { provider ->
                assertEquals("ASIADAEMON", provider.resolve().accessKeyId)
            }
            assertEquals("dev", requestedCredential(daemon))
            assertEquals(0, conn.requests().size)
        }
    }

    @Test
    fun testEnvironmentCredentialsWinOverDaemonProfile() = runTest {
        FakeCredentialDaemon().use { daemon ->
            val env = env(
                daemon,
                "[default]\ndaemon_credential = dev",
                extra = mapOf("AWS_ACCESS_KEY_ID" to "AKIDENV", "AWS_SECRET_ACCESS_KEY" to "secret"),
            )
            DefaultChainCredentialsProvider(platformProvider = fakePlatform(env), region = "us-east-1").use { provider ->
                assertEquals("AKIDENV", provider.resolve().accessKeyId)
            }
            assertEquals(0, daemon.requests.size)
        }
    }

    @Test
    fun testRoleWithDaemonSourceProfile() = runTest {
        FakeCredentialDaemon().use { daemon ->
            val env = env(
                daemon,
                """
                [default]
                role_arn = ${StsTestUtils.ARN}
                source_profile = base

                [profile base]
                daemon_credential = base-cred
                """,
            )
            val conn = buildTestConnection { expect(StsTestUtils.stsResponse()) }
            val provider = ProfileCredentialsProvider(platformProvider = fakePlatform(env), httpClient = conn, region = StsTestUtils.REGION)
            val creds = provider.resolve()
            assertEquals("AKIDTest", creds.accessKeyId)
            assertEquals("base-cred", requestedCredential(daemon))
            assertEquals(1, conn.requests().size)
            // the assume-role call was signed with the daemon's credentials
            val auth = conn.requests().single().actual.headers["Authorization"]
            assertNotNull(auth)
            assertTrue(auth.contains("Credential=ASIADAEMON/"), auth)
            val ids = creds.attributes.getOrNull(BusinessMetrics).orEmpty().map { it.identifier }
            listOf("o", "Aq", "i").forEach { assertTrue(it in ids, "metrics: $ids") }
        }
    }

    @Test
    fun testCredentialsFileValueIgnored() = runTest {
        FakeCredentialDaemon().use { daemon ->
            val env = env(
                daemon,
                """
                [default]
                aws_access_key_id = AKIDCONFIG
                aws_secret_access_key = secret
                """,
                credentials = """
                [default]
                daemon_credential = dev
                """,
            )
            assertEquals("AKIDCONFIG", ProfileCredentialsProvider(platformProvider = fakePlatform(env)).resolve().accessKeyId)
            assertEquals(0, daemon.requests.size)
        }
    }

    @Test
    fun testCredentialsFileDoesNotOverrideConfig() = runTest {
        FakeCredentialDaemon().use { daemon ->
            val env = env(
                daemon,
                "[default]\ndaemon_credential = from-config",
                credentials = "[default]\ndaemon_credential = from-credentials",
            )
            assertEquals("ASIADAEMON", ProfileCredentialsProvider(platformProvider = fakePlatform(env)).resolve().accessKeyId)
            assertEquals("from-config", requestedCredential(daemon))
        }
    }

    @Test
    fun testDaemonFailureDoesNotFallBackWithinProfile() = runTest {
        FakeCredentialDaemon(handler = { Action.Reply(failingReply) }).use { daemon ->
            val marker = File(FakeCredentialDaemon.tempDir(), "process-ran")
            val env = env(
                daemon,
                """
                [default]
                daemon_credential = dev
                aws_access_key_id = AKIDSTATIC
                aws_secret_access_key = secret
                credential_process = touch ${marker.absolutePath}
                """,
            )
            val ex = assertFailsWith<CredentialDaemonException> {
                ProfileCredentialsProvider(platformProvider = fakePlatform(env)).resolve()
            }
            assertEquals(CredentialDaemonErrorCode.REAUTH_REQUIRED, ex.errorCode)
            assertEquals(1, daemon.requests.size)
            assertFalse(marker.exists(), "credential_process must not run")
        }
    }

    @Test
    fun testFeatureIdOnTheWire() = runTest {
        FakeCredentialDaemon().use { daemon ->
            val env = env(daemon, "[default]\ndaemon_credential = dev")
            val conn = buildTestConnection { expect(StsTestUtils.stsResponse()) }
            StsClient {
                region = StsTestUtils.REGION
                credentialsProvider = ProfileCredentialsProvider(platformProvider = fakePlatform(env))
                httpClient = conn
            }.use { client ->
                client.assumeRole {
                    roleArn = StsTestUtils.ARN
                    roleSessionName = "test"
                }
            }
            val ua = conn.requests().single().actual.headers["User-Agent"] ?: fail("no User-Agent header")
            val metrics = ua.split(" ").firstOrNull { it.startsWith("m/") } ?: fail("no m/ segment in `$ua`")
            assertTrue("Aq" in metrics.removePrefix("m/").split(","), ua)
        }
    }

    @Test
    fun testNoRequestWhenDaemonNotRunning() = runTest {
        FakeCredentialDaemon.assumeSupported()
        val missing = File(FakeCredentialDaemon.tempDir(), "missing.sock").absolutePath
        val env = env(null, "[default]\ndaemon_credential = dev", extra = mapOf("AWS_CREDENTIAL_DAEMON_SOCKET" to missing))
        val conn = TestConnection()
        StsClient {
            region = StsTestUtils.REGION
            credentialsProvider = ProfileCredentialsProvider(platformProvider = fakePlatform(env))
            httpClient = conn
        }.use { client ->
            val ex = assertFailsWith<Exception> {
                client.assumeRole {
                    roleArn = StsTestUtils.ARN
                    roleSessionName = "test"
                }
            }
            val daemonEx = generateSequence<Throwable>(ex) { it.cause }.filterIsInstance<CredentialDaemonException>().firstOrNull()
                ?: fail("expected a CredentialDaemonException, got $ex")
            assertEquals(CredentialDaemonErrorCode.NOT_RUNNING, daemonEx.errorCode)
        }
        assertEquals(0, conn.requests().size)
    }
}
