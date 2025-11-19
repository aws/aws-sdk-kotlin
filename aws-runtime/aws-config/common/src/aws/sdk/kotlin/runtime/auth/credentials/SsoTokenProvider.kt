/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.auth.credentials.internal.ssooidc.SsoOidcClient
import aws.sdk.kotlin.runtime.auth.credentials.internal.ssooidc.createToken
import aws.sdk.kotlin.runtime.auth.credentials.internal.ssooidc.model.CreateTokenResponse
import aws.sdk.kotlin.runtime.config.profile.normalizePath
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.emptyAttributes
import aws.smithy.kotlin.runtime.hashing.sha1
import aws.smithy.kotlin.runtime.http.auth.BearerToken
import aws.smithy.kotlin.runtime.http.auth.BearerTokenProvider
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.serde.json.*
import aws.smithy.kotlin.runtime.telemetry.logging.debug
import aws.smithy.kotlin.runtime.telemetry.logging.error
import aws.smithy.kotlin.runtime.telemetry.telemetryProvider
import aws.smithy.kotlin.runtime.text.encoding.encodeToHex
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.TimestampFormat
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.SingleFlightGroup
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val DEFAULT_SSO_TOKEN_REFRESH_BUFFER_SECONDS = 60 * 5
private const val OIDC_GRANT_TYPE_REFRESH = "refresh_token"

/**
 * SsoTokenProvider provides a utility for refreshing SSO AccessTokens for Bearer Authentication.
 * The provider can only be used to refresh already cached SSO Tokens. This utility cannot
 * perform the initial SSO create token flow.
 *
 * A utility such as the AWS CLI must be used to initially create the SSO session and cached token file  before the
 * application using the provider will need to retrieve the SSO token. If the token has not been cached already,
 * this provider will return an error when attempting to retrieve the token.
 * See [Configure SSO](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-sso.html)
 *
 *
 * This provider will attempt to refresh the cached SSO token periodically if needed when [resolve] is
 * called and a refresh token is available.
 *
 * @param ssoSessionName the name of the SSO Session from the shared config file to load tokens for
 * @param startUrl the start URL (also known as the "User Portal URL") provided by the SSO service
 * @param ssoRegion the AWS region where the SSO directory for the given [startUrl] is hosted.
 * @param refreshBufferWindow amount of time before the actual credential expiration time when credentials are
 * considered expired. For example, if credentials are expiring in 15 minutes, and the buffer time is 10 seconds,
 * then any requests made after 14 minutes and 50 seconds will load new credentials. Defaults to 5 minutes.
 * @param httpClient the [HttpClientEngine] instance to use to make requests. NOTE: This engine's resources and lifetime
 * are NOT managed by the provider. Caller is responsible for closing.
 * @param platformProvider the platform provider to use
 * @param clock the source of time for the provider
 */
public class SsoTokenProvider(
    public val ssoSessionName: String,
    public val startUrl: String,
    public val ssoRegion: String,
    public val refreshBufferWindow: Duration = DEFAULT_SSO_TOKEN_REFRESH_BUFFER_SECONDS.seconds,
    public val httpClient: HttpClientEngine? = null,
    public val platformProvider: PlatformProvider = PlatformProvider.System,
    private val clock: Clock = Clock.System,
) : BearerTokenProvider {

    // debounce concurrent requests for a token
    private val sfg = SingleFlightGroup<SsoToken>()

    override suspend fun resolve(attributes: Attributes): BearerToken = sfg.singleFlight {
        getToken(attributes)
    }

    private suspend fun getToken(attributes: Attributes): SsoToken {
        val token = readTokenFromCache(ssoSessionName, platformProvider)
        if (clock.now() < (token.expiration - refreshBufferWindow)) {
            coroutineContext.debug<SsoTokenProvider> { "using cached token for sso-session: $ssoSessionName" }
            return token
        }

        // token is within expiry window
        if (token.canRefresh) {
            return attemptRefresh(token)
        }

        return token.takeIf { clock.now() < it.expiration }?.also {
            coroutineContext.debug<SsoTokenProvider> { "cached token is not refreshable but still valid until ${it.expiration} for sso-session: $ssoSessionName" }
        } ?: throwTokenExpired()
    }
    private suspend fun attemptRefresh(oldToken: SsoToken): SsoToken {
        coroutineContext.debug<SsoTokenProvider> { "attempting to refresh token for sso-session: $ssoSessionName" }
        val result = runCatching { refreshToken(oldToken) }
        return result
            .onSuccess { refreshed -> writeToken(refreshed) }
            .getOrElse { cause ->
                if (clock.now() >= oldToken.expiration) {
                    coroutineContext.error<SsoTokenProvider>(cause) { "token refresh failed" }
                    throwTokenExpired(cause)
                }
                coroutineContext.debug<SsoTokenProvider> { "refresh token failed, original token is still valid until ${oldToken.expiration} for sso-session: $ssoSessionName, re-using" }
                oldToken
            }
    }

    private suspend fun writeToken(refreshed: SsoToken) {
        val cacheKey = getCacheFilename(ssoSessionName)
        val filepath = normalizePath(platformProvider.filepath("~", ".aws", "sso", "cache", cacheKey), platformProvider)
        try {
            val contents = serializeSsoToken(refreshed)
            platformProvider.writeFile(filepath, contents)
        } catch (ex: Exception) {
            coroutineContext.debug<SsoTokenProvider>(ex) { "failed to write refreshed token back to disk at $filepath" }
        }
    }

    private fun throwTokenExpired(cause: Throwable? = null): Nothing = throw InvalidSsoTokenException("SSO token for sso-session: $ssoSessionName is expired", cause)

    private suspend fun refreshToken(oldToken: SsoToken): SsoToken {
        val telemetry = coroutineContext.telemetryProvider
        SsoOidcClient.fromEnvironment {
            region = ssoRegion
            httpClient = this@SsoTokenProvider.httpClient
            telemetryProvider = telemetry
        }.use { client ->
            val resp = client.createToken {
                clientId = oldToken.clientId
                clientSecret = oldToken.clientSecret
                refreshToken = oldToken.refreshToken
                grantType = OIDC_GRANT_TYPE_REFRESH
            }

            return resp.toSsoToken(oldToken, clock)
        }
    }
}

internal fun PlatformProvider.filepath(vararg parts: String): String = parts.joinToString(separator = filePathSeparator)
internal suspend fun readTokenFromCache(cacheKey: String, platformProvider: PlatformProvider): SsoToken {
    val key = getCacheFilename(cacheKey)
    val bytes = with(platformProvider) {
        val defaultCacheLocation = normalizePath(filepath("~", ".aws", "sso", "cache"), this)
        readFileOrNull(filepath(defaultCacheLocation, key))
    } ?: throw ProviderConfigurationException("Invalid or missing SSO session cache. Run `aws sso login` to initiate a new SSO session")
    return deserializeSsoToken(bytes)
}
internal fun getCacheFilename(cacheKey: String): String {
    val sha1HexDigest = cacheKey.encodeToByteArray().sha1().encodeToHex()
    return "$sha1HexDigest.json"
}

internal data class SsoToken(
    override val token: String,
    override val expiration: Instant,
    val refreshToken: String? = null,
    val clientId: String? = null,
    val clientSecret: String? = null,
    val registrationExpiresAt: Instant? = null,
    val region: String? = null,
    val startUrl: String? = null,
) : BearerToken {
    override val attributes: Attributes = emptyAttributes()
}

private fun CreateTokenResponse.toSsoToken(oldToken: SsoToken, clock: Clock): SsoToken {
    val token = checkNotNull(accessToken) { "missing accessToken from CreateTokenResponse" }
    val expiresAt = clock.now() + expiresIn.seconds
    return oldToken.copy(token = token, expiration = expiresAt, refreshToken = refreshToken)
}

/**
 * Test if a token has the components to allow it to be refreshed for a new one
 */
private val SsoToken.canRefresh: Boolean
    get() = clientId != null && clientSecret != null && refreshToken != null

internal fun deserializeSsoToken(json: ByteArray): SsoToken {
    val lexer = jsonStreamReader(json)

    var accessToken: String? = null
    var expiresAtRfc3339: String? = null
    var refreshToken: String? = null
    var clientId: String? = null
    var clientSecret: String? = null
    var registrationExpiresAt: Instant? = null
    var region: String? = null
    var startUrl: String? = null

    try {
        lexer.nextTokenOf<JsonToken.BeginObject>()
        loop@while (true) {
            when (val token = lexer.nextToken()) {
                is JsonToken.EndObject -> break@loop
                is JsonToken.Name -> when (token.value) {
                    "accessToken" -> accessToken = lexer.nextTokenOf<JsonToken.String>().value
                    "expiresAt" -> expiresAtRfc3339 = lexer.nextTokenOf<JsonToken.String>().value
                    "refreshToken" -> refreshToken = lexer.nextTokenOf<JsonToken.String>().value
                    "clientId" -> clientId = lexer.nextTokenOf<JsonToken.String>().value
                    "clientSecret" -> clientSecret = lexer.nextTokenOf<JsonToken.String>().value
                    "registrationExpiresAt" -> registrationExpiresAt = lexer.nextTokenOf<JsonToken.String>().value.let { Instant.fromIso8601(it) }
                    "region" -> region = lexer.nextTokenOf<JsonToken.String>().value
                    "startUrl" -> startUrl = lexer.nextTokenOf<JsonToken.String>().value
                    else -> lexer.skipNext()
                }
                else -> error("expected either key or end of object")
            }
        }
    } catch (ex: Exception) {
        throw InvalidSsoTokenException("invalid cached SSO token", ex)
    }

    if (accessToken == null) throw InvalidSsoTokenException("missing `accessToken`")
    val expiresAt = expiresAtRfc3339?.let { Instant.fromIso8601(it) } ?: throw InvalidSsoTokenException("missing `expiresAt`")

    return SsoToken(
        accessToken,
        expiresAt,
        refreshToken,
        clientId,
        clientSecret,
        registrationExpiresAt,
        region,
        startUrl,
    )
}

internal fun serializeSsoToken(token: SsoToken): ByteArray =
    jsonStreamWriter(pretty = true).apply {
        beginObject()
        writeName("accessToken")
        writeValue(token.token)
        writeName("expiresAt")
        writeValue(token.expiration.format(TimestampFormat.ISO_8601))
        writeNotNull("refreshToken", token.refreshToken)
        writeNotNull("clientId", token.clientId)
        writeNotNull("clientSecret", token.clientSecret)
        writeNotNull("registrationExpiresAt", token.registrationExpiresAt?.let { it.format(TimestampFormat.ISO_8601) })
        writeNotNull("region", token.region)
        writeNotNull("startUrl", token.startUrl)
        endObject()
    }.bytes ?: error("serializing SsoToken failed")

internal fun JsonStreamWriter.writeNotNull(name: String, value: String?) {
    if (value == null) return
    writeName(name)
    writeValue(value)
}

/**
 * An error associated with a cached SSO token from `~/.aws/sso/cache/`
 */
public class InvalidSsoTokenException(message: String, cause: Throwable? = null) : ConfigurationException(message, cause)
