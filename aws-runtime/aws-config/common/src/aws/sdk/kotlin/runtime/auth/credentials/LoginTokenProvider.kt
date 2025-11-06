/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.sdk.kotlin.runtime.auth.credentials.internal.signin.SigninClient
import aws.sdk.kotlin.runtime.auth.credentials.internal.signin.createOAuth2Token
import aws.sdk.kotlin.runtime.config.profile.normalizePath
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.hashing.ecdsaSecp256r1Rs
import aws.smithy.kotlin.runtime.hashing.sha256
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.header
import aws.smithy.kotlin.runtime.http.request.toBuilder
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.serde.json.JsonToken
import aws.smithy.kotlin.runtime.serde.json.jsonStreamReader
import aws.smithy.kotlin.runtime.serde.json.jsonStreamWriter
import aws.smithy.kotlin.runtime.serde.json.nextTokenOf
import aws.smithy.kotlin.runtime.telemetry.logging.debug
import aws.smithy.kotlin.runtime.telemetry.logging.error
import aws.smithy.kotlin.runtime.telemetry.telemetryProvider
import aws.smithy.kotlin.runtime.text.encoding.decodeBase64Bytes
import aws.smithy.kotlin.runtime.text.encoding.encodeToHex
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.TimestampFormat
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.SingleFlightGroup
import aws.smithy.kotlin.runtime.util.Uuid
import aws.smithy.kotlin.runtime.text.encoding.encodeBase64String
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.io.encoding.Base64
import kotlin.io.encoding.Base64.Default.UrlSafe

private const val DEFAULT_SIGNIN_TOKEN_REFRESH_BUFFER_SECONDS = 60 * 5
//internal class PrintAllHeadersInterceptor : HttpInterceptor {
//
//    override suspend fun modifyBeforeTransmit(context: ProtocolRequestInterceptorContext<Any, HttpRequest>): HttpRequest {
//        context.protocolRequest.headers.entries().forEach { (name, values) ->
//            println("$name: ${values.joinToString(", ")}")
//        }
//        return context.protocolRequest
//    }
//}


// HTTP interceptor that adds DPoP (Demonstration of Proof-of-Possession) headers to requests.
internal class DpopInterceptor(private val dpopKeyPem: String) : HttpInterceptor {
    override suspend fun modifyBeforeTransmit(context: ProtocolRequestInterceptorContext<Any, HttpRequest>): HttpRequest {
        val endpoint = extractRequestEndpoint(context.protocolRequest)
        val dpopHeader = generateDpopProof(dpopKeyPem, endpoint)

        val request = context.protocolRequest.toBuilder()
        println("Setting DPoP header: $dpopHeader")

        request.header("DPoP", dpopHeader)
        return request.build()
    }

    // extracts the full request endpoint URL for use in DPoP proof generation.
    private fun extractRequestEndpoint(request: HttpRequest): String {
        val url = request.url
        return url.toString()
//        val port = if (isStandardPort(url.scheme, url.port)) "" else ":${url.port}"
//        return "${url.scheme}://${url.host}$port${url.encodedPath}"
    }
}

/**
 * LoginTokenProvider provides a utility for refreshing AWS Login tokens for credential authentication.
 * The provider can only be used to refresh already cached login tokens. This utility cannot
 * perform the initial login flow.
 *
 * A utility such as the AWS CLI must be used to initially create the login session and cached token file before the
 * application using the provider will need to retrieve the login token. If the token has not been cached already,
 * this provider will return an error when attempting to retrieve the token.
 * See [Configure AWS Login](doc link TBD)
 *
 * This provider will attempt to refresh the cached login token periodically if needed when [resolve] is
 * called and a refresh token is available.
 *
 * @param loginSessionName the name of the login session from the shared config file to load tokens for
 * @param refreshBufferWindow amount of time before the actual credential expiration time when credentials are
 * considered expired. For example, if credentials are expiring in 15 minutes, and the buffer time is 10 seconds,
 * then any requests made after 14 minutes and 50 seconds will load new credentials. Defaults to 5 minutes.
 * @param httpClient the [HttpClientEngine] instance to use to make requests. NOTE: This engine's resources and lifetime
 * are NOT managed by the provider. Caller is responsible for closing.
 * @param platformProvider the platform provider to use
 * @param clock the source of time for the provider
 */
public class LoginTokenProvider (
    public val loginSessionName: String,
    public val refreshBufferWindow: Duration = DEFAULT_SIGNIN_TOKEN_REFRESH_BUFFER_SECONDS.seconds,
    public val httpClient: HttpClientEngine? = null,
    public val platformProvider: PlatformProvider = PlatformProvider.System,
    private val clock: Clock = Clock.System,
): CredentialsProvider {

    // debounce concurrent requests for a token
    private val sfg = SingleFlightGroup<LoginToken>()

    override suspend fun resolve(attributes: Attributes): Credentials {
        val token = sfg.singleFlight { getToken(attributes) }

        return credentials(
            accessKeyId = token.accessKeyId,
            secretAccessKey = token.secretAccessKey,
            sessionToken = token.sessionToken,
            expiration = token.expiresAt,
            accountId = token.accountId
        )
    }

    private suspend fun getToken(attributes: Attributes): LoginToken {
        val token = readLoginTokenFromCache(loginSessionName, platformProvider)

        if (clock.now() < (token.expiresAt - refreshBufferWindow)) {
            coroutineContext.debug<LoginTokenProvider> { "using cached token for login-session: $loginSessionName" }
            return token
        }

        // token is within expiry window
        if (token.canRefresh) {
            return attemptRefresh(token)
        }

        return token.takeIf { clock.now() < it.expiresAt }?.also {
            coroutineContext.debug<LoginTokenProvider> { "cached token is not refreshable but still valid until ${it.expiresAt} for login-session: $loginSessionName" }
        } ?: throwTokenExpired()
        return token
    }

    private suspend fun attemptRefresh(oldToken: LoginToken): LoginToken {
        coroutineContext.debug<LoginTokenProvider> { "attempting to refresh token for login-session: $loginSessionName" }
        val result = runCatching { refreshToken(oldToken) }
        return result
            .onSuccess { refreshed -> writeToken(refreshed) }
            .getOrElse { cause ->
                if (clock.now() >= oldToken.expiresAt) {
                    coroutineContext.error<LoginTokenProvider>(cause) { "token refresh failed" }
                    throwTokenExpired(cause)
                }
                coroutineContext.debug<LoginTokenProvider> { "refresh token failed, original token is still valid until ${oldToken.expiresAt} for login-session: $loginSessionName, re-using" }
                oldToken
            }
    }

    private suspend fun writeToken(refreshed: LoginToken) {
        val cacheKey = getLoginCacheFilename(loginSessionName)
        val filepath = normalizePath(platformProvider.filepath("~", ".aws", "login", "cache", cacheKey), platformProvider)
        try {
            //println("attempting to write refreshed token")
            val contents = serializeLoginToken(refreshed)
            //println("contents: "+ contents.decodeToString())
            platformProvider.writeFile(filepath, contents)
        } catch (ex: Exception) {
            coroutineContext.debug<LoginTokenProvider>(ex) { "failed to write refreshed token back to disk at $filepath" }
        }
    }

    private fun throwTokenExpired(cause: Throwable? = null): Nothing = throw InvalidLoginTokenException("Login token for login-session: $loginSessionName is expired", cause)

    private suspend fun refreshToken(oldToken: LoginToken): LoginToken {
        val telemetry = coroutineContext.telemetryProvider
        println("attempting to refresh token")
        SigninClient.fromEnvironment {
            httpClient = this@LoginTokenProvider.httpClient
            telemetryProvider = telemetry
            endpointUrl = Url.parse("https://ap-northeast-1.aws-signin-testing.amazon.com") //TODO: use testing endpoint, remove this once service prod endpoint is available
            interceptors += DpopInterceptor(oldToken.dpopKey) // note for implementer: this is for writing DpopProof in request header instead of sending in request
            //interceptors += PrintAllHeadersInterceptor()
        }.use { client ->
            val result = client.createOAuth2Token {
                dpopProof = generateDpopProof(oldToken.dpopKey!!, "https://ap-northeast-1.aws-signin-testing.amazon.com/v1/token") //TODO: remove this line once login model remove dpopproof field
                tokenInput {
                    clientId = oldToken.clientId
                    grantType = "refresh_token"
                    refreshToken = oldToken.refreshToken
                }
            }

            //TODO: use model provided exception:
            // If the CreateOAuth2Token call returns a TBD error with the TBD member set to TBD, then the SDK MUST return an error with TBD wording.
            return try {
                oldToken.copy(
                    accessKeyId = result.tokenOutput!!.accessToken!!.accessKeyId,
                    secretAccessKey = result.tokenOutput!!.accessToken!!.secretAccessKey,
                    sessionToken = result.tokenOutput!!.accessToken!!.sessionToken,
                    expiresAt = clock.now() + result.tokenOutput?.expiresIn!!.seconds,
                    refreshToken = result.tokenOutput!!.refreshToken
                )
            } catch (e: Exception) {
                throw InvalidLoginTokenException("Failed to parse token response", e)
            }
        }
    }
}

internal data class ECKeyData(
    val d: ByteArray,  // private key scalar
    val x: ByteArray,  // public key x coordinate
    val y: ByteArray   // public key y coordinate
)

/**
 * Parses a PEM-encoded EC private key and extracts the private key scalar and public key coordinates.
 * Supports both "EC PRIVATE KEY" and "PRIVATE KEY" PEM formats for P-256 curve keys.
 */
private fun parseECKeyPem(pem: String): ECKeyData {
    val base64 = pem.replace("-----BEGIN EC PRIVATE KEY-----", "")
        .replace("-----END EC PRIVATE KEY-----", "")
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replace("\\s".toRegex(), "")
        .replace("\n", "")
        .replace("\r", "")

    val der = base64.decodeBase64Bytes()
    println("DER hex: ${der.encodeToHex()}")
    // Extract private key scalar (32 bytes at offset 7)
    val d = der.copyOfRange(7, 39)

    // Find public key coordinates (look for 0x04 prefix after offset 40)
    var publicKeyStart = -1
    for (i in 40 until der.size) {
        if (der[i] == 0x04.toByte()) {
            publicKeyStart = i + 1
            println("Found 0x04 at position $i, public key starts at ${i + 1}")
            break
        }
    }

    val remainingBytes = der.size - publicKeyStart
    val coordLen = remainingBytes / 2
    println("Public key section: ${der.copyOfRange(publicKeyStart - 1, der.size).encodeToHex()}")

    val x = der.copyOfRange(publicKeyStart, publicKeyStart + coordLen).padTo32()
    val y = der.copyOfRange(publicKeyStart + coordLen, publicKeyStart + 2 * coordLen).padTo32()
    println("Raw x (${x.size} bytes): ${x.encodeToHex()}")
    println("Raw y (${y.size} bytes): ${y.encodeToHex()}")

    println("d: ${d.encodeBase64String()}")
    println("x: ${x.encodeBase64String()}")
    println("y: ${y.encodeBase64String()}")
    return ECKeyData(d, x, y)
}

private fun ByteArray.padTo32(): ByteArray {
    return if (size >= 32) {
        takeLast(32).toByteArray()
    } else {
        ByteArray(32 - size) + this
    }
}

/**
 * Generates a DPoP (Demonstration of Proof-of-Possession) JWT proof for OAuth 2.0 requests.
 * Creates a signed JWT with the required claims (jti, htm, htu, iat) using ES256 algorithm.
 */
private fun generateDpopProof(
    privateKeyPem: String,
    endpoint: String,
): String {
    val ecKeyData = parseECKeyPem(privateKeyPem)

    val base64UrlNoPadding = UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    val header = jsonStreamWriter().apply {
        beginObject()
        writeName("typ")
        writeValue("dpop+jwt")
        writeName("alg")
        writeValue("ES256")
        writeName("jwk")
        beginObject()
        writeName("kty")
        writeValue("EC")
        writeName("x")
        //writeValue(xB64)
        writeValue(base64UrlNoPadding.encode(ecKeyData.x))
        writeName("y")
        //writeValue(yB64)
        writeValue(base64UrlNoPadding.encode(ecKeyData.y))
        writeName("crv")
        writeValue("P-256")
        endObject()
        endObject()
    }.bytes
    println("header: ${header?.decodeToString()}")
    val payload = jsonStreamWriter().apply {
        beginObject()
        writeName("jti")
        writeValue(Uuid.random().toString())
        writeName("htm")
        writeValue("POST")
        writeName("htu")
        writeValue(endpoint) // hardcoded test endpoint, TODO: change it
        writeName("iat")
        writeValue(System.currentTimeMillis() / 1000)
        endObject()
    }.bytes
    println("payload: ${payload?.decodeToString()}")

    val headerEncoded = base64UrlNoPadding.encode(header!!)
    val payloadEncoded = base64UrlNoPadding.encode(payload!!)
    val message = "$headerEncoded.$payloadEncoded"
    println("message: $message")

    val privateKeyBytes = ecKeyData.d
    val signature = ecdsaSecp256r1Rs(privateKeyBytes, message.encodeToByteArray())

    println("signature hex: ${signature.encodeToHex()}")
    println("signature length: ${signature.size}")

    return "$message.${ base64UrlNoPadding.encode(signature) }"
}

internal suspend fun readLoginTokenFromCache(cacheKey: String, platformProvider: PlatformProvider): LoginToken {
    val key = getLoginCacheFilename(cacheKey)
    val bytes = with(platformProvider) {
        val directory = getenv("AWS_LOGIN_IN_CACHE_DIRECTORY") ?: filepath("~", ".aws", "login", "cache")
        val defaultCacheLocation = normalizePath(directory, this)
        readFileOrNull(filepath(defaultCacheLocation, key))
    } ?: throw ProviderConfigurationException("Invalid or missing login session cache. Run `aws login` to initiate a new session")
    return deserializeLoginToken(bytes)
}

internal fun getLoginCacheFilename(cacheKey: String): String {
    val sha256HexDigest = cacheKey.trim().encodeToByteArray().sha256().encodeToHex()
    return "$sha256HexDigest.json"
}

internal data class LoginToken(
    val accessKeyId: String,
    val secretAccessKey: String,
    val sessionToken: String,
    val accountId: String,
    val tokenType: String? = null,
    val expiresAt: Instant,
    val refreshToken: String,
    val idToken: String? = null,
    val clientId: String?,
    val dpopKey: String,
)

/**
 * Test if a token has the components to allow it to be refreshed for a new one
 */
private val LoginToken.canRefresh: Boolean
    get() = clientId != null && dpopKey != null && refreshToken != null

internal fun deserializeLoginToken(json: ByteArray): LoginToken {
    val lexer = jsonStreamReader(json)

    var sessionToken: String? = null
    var accessKeyId: String? = null
    var secretAccessKey: String? = null
    var accountId: String? = null
    var tokenType: String? = null
    var expiresAtRfc3339: String? = null
    var refreshToken: String? = null
    var idToken: String? = null
    var clientId: String? = null
    var dpopKey: String? = null

    try {
        lexer.nextTokenOf<JsonToken.BeginObject>()
        loop@while (true) {
            when (val token = lexer.nextToken()) {
                is JsonToken.EndObject -> break@loop
                is JsonToken.Name -> when (token.value) {
                    "accessToken" -> {
                        lexer.nextTokenOf<JsonToken.BeginObject>()
                        while (true) {
                            when (val nestedToken = lexer.nextToken()) {
                                is JsonToken.EndObject -> break
                                is JsonToken.Name -> when (nestedToken.value) {
                                    "accessKeyId" -> accessKeyId = lexer.nextTokenOf<JsonToken.String>().value
                                    "secretAccessKey" -> secretAccessKey = lexer.nextTokenOf<JsonToken.String>().value
                                    "sessionToken" -> sessionToken = lexer.nextTokenOf<JsonToken.String>().value
                                    "expiresAt" -> expiresAtRfc3339 = lexer.nextTokenOf<JsonToken.String>().value
                                    "accountId" -> accountId = lexer.nextTokenOf<JsonToken.String>().value
                                    else -> lexer.skipNext()
                                }
                                else -> error("expected key or end of object in accessToken")
                            }
                        }
                    }
                    "tokenType" -> tokenType = lexer.nextTokenOf<JsonToken.String>().value
                    "refreshToken" -> refreshToken = lexer.nextTokenOf<JsonToken.String>().value
                    "idToken" -> idToken = lexer.nextTokenOf<JsonToken.String>().value
                    "clientId" -> clientId = lexer.nextTokenOf<JsonToken.String>().value
                    "dpopKey" -> dpopKey = lexer.nextTokenOf<JsonToken.String>().value
                    else -> lexer.skipNext()
                }
                else -> error("expected either key or end of object")
            }
        }
    } catch (ex: Exception) {
        throw InvalidLoginTokenException("invalid cached login token", ex)
    }

    if (accessKeyId == null) throw InvalidLoginTokenException("missing `accessKeyId`")
    if (secretAccessKey == null) throw InvalidLoginTokenException("missing `secretAccessKey`")
    if (sessionToken == null) throw InvalidLoginTokenException("missing `sessionToken`")
    if (accountId == null) throw InvalidLoginTokenException("missing `accountId`")
    val expiresAt = expiresAtRfc3339?.let { Instant.fromIso8601(it) } ?: throw InvalidLoginTokenException("missing `expiresAt`")
    if (clientId == null) throw InvalidLoginTokenException("missing `clientId`")
    if (refreshToken == null) throw InvalidLoginTokenException("missing `refreshToken`")
    if (dpopKey == null) throw InvalidLoginTokenException("missing `dpopKey`")

    return LoginToken(
        accessKeyId,
        secretAccessKey,
        sessionToken,
        accountId,
        tokenType,
        expiresAt,
        refreshToken,
        idToken,
        clientId,
        dpopKey,
    )
}

internal fun serializeLoginToken(token: LoginToken): ByteArray =
    jsonStreamWriter(pretty = true).apply {
        beginObject()
        writeName("accessToken")
        beginObject()
        writeNotNull("accessKeyId", token.accessKeyId)
        writeNotNull("secretAccessKey", token.secretAccessKey)
        writeNotNull("sessionToken", token.sessionToken)
        writeNotNull("accountId", token.accountId)
        writeNotNull("expiresAt", token.expiresAt.format(TimestampFormat.ISO_8601))
        endObject()
        writeNotNull("tokenType", token.tokenType)
        writeNotNull("refreshToken", token.refreshToken)
        writeNotNull("idToken", token.idToken)
        writeNotNull("clientId", token.clientId)
        writeNotNull("dpopKey", token.dpopKey)
        endObject()
    }.bytes ?: error("serializing LoginToken failed")

public class InvalidLoginTokenException(message: String, cause: Throwable? = null) : ConfigurationException(message, cause)
