/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.middleware

import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.sdk.kotlin.runtime.endpoint.Endpoint
import aws.sdk.kotlin.runtime.endpoint.EndpointResolver
import aws.sdk.kotlin.runtime.execution.AuthAttributes
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import software.aws.clientrt.http.*
import software.aws.clientrt.http.engine.HttpClientEngine
import software.aws.clientrt.http.operation.*
import software.aws.clientrt.http.request.HttpRequestBuilder
import software.aws.clientrt.http.response.HttpResponse
import software.aws.clientrt.util.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalStdlibApi::class)
class ServiceEndpointResolverTest {

    @Test
    fun `it sets the host to the expected endpoint`(): Unit = runSuspendTest {
        val expectedHost = "test.com"
        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(requestBuilder: HttpRequestBuilder): HttpResponse {
                assertEquals(expectedHost, requestBuilder.url.host)
                assertEquals(expectedHost, requestBuilder.headers["Host"])
                assertEquals("https", requestBuilder.url.scheme.protocolName)
                return HttpResponse(HttpStatusCode.fromValue(200), Headers {}, HttpBody.Empty, requestBuilder.build())
            }
        }

        val client = sdkHttpClient(mockEngine)

        val op = SdkHttpOperation.build<Unit, HttpResponse> {
            serializer = UnitSerializer
            deserializer = IdentityDeserializer
            context {
                service = "TestService"
                operationName = "testOperation"
            }
        }

        op.install(ServiceEndpointResolver) {
            serviceId = "TestService"
            resolver = object : EndpointResolver {
                override suspend fun resolve(service: String, region: String): Endpoint {
                    return Endpoint("test.com", "https")
                }
            }
        }

        op.context[AwsClientOption.Region] = "us-east-1"
        val response = op.roundTrip(client, Unit)
        assertNotNull(response)
    }

    @Test
    fun `it prepends hostPrefix when present`(): Unit = runSuspendTest {
        val expectedHost = "prefix.test.com"
        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(requestBuilder: HttpRequestBuilder): HttpResponse {
                assertEquals(expectedHost, requestBuilder.url.host)
                return HttpResponse(HttpStatusCode.fromValue(200), Headers {}, HttpBody.Empty, requestBuilder.build())
            }
        }

        val client = sdkHttpClient(mockEngine)

        val op = SdkHttpOperation.build<Unit, HttpResponse> {
            serializer = UnitSerializer
            deserializer = IdentityDeserializer
            context {
                service = "TestService"
                operationName = "testOperation"
                set(AwsClientOption.Region, "us-east-1")
                set(HttpOperationContext.HostPrefix, "prefix.")
            }
        }
        op.install(ServiceEndpointResolver) {
            serviceId = "TestService"
            resolver = object : EndpointResolver {
                override suspend fun resolve(service: String, region: String): Endpoint {
                    return Endpoint("test.com", "https")
                }
            }
        }

        val response = op.roundTrip(client, Unit)
        assertNotNull(response)
    }

    @Test
    fun `it overrides credential scopes`(): Unit = runSuspendTest {
        // if an endpoint specifies credential scopes we should override the context
        val expectedHost = "test.com"
        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(requestBuilder: HttpRequestBuilder): HttpResponse {
                assertEquals(expectedHost, requestBuilder.url.host)
                assertEquals(expectedHost, requestBuilder.headers["Host"])
                assertEquals("https", requestBuilder.url.scheme.protocolName)
                return HttpResponse(HttpStatusCode.fromValue(200), Headers {}, HttpBody.Empty, requestBuilder.build())
            }
        }

        val client = sdkHttpClient(mockEngine)

        val op = SdkHttpOperation.build<Unit, HttpResponse> {
            serializer = UnitSerializer
            deserializer = IdentityDeserializer
            context {
                service = "TestService"
                operationName = "testOperation"
            }
        }

        op.install(ServiceEndpointResolver) {
            serviceId = "TestService"
            resolver = object : EndpointResolver {
                override suspend fun resolve(service: String, region: String): Endpoint {
                    return Endpoint("test.com", "https", signingName = "foo", signingRegion = "us-west-2")
                }
            }
        }

        op.context[AwsClientOption.Region] = "us-east-1"
        op.context[AuthAttributes.SigningRegion] = "us-east-1"
        op.context[AuthAttributes.SigningService] = "quux"
        op.roundTrip(client, Unit)
        assertEquals("foo", op.context[AuthAttributes.SigningService])
        assertEquals("us-west-2", op.context[AuthAttributes.SigningRegion])
    }
}
