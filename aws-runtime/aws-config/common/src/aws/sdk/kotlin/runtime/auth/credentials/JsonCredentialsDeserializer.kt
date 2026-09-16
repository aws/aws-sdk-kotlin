/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.ClientException
import aws.smithy.kotlin.runtime.serde.*
import aws.smithy.kotlin.runtime.serde.json.JsonSerialName
import aws.smithy.kotlin.runtime.serde.json.serialName
import aws.smithy.kotlin.runtime.time.Instant

/**
 * Exception thrown when credentials from response do not contain valid credentials or malformed JSON
 */
public class InvalidJsonCredentialsException(message: String, cause: Throwable? = null) : ClientException(message, cause)

/**
 * Common response elements for multiple HTTP credential providers (e.g. IMDS and ECS)
 */
internal sealed class JsonCredentialsResponse {
    /**
     * Credentials that can expire. A null [sessionToken] indicates long term credentials which do not expire (e.g.
     * static credentials returned by a credential process).
     */
    data class SessionCredentials(
        val accessKeyId: String,
        val secretAccessKey: String,
        val sessionToken: String?,
        val expiration: Instant?,
        val accountId: String? = null,
    ) : JsonCredentialsResponse()

    // TODO - add support for assume role credentials
    //   {
    //     // fields to construct STS client:
    //     "Region": "sts-region-name",
    //     "AccessKeyId" : "MUA...",
    //     "Expiration" : "2016-02-25T06:03:31Z", // optional
    //     "SecretAccessKey" : "/7PC5om....",
    //     "Token" : "AQoDY....=", // optional
    //     // fields controlling the STS role:
    //     "RoleArn": "...", // required
    //     "RoleSessionName": "...", // required
    //     // and also: Duration, ExternalId, SerialNumber, TokenCode, Policy
    //     ...
    //   }

    /**
     * Response successfully parsed as an error response
     */
    data class Error(val code: String?, val message: String?) : JsonCredentialsResponse()
}

/**
 * In general, the document looks something like:
 *
 * ```
 * {
 *     "Code" : "Success",
 *     "LastUpdated" : "2019-05-28T18:03:09Z",
 *     "Type" : "AWS-HMAC",
 *     "AccessKeyId" : "...",
 *     "SecretAccessKey" : "...",
 *     "Token" : "...",
 *     "Expiration" : "2019-05-29T00:21:43Z"
 * }
 * ```
 */
@Suppress("ktlint:standard:property-naming")
internal fun deserializeJsonCredentials(deserializer: Deserializer): JsonCredentialsResponse {
    val CODE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("Code"))
    val ACCESS_KEY_ID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("AccessKeyId"))
    val SECRET_ACCESS_KEY_ID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("SecretAccessKey"))
    val SESSION_TOKEN_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("Token"))
    val EXPIRATION_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Timestamp, JsonSerialName("Expiration"))
    val ACCOUNT_ID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("AccountId"))
    val MESSAGE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("Message"))

    val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        field(CODE_DESCRIPTOR)
        field(ACCESS_KEY_ID_DESCRIPTOR)
        field(SECRET_ACCESS_KEY_ID_DESCRIPTOR)
        field(SESSION_TOKEN_DESCRIPTOR)
        field(EXPIRATION_DESCRIPTOR)
        field(ACCOUNT_ID_DESCRIPTOR)
        field(MESSAGE_DESCRIPTOR)
    }

    var code: String? = null
    var accessKeyId: String? = null
    var secretAccessKey: String? = null
    var sessionToken: String? = null
    var expiration: Instant? = null
    var message: String? = null
    var accountId: String? = null

    try {
        deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
            loop@while (true) {
                when (findNextFieldIndex()) {
                    CODE_DESCRIPTOR.index -> code = deserializeString()
                    ACCESS_KEY_ID_DESCRIPTOR.index -> accessKeyId = deserializeString()
                    SECRET_ACCESS_KEY_ID_DESCRIPTOR.index -> secretAccessKey = deserializeString()
                    SESSION_TOKEN_DESCRIPTOR.index -> sessionToken = deserializeString()
                    EXPIRATION_DESCRIPTOR.index -> expiration = Instant.fromIso8601(deserializeString())
                    ACCOUNT_ID_DESCRIPTOR.index -> accountId = deserializeString()

                    // error responses
                    MESSAGE_DESCRIPTOR.index -> message = deserializeString()
                    null -> break@loop
                    else -> skipValue()
                }
            }
        }
    } catch (ex: DeserializationException) {
        throw InvalidJsonCredentialsException("invalid JSON credentials response", ex)
    }

    return when (code?.lowercase()) {
        // IMDS does not appear to reply with `Code` missing but documentation indicates it may be possible
        "success", null -> {
            if (accessKeyId == null) throw InvalidJsonCredentialsException("missing field `AccessKeyId`")
            if (secretAccessKey == null) throw InvalidJsonCredentialsException("missing field `SecretAccessKey`")
            if (sessionToken == null) throw InvalidJsonCredentialsException("missing field `Token`")
            if (expiration == null) throw InvalidJsonCredentialsException("missing field `Expiration`")
            JsonCredentialsResponse.SessionCredentials(accessKeyId!!, secretAccessKey!!, sessionToken!!, expiration!!, accountId)
        }
        else -> JsonCredentialsResponse.Error(code, message)
    }
}

/**
 * Deserialize credentials coming from process credentials. Used by [ProcessCredentialsProvider].
 * The difference between this and [deserializeJsonCredentials] is that process credentials _must_ provide a version field,
 * the session token field is called `SessionToken` instead of `Token`, and the session token and expiration fields are
 * optional.
 */
@Suppress("ktlint:standard:property-naming")
internal fun deserializeJsonProcessCredentials(deserializer: Deserializer): JsonCredentialsResponse {
    val ACCESS_KEY_ID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("AccessKeyId"))
    val SECRET_ACCESS_KEY_ID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("SecretAccessKey"))
    val SESSION_TOKEN_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("SessionToken"))
    val EXPIRATION_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Timestamp, JsonSerialName("Expiration"))
    val VERSION_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("Version"))
    val ACCOUNT_ID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("AccountId"))

    val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        field(ACCESS_KEY_ID_DESCRIPTOR)
        field(SECRET_ACCESS_KEY_ID_DESCRIPTOR)
        field(SESSION_TOKEN_DESCRIPTOR)
        field(EXPIRATION_DESCRIPTOR)
        field(VERSION_DESCRIPTOR)
        field(ACCOUNT_ID_DESCRIPTOR)
    }

    var accessKeyId: String? = null
    var secretAccessKey: String? = null
    var sessionToken: String? = null
    var expiration: Instant? = null
    var version: Int? = null
    var accountId: String? = null

    try {
        deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
            loop@while (true) {
                when (findNextFieldIndex()) {
                    ACCESS_KEY_ID_DESCRIPTOR.index -> accessKeyId = deserializeString()
                    SECRET_ACCESS_KEY_ID_DESCRIPTOR.index -> secretAccessKey = deserializeString()
                    SESSION_TOKEN_DESCRIPTOR.index -> sessionToken = deserializeString()
                    EXPIRATION_DESCRIPTOR.index -> expiration = Instant.fromIso8601(deserializeString())
                    VERSION_DESCRIPTOR.index -> version = deserializeInt()
                    ACCOUNT_ID_DESCRIPTOR.index -> accountId = deserializeString()
                    null -> break@loop
                    else -> skipValue()
                }
            }
        }
    } catch (ex: DeserializationException) {
        throw InvalidJsonCredentialsException("invalid JSON credentials response", ex)
    }

    if (accessKeyId == null) throw InvalidJsonCredentialsException("missing field `${ACCESS_KEY_ID_DESCRIPTOR.serialName}`")
    if (secretAccessKey == null) throw InvalidJsonCredentialsException("missing field `${SECRET_ACCESS_KEY_ID_DESCRIPTOR.serialName}`")
    if (version == null) throw InvalidJsonCredentialsException("missing field `${VERSION_DESCRIPTOR.serialName}`")
    if (version != 1) throw InvalidJsonCredentialsException("version $version is not supported")
    return JsonCredentialsResponse.SessionCredentials(accessKeyId!!, secretAccessKey!!, sessionToken, expiration, accountId)
}
