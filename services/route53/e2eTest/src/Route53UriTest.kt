/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.services.route53

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.httptest.TestEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class Route53UriTest {
    /**
     * Validates that HostedZoneId isn't trimmed when not prefixed
     */
    @Test
    fun listResourceRecordSetsNoTrim() = runTest {
        Route53Client {
            region = "us-east-1"
            httpClient = TestEngine()
            interceptors = mutableListOf(
                AssertUrlInterceptor(
                    expectedUrl = "/0000-00-00/hostedzone/IDOFMYHOSTEDZONE/rrset",
                ),
            )
            credentialsProvider = StaticCredentialsProvider(Credentials("AKID", "SECRETAK"))
        }.use { client ->
            client.listResourceRecordSets {
                hostedZoneId = "IDOFMYHOSTEDZONE"
            }
        }
    }

    /**
     * Validates that HostedZoneId is trimmed
     */
    @Test
    fun listResourceRecordSetsTrim() = runTest {
        Route53Client {
            region = "us-east-1"
            httpClient = TestEngine()
            interceptors = mutableListOf(
                AssertUrlInterceptor(
                    expectedUrl = "/0000-00-00/hostedzone/IDOFMYHOSTEDZONE/rrset",
                ),
            )
            credentialsProvider = StaticCredentialsProvider(Credentials("AKID", "SECRETAK"))
        }.use { client ->
            client.listResourceRecordSets {
                hostedZoneId = "hostedzone/IDOFMYHOSTEDZONE"
            }
        }
    }

    /**
     * Validates that HostedZoneId is trimmed even with a leading slash
     */
    @Test
    fun listResourceRecordSetsTrimLeadingSlash() = runTest {
        Route53Client {
            region = "us-east-1"
            httpClient = TestEngine()
            interceptors = mutableListOf(
                AssertUrlInterceptor(
                    expectedUrl = "/0000-00-00/hostedzone/IDOFMYHOSTEDZONE/rrset",
                ),
            )
            credentialsProvider = StaticCredentialsProvider(Credentials("AKID", "SECRETAK"))
        }.use { client ->
            client.listResourceRecordSets {
                hostedZoneId = "/hostedzone/IDOFMYHOSTEDZONE"
            }
        }
    }

    /**
     * Validates that HostedZoneId isn't over-trimmed
     */
    @Test
    fun listResourceRecordSetsTrimMultiSlash() = runTest {
        Route53Client {
            region = "us-east-1"
            httpClient = TestEngine()
            interceptors = mutableListOf(
                AssertUrlInterceptor(
                    expectedUrl = "/0000-00-00/hostedzone/IDOFMY%2FHOSTEDZONE/rrset",
                ),
            )
            credentialsProvider = StaticCredentialsProvider(Credentials("AKID", "SECRETAK"))
        }.use { client ->
            client.listResourceRecordSets {
                hostedZoneId = "/hostedzone/IDOFMY/HOSTEDZONE"
            }
        }
    }

    /**
     * This test validates that change id is correctly trimmed
     */
    @Test
    fun getChangeTrimChangeId() = runTest {
        Route53Client {
            region = "us-east-1"
            httpClient = TestEngine()
            interceptors = mutableListOf(
                AssertUrlInterceptor(
                    expectedUrl = "/0000-00-00/change/SOMECHANGEID",
                ),
            )
            credentialsProvider = StaticCredentialsProvider(Credentials("AKID", "SECRETAK"))
        }.use { client ->
            client.getChange {
                id = "/change/SOMECHANGEID"
            }
        }
    }

    /**
     * This test validates that delegation set id is correctly trimmed
     */
    @Test
    fun getReusableDelegationSetTrimDelegationSetId() = runTest {
        Route53Client {
            region = "us-east-1"
            httpClient = TestEngine()
            interceptors = mutableListOf(
                AssertUrlInterceptor(
                    expectedUrl = "/0000-00-00/delegationset/DELEGATIONSETID",
                ),
            )
            credentialsProvider = StaticCredentialsProvider(Credentials("AKID", "SECRETAK"))
        }.use { client ->
            client.getReusableDelegationSet {
                id = "/delegationset/DELEGATIONSETID"
            }
        }
    }

    /**
     * Model version can change the URL used as it's included in the URL.
     * This interceptor removes the model version from the expected and actual URLs.
     * Then performs an equality assertion between the two.
     *
     * https://github.com/awslabs/aws-sdk-kotlin/issues/1370
     */
    private class AssertUrlInterceptor(private val expectedUrl: String) : HttpInterceptor {
        override fun readBeforeTransmit(context: ProtocolRequestInterceptorContext<Any, HttpRequest>) {
            val actualUrl = context.protocolRequest.url.path.toString()

            val parsedActualUrl = removeModelVersion(actualUrl)
            val parsedExpectedUrl = removeModelVersion(expectedUrl)

            assertEquals(parsedExpectedUrl, parsedActualUrl)
        }

        private fun removeModelVersion(url: String) =
            try {
                url.replaceFirst(
                    Regex("^/\\d{4}-\\d{2}-\\d{2}/"),
                    "//",
                )
            } catch (e: Exception) {
                fail("The URL '$url' is not in the expected format", e)
            }
    }
}
