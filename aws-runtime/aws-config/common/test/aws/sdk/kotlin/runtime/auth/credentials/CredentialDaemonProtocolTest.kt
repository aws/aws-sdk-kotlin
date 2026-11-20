/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.SocketPathEntry
import aws.sdk.kotlin.runtime.auth.credentials.internal.credentialDaemonRequest
import aws.sdk.kotlin.runtime.auth.credentials.internal.isValidCredentialDaemonName
import aws.sdk.kotlin.runtime.auth.credentials.internal.parseCredentialDaemonReply
import aws.sdk.kotlin.runtime.auth.credentials.internal.resolveCredentialDaemonSocketPath
import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.smithy.kotlin.runtime.identity.IdentityAttributes
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.OperatingSystem
import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CredentialDaemonProtocolTest {
    // In the past, so `testSuccess` also covers `expiry-past`: a `requiresCredentialRefresh` case that passes on the existing refresh code, so it runs as a plain test.
    private val expires = "2026-03-01T12:00:00Z"

    private fun okReply(
        material: String = """"type":"session_credentials","access_key_id":"AKID","secret_access_key":"SECRET","session_token":"TOKEN"""",
        expiresAt: String = "\"$expires\"",
        extra: String = "",
    ): String = """{"jsonrpc":"2.0","id":1,"result":{"credential":"dev","material":{$material},"expires_at":$expiresAt$extra}}"""

    private fun parse(reply: String) = parseCredentialDaemonReply(reply.encodeToByteArray(), "dev")

    private fun assertError(code: CredentialDaemonErrorCode, reply: String): CredentialDaemonException = assertError(code, reply.encodeToByteArray())

    private fun assertError(code: CredentialDaemonErrorCode, reply: ByteArray): CredentialDaemonException {
        val ex = assertFailsWith<CredentialDaemonException> { parseCredentialDaemonReply(reply, "dev") }
        assertEquals(code, ex.errorCode, ex.message)
        return ex
    }

    @Test
    fun testSuccess() {
        val creds = parse(okReply(extra = ""","provenance":{"source":"x"},"unknown":[1,2.5,true,null,-0.5e-3]"""))
        assertEquals("AKID", creds.accessKeyId)
        assertEquals("SECRET", creds.secretAccessKey)
        assertEquals("TOKEN", creds.sessionToken)
        assertEquals(Instant.fromIso8601(expires), creds.expiration)
        assertEquals("CredentialDaemon", creds.attributes.getOrNull(IdentityAttributes.ProviderName))
        assertNull(creds.attributes.getOrNull(AwsClientOption.AccountId))
    }

    @Test
    fun testAccountIdAndOptionalFields() {
        val creds = parse(
            okReply(material = """"type":"session_credentials","access_key_id":"AKID","secret_access_key":"S","session_token":"","account_id":"123456789012""""),
        )
        assertNull(creds.sessionToken)
        assertEquals("123456789012", creds.attributes.getOrNull(AwsClientOption.AccountId))

        val nullToken = parse(okReply(material = """"type":"session_credentials","access_key_id":"AKID","secret_access_key":"S","session_token":null"""))
        assertNull(nullToken.sessionToken)

        assertError(
            CredentialDaemonErrorCode.PROTOCOL_ERROR,
            okReply(material = """"type":"session_credentials","access_key_id":"AKID","secret_access_key":"S","session_token":5"""),
        )
    }

    @Test
    fun testMismatchedCredentialAndIdIgnored() {
        val reply = """{"jsonrpc":"2.0","id":"other","result":{"credential":"someone-else","material":{"type":"session_credentials","access_key_id":"AKID","secret_access_key":"S"},"expires_at":"$expires"}}"""
        assertEquals("AKID", parse(reply).accessKeyId)
    }

    @Test
    fun testEnvelopeRules() {
        val marker = "MARKER-SHOULD-NOT-LEAK"
        listOf(
            """{"jsonrpc":"1.0","result":{"marker":"$marker"}}""",
            """{"result":{"marker":"$marker"}}""",
            """{"jsonrpc":"2.0","id":1}""",
            """{"jsonrpc":"2.0","result":{},"error":{"marker":"$marker"}}""",
            """{"jsonrpc":"2.0","result":null,"error":null}""",
            """["$marker"]""",
            """"$marker"""",
            """{"jsonrpc":"2.0","result":{}} {"$marker":1}""",
            """{"jsonrpc":"2.0","result":{"$marker":""",
            "not json $marker",
            "",
            "   ",
            """{"jsonrpc":"2.0","error":"$marker"}""",
            """{"jsonrpc":"2.0","result":"$marker"}""",
            """{"jsonrpc":"2.0","result":null}""",
            """{"jsonrpc":"2.0","result":{"material":"$marker"}}""",
            """{"jsonrpc":"2.0","result":{"material":{"type":""}}}""",
            """{"jsonrpc":"2.0","result":{"material":{"type":5}}}""",
        ).forEach { reply ->
            val ex = assertError(CredentialDaemonErrorCode.PROTOCOL_ERROR, reply)
            assertFalse(ex.message!!.contains(marker), "message leaked reply content: ${ex.message}")
        }
    }

    @Test
    fun testOutOfRangeNumberIsProtocolError() {
        // smithy-kotlin's JSON document deserializer rejects numbers it can't represent, even in fields the SDK ignores
        listOf("1e999", "123456789012345678901234567890").forEach {
            assertError(CredentialDaemonErrorCode.PROTOCOL_ERROR, okReply(extra = ",\"unknown\":$it"))
        }
    }

    @Test
    fun testInvalidUtf8() {
        val bytes = okReply().encodeToByteArray() + byteArrayOf(0xC3.toByte(), 0x28)
        val ex = assertError(CredentialDaemonErrorCode.PROTOCOL_ERROR, bytes)
        assertTrue(ex.message!!.contains("UTF-8"))
    }

    @Test
    fun testDeepNesting() {
        val deep = "[".repeat(10_000) + "]".repeat(10_000)
        assertError(CredentialDaemonErrorCode.PROTOCOL_ERROR, """{"jsonrpc":"2.0","result":{"x":$deep}}""")
    }

    @Test
    fun testRequiredFields() {
        val fields = mapOf(
            "access_key_id" to "\"AKID\"",
            "secret_access_key" to "\"S\"",
        )
        fields.keys.forEach { missing ->
            listOf(null, "\"\"", "null", "5").forEach { value ->
                val material = (fields - missing).entries.joinToString(",") { "\"${it.key}\":${it.value}" } +
                    (value?.let { ",\"$missing\":$it" } ?: "")
                val ex = assertError(CredentialDaemonErrorCode.PROTOCOL_ERROR, okReply(material = """"type":"session_credentials",$material"""))
                assertEquals("invalid reply from the credential daemon: missing required field $missing", ex.message)
            }
        }

        listOf("\"\"", "null", "5").forEach { value ->
            val ex = assertError(CredentialDaemonErrorCode.PROTOCOL_ERROR, okReply(expiresAt = value))
            assertEquals("invalid reply from the credential daemon: missing required field expires_at", ex.message)
        }
        val reply = """{"jsonrpc":"2.0","result":{"material":{"type":"session_credentials","access_key_id":"A","secret_access_key":"S"}}}"""
        assertError(CredentialDaemonErrorCode.PROTOCOL_ERROR, reply)
    }

    @Test
    fun testExpiresAt() {
        val nanos = parse(okReply(expiresAt = "\"2026-03-01T12:00:00.123456789Z\"")).expiration!!
        assertEquals(Instant.fromIso8601(expires).epochSeconds, nanos.epochSeconds)
        assertEquals(123456789, nanos.nanosecondsOfSecond)

        assertEquals(Instant.fromIso8601(expires), parse(okReply()).expiration)
        assertEquals(123_000_000, parse(okReply(expiresAt = "\"2026-03-01T12:00:00.123Z\"")).expiration!!.nanosecondsOfSecond)

        listOf(
            "2026-03-01T12:00:00+00:00",
            "2026-03-01T12:00:00",
            "2026-02-30T12:00:00Z",
            "2026-03-01T12:00:00.1234567890Z",
            "2026-03-01T12:00:00.Z",
            "2026-03-01 12:00:00Z",
            "tomorrow",
        ).forEach {
            val ex = assertError(CredentialDaemonErrorCode.PROTOCOL_ERROR, okReply(expiresAt = "\"$it\""))
            assertEquals("invalid reply from the credential daemon: unparseable expires_at", ex.message, it)
        }
    }

    @Test
    fun testUnsupportedMaterial() {
        val ex = assertError(
            CredentialDaemonErrorCode.UNSUPPORTED_MATERIAL,
            """{"jsonrpc":"2.0","result":{"material":{"type":"bearer\u0007_token","access_key_id":5},"expires_at":"garbage"}}""",
        )
        assertTrue(ex.message!!.contains("`bearer_token`"), ex.message)
        assertFalse(ex.message!!.any { it.isISOControl() })
    }

    @Test
    fun testUnsupportedMaterialTypeIsCapped() {
        val longType = "x509_" + "a".repeat(60_000)
        val ex = assertError(
            CredentialDaemonErrorCode.UNSUPPORTED_MATERIAL,
            """{"jsonrpc":"2.0","result":{"material":{"type":"$longType"}}}""",
        )
        assertTrue(ex.message!!.contains("`${longType.take(64)}...`"), ex.message)
        assertTrue(ex.message!!.length < 200, "message length ${ex.message!!.length}")
    }

    @Test
    fun testErrorCodes() {
        fun err(error: String) = """{"jsonrpc":"2.0","id":1,"error":$error}"""

        val reauth = assertError(
            CredentialDaemonErrorCode.REAUTH_REQUIRED,
            err("""{"code":-32000,"message":"session\u001b[31m expired\n","data":{"code":"REAUTH_REQUIRED"}}"""),
        )
        assertEquals("REAUTH_REQUIRED", reauth.daemonErrorCode)
        assertTrue(reauth.message!!.contains("sign in again"))
        assertEquals("the credential daemon requires you to sign in again (daemon: session[31m expired)", reauth.message)
        assertFalse(reauth.message!!.any { it.isISOControl() })

        val notFound = assertError(
            CredentialDaemonErrorCode.CREDENTIAL_NOT_FOUND,
            err("""{"code":-32000,"data":{"code":"CREDENTIAL_NOT_FOUND"}}"""),
        )
        assertEquals("this user has no credential named `dev`; sign in again", notFound.message)

        val notFoundWithMessage = assertError(
            CredentialDaemonErrorCode.CREDENTIAL_NOT_FOUND,
            err("""{"code":-32000,"message":"credential not found","data":{"code":"CREDENTIAL_NOT_FOUND"}}"""),
        )
        assertEquals("this user has no credential named `dev`; sign in again (daemon: credential not found)", notFoundWithMessage.message)

        val rejected = assertError(
            CredentialDaemonErrorCode.REQUEST_REJECTED,
            err("""{"code":-32000,"message":"nope","data":{"code":"POLICY\u0000DENIED"}}"""),
        )
        assertEquals("POLICYDENIED", rejected.daemonErrorCode)
        assertEquals("the credential daemon rejected the request (code: POLICYDENIED, daemon: nope)", rejected.message)

        val noData = assertError(CredentialDaemonErrorCode.REQUEST_REJECTED, err("""{"code":-32601,"message":"no such method"}"""))
        assertNull(noData.daemonErrorCode)
        assertEquals("the credential daemon rejected the request (daemon: no such method)", noData.message)

        assertError(CredentialDaemonErrorCode.REQUEST_REJECTED, err("{}"))
        assertError(CredentialDaemonErrorCode.PROTOCOL_ERROR, err("null"))
        assertError(CredentialDaemonErrorCode.PROTOCOL_ERROR, err("[]"))
    }

    @Test
    fun testRequestShape() {
        val plain = credentialDaemonRequest("dev", null, null).decodeToString()
        assertTrue(plain.endsWith("\n"))
        assertEquals(1, plain.count { it == '\n' })
        val json = Json.parseToJsonElement(plain).jsonObject
        assertEquals("2.0", json["jsonrpc"]!!.jsonPrimitive.content)
        assertEquals("1", json["id"]!!.jsonPrimitive.content)
        assertEquals("credential/get", json["method"]!!.jsonPrimitive.content)
        assertEquals(JsonObject(mapOf("credential" to Json.parseToJsonElement("\"dev\""))), json["params"])

        val sso = credentialDaemonRequest("dev", "123456789012", "Role\"\n").decodeToString()
        assertEquals(1, sso.count { it == '\n' })
        val params = Json.parseToJsonElement(sso).jsonObject["params"]!!.jsonObject
        assertEquals("dev", params["credential"]!!.jsonPrimitive.content)
        assertEquals("123456789012", params["account_id"]!!.jsonPrimitive.content)
        assertEquals("Role\"\n", params["role_name"]!!.jsonPrimitive.content)
    }

    @Test
    fun testNameValidation() {
        listOf("a", "-a", "_a", "A.b-c_9", "x".repeat(64), "a.").forEach { assertTrue(isValidCredentialDaemonName(it), it) }
        listOf("", ".x", "x".repeat(65), "a b", "a/b", "é", "a\nb", " a").forEach { assertFalse(isValidCredentialDaemonName(it), it) }
    }

    private val linux = OperatingSystem(OsFamily.Linux, "test")
    private val mac = OperatingSystem(OsFamily.MacOs, "test")

    private fun path(
        env: Map<String, String>,
        os: OperatingSystem = linux,
        entries: Map<String, SocketPathEntry> = emptyMap(),
        props: Map<String, String> = emptyMap(),
        socketPath: String? = null,
    ): String = resolveCredentialDaemonSocketPath(TestPlatformProvider(env = env, props = props, os = os), socketPath) {
        entries[it] ?: SocketPathEntry.Missing
    }

    private fun noLocation(
        env: Map<String, String>,
        entries: Map<String, SocketPathEntry> = emptyMap(),
        props: Map<String, String> = emptyMap(),
        socketPath: String? = null,
    ): CredentialDaemonException {
        val ex = assertFailsWith<CredentialDaemonException> { path(env, entries = entries, props = props, socketPath = socketPath) }
        assertEquals(CredentialDaemonErrorCode.NOT_RUNNING, ex.errorCode, ex.message)
        listOf("install the credential daemon", "\$HOME/.aws/credential-daemon.sock", "AWS_CREDENTIAL_DAEMON_SOCKET").forEach {
            assertTrue(ex.message!!.contains(it, ignoreCase = true), ex.message)
        }
        return ex
    }

    @Test
    fun testSocketPathResolution() {
        val link = "/home/u/.aws/credential-daemon.sock"
        val present = mapOf(link to SocketPathEntry.Present)

        // env absolute wins over the link
        assertEquals("/tmp/d.sock", path(mapOf("AWS_CREDENTIAL_DAEMON_SOCKET" to "/tmp/d.sock", "HOME" to "/home/u"), entries = present))
        // env absolute is used as is, with nothing checked at the path
        assertEquals("/tmp/d.sock", path(mapOf("AWS_CREDENTIAL_DAEMON_SOCKET" to "/tmp/d.sock")))
        // env relative or empty counts as unset
        assertEquals(link, path(mapOf("AWS_CREDENTIAL_DAEMON_SOCKET" to "rel/d.sock", "HOME" to "/home/u"), entries = present))
        assertEquals(link, path(mapOf("AWS_CREDENTIAL_DAEMON_SOCKET" to "", "HOME" to "/home/u"), entries = present))
        // link, home-socket: anything at the location applies, and the location is returned rather than a target
        assertEquals(link, path(mapOf("HOME" to "/home/u"), entries = present))
        assertEquals(link, path(mapOf("HOME" to "/home/u/"), entries = present))
        // the lookup is the same on macOS
        assertEquals(link, path(mapOf("HOME" to "/home/u"), mac, present))
        // the old locations are not checked
        val old = listOf(
            "/home/u/.local/share/aws-credential-daemon/daemon.sock",
            "/xdg/aws-credential-daemon/daemon.sock",
            "/run/aws-credential-daemon/daemon.sock",
            "/home/u/.local/share/credsd/credsd.sock",
            "/run/credsd/credsd.sock",
            "/var/run/credsd/credsd.sock",
        ).associateWith { SocketPathEntry.Present }
        noLocation(mapOf("HOME" to "/home/u", "XDG_DATA_HOME" to "/xdg"), old)

        // no-link
        noLocation(mapOf("HOME" to "/home/u"))
        // no-link-home-unset, empty HOME, no-link-home-relative: the location is skipped
        noLocation(emptyMap(), mapOf("/.aws/credential-daemon.sock" to SocketPathEntry.Present))
        noLocation(mapOf("HOME" to ""), mapOf("/.aws/credential-daemon.sock" to SocketPathEntry.Present))
        noLocation(mapOf("HOME" to "home/u"), mapOf("home/u/.aws/credential-daemon.sock" to SocketPathEntry.Present))
        // a relative env value does not stop the lookup from failing
        noLocation(mapOf("AWS_CREDENTIAL_DAEMON_SOCKET" to "rel/d.sock"))

        // the system property wins over the env var and the link, and is used as is
        val env = mapOf("AWS_CREDENTIAL_DAEMON_SOCKET" to "/tmp/env.sock", "HOME" to "/home/u")
        val prop = "aws.credentialDaemonSocket"
        assertEquals("/tmp/prop.sock", path(env, entries = present, props = mapOf(prop to "/tmp/prop.sock")))
        // a relative or empty system property counts as unset
        assertEquals("/tmp/env.sock", path(env, entries = present, props = mapOf(prop to "rel/prop.sock")))
        assertEquals("/tmp/env.sock", path(env, entries = present, props = mapOf(prop to "")))
        assertEquals(link, path(mapOf("HOME" to "/home/u"), entries = present, props = mapOf(prop to "rel/prop.sock")))

        // the provider setting wins over the system property, the env var and the link
        val props = mapOf(prop to "/tmp/prop.sock")
        assertEquals("/tmp/set.sock", path(env, entries = present, props = props, socketPath = "/tmp/set.sock"))
        // a relative or empty provider setting counts as unset
        assertEquals("/tmp/prop.sock", path(env, entries = present, props = props, socketPath = "rel/set.sock"))
        assertEquals("/tmp/prop.sock", path(env, entries = present, props = props, socketPath = ""))
        assertEquals("/tmp/env.sock", path(env, entries = present, socketPath = ""))
        noLocation(mapOf("HOME" to "/home/u"), socketPath = "rel/set.sock", props = mapOf(prop to "rel/prop.sock"))

        // link-dangling names the link and its target
        val dangling = assertFailsWith<CredentialDaemonException> {
            path(mapOf("HOME" to "/home/u"), entries = mapOf(link to SocketPathEntry.DanglingLink("/tmp/x/missing-daemon.sock")))
        }
        assertEquals(CredentialDaemonErrorCode.NOT_RUNNING, dangling.errorCode)
        assertTrue(dangling.message!!.contains(link), dangling.message)
        assertTrue(dangling.message!!.contains("/tmp/x/missing-daemon.sock"), dangling.message)
        assertTrue(dangling.message!!.contains(CREDENTIAL_DAEMON_DOC), dangling.message)
    }

    @Test
    fun testNullResultWithErrorIsDaemonError() {
        val reply = """{"jsonrpc":"2.0","id":1,"result":null,"error":{"code":-32000,"message":"denied","data":{"code":"REAUTH_REQUIRED"}}}"""
        val ex = assertError(CredentialDaemonErrorCode.REAUTH_REQUIRED, reply)
        assertTrue(ex.message!!.contains("denied"), ex.message)
    }

    @Test
    fun testResultWithNullErrorIsCredentials() {
        val reply = """{"jsonrpc":"2.0","id":1,""" +
            """"result":{"credential":"dev","material":{"type":"session_credentials","access_key_id":"AKID","secret_access_key":"SECRET"},"expires_at":"$expires"},""" +
            """"error":null}"""
        val creds = parseCredentialDaemonReply(reply.encodeToByteArray(), "dev")
        assertEquals("AKID", creds.accessKeyId)
        assertEquals("SECRET", creds.secretAccessKey)
    }

    @Test
    fun testReplyWithResultAndError() {
        val secret = "SECRET-SHOULD-NOT-LEAK"
        val reply = """{"jsonrpc":"2.0","id":1,""" +
            """"result":{"credential":"dev","material":{"type":"session_credentials","access_key_id":"AKID","secret_access_key":"$secret"},"expires_at":"$expires"},""" +
            """"error":{"code":-32000,"message":"denied","data":{"code":"REAUTH_REQUIRED"}}}"""
        val ex = assertError(CredentialDaemonErrorCode.PROTOCOL_ERROR, reply)
        assertFalse(ex.message!!.contains(secret), "message leaked the secret: ${ex.message}")
        assertFalse(ex.message!!.contains("denied"), "message leaked the error message: ${ex.message}")
    }
}
