/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.accountId
import aws.sdk.kotlin.runtime.config.imds.EC2MetadataError
import aws.sdk.kotlin.runtime.util.VerifyingInstanceMetadataProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.WithDataTestName
import io.kotest.datatest.withData
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.fail

class ImdsCredentialsProviderTest :
    FunSpec({
        context("ImdsCredentialsProviderTest") {
            withData(testCases) { testCase ->
                println("**** TC: ${testCases.indexOf(testCase)}=${testCase.summary}")
                val imds = imds(testCase.expectations)

                val provider = ImdsCredentialsProvider(
                    instanceProfileName = testCase.config.profileName,
                    client = imds,
                    platformProvider = TestPlatformProvider(env = testCase.config.envVars),
                )

                testCase.outcomes.forEachIndexed { index, outcome ->
                    println("**** Outcome: $index=$outcome")
                    assertCredentials(provider, index, outcome)
                }

                imds.verifyComplete()
            }
        }
    }) {
    private companion object {
        val json = Json {
            @OptIn(ExperimentalSerializationApi::class)
            decodeEnumsCaseInsensitive = true
        }

        val testCases = json.decodeFromString<List<TestCase>>(imdsCredentialsTestSpec)

        fun imds(expectations: List<Expectation>) = VerifyingInstanceMetadataProvider(
            expectations.map { it.get to it.response.asStringProvider() },
        )

        fun Response.asStringProvider(): () -> String = {
            if (status == 200) requireNotNull(body) else throw EC2MetadataError(HttpStatusCode.fromValue(status), "err")
        }

        suspend fun assertCredentials(provider: ImdsCredentialsProvider, index: Int, outcome: Outcome) {
            val result = runCatching { provider.resolve() }.also { println("**** Got $it") }

            (result.exceptionOrNull() as? AssertionError)?.let { throw it } // Rethrow any failed assertions

            when (outcome.result) {
                Result.CREDENTIALS -> {
                    val creds = result.getOrNull() ?: fail("Test index $index: expected credentials but got $result")
                    assertEquals(creds.accountId, outcome.accountId, "Test index $index: Unexpected account ID value")
                }

                Result.NO_CREDENTIALS -> {
                    val ex = result.exceptionOrNull() ?: fail("Test index $index: Expected exception but got $result")
                    assertIs<CredentialsNotLoadedException>(ex, "Test index $index: Unexpected exception type $ex")
                }

                Result.INVALID_PROFILE -> {
                    val ex = result.exceptionOrNull() ?: fail("Test index $index: Expected exception but got $result")
                    assertIs<CredentialsProviderException>(ex, "Test index $index: Unexpected exception $ex")
                    val cause = assertNotNull(ex.cause, "Test index $index: Expected non-null exception cause")
                    assertIs<ImdsCredentialsException>(cause, "Test index $index: Unexpected cause $cause")
                }
            }
        }
    }
}

@Serializable
data class TestCase(
    val summary: String,
    val config: Config,
    val expectations: List<Expectation>,
    val outcomes: List<Outcome>,
) : WithDataTestName {
    override fun dataTestName() = summary
}

@Serializable
data class Config(
    @SerialName("ec2InstanceProfileName")
    val profileName: String? = null,

    val envVars: Map<String, String> = mapOf(),
)

@Serializable
data class Expectation(
    val get: String,
    val response: Response,
)

@Serializable
data class Response(
    val status: Int,

    @Serializable(with = StringOrObjectSerializer::class)
    val body: String? = null,
)

@Serializable
data class Outcome(
    val result: Result,
    val accountId: String? = null,
)

enum class Result {
    CREDENTIALS,

    @SerialName("no credentials")
    NO_CREDENTIALS,

    @SerialName("invalid profile")
    INVALID_PROFILE,
}

object StringOrObjectSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        SerialDescriptor("string-or-object", JsonElement.serializer().descriptor)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: error("This serializer only supports JSON")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> element.content
            is JsonObject -> element.toString()
            else -> error("Unsupported JSON type ${element::class}")
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}
