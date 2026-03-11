/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.testutils

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.deleteTable
import aws.sdk.kotlin.services.dynamodb.waiters.waitUntilTableNotExists
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.net.Host
import aws.smithy.kotlin.runtime.net.Scheme
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.testing.AfterAll
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

/**
 * The base class for test classes which need DynamoDB Local running. This class provides a few convenience declarations
 * for subclasses:
 * * [ddb] — An instance of a low-level [DynamoDbClient] utilizing the DynamoDB Local instance which may be used for
 *   setting up or verifying various mapper tests
 * * [createTable] — Creates a table for the given name/schema and optionally persists the given items into it. All
 *   tables created via this method will be automatically dropped when the test spec completes (whether successfully or
 *   unsuccessfully).
 * * [mapper] — Returns a [DynamoDbMapper] instance utilizing the DynamoDB Local instance
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class DdbLocalTest {
    companion object {
        private const val DOUBLE_TOLERANCE = 0.000001

        fun assertEquals(expected: Double, actual: Double?) {
            assertNotNull(actual)
            assertEquals(expected, actual, DOUBLE_TOLERANCE)
        }
    }

    private val requests = mutableListOf<HttpRequest>()
    private val requestInterceptor = RequestCapturingInterceptor(this@DdbLocalTest.requests)

    private val ddbHolder = lazy {
        val portFilePath = "build/ddblocal/port.info" // Keep in sync with build.gradle.kts
        println("Reading DDB Local port info from $portFilePath")
        val portFileContents = requireNotNull(runBlocking { PlatformProvider.System.readFileOrNull(portFilePath) }) {
            "Failed to locate $portFilePath containing DDB Local port specification"
        }
        val port = portFileContents.decodeToString().toInt()
        println("Connecting to DDB Local on port $port")

        DynamoDbClient {
            endpointUrl = Url {
                scheme = Scheme.HTTP
                host = Host.Domain("localhost")
                this.port = port
            }

            region = "DUMMY"

            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = "DUMMY"
                secretAccessKey = "DUMMY"
            }

            interceptors += requestInterceptor
        }
    }

    /**
     * An instance of a low-level [DynamoDbClient] utilizing the DynamoDB Local instance which may be used for setting
     * up or verifying various mapper tests. If this is the first time accessing the value, the client will be
     * initialized.
     *
     * **Important**: This low-level client should only be accessed via [lowLevelAccess] to ensure that User-Agent
     * header verification succeeds.
     */
    private val ddb by ddbHolder

    private val tempTables = mutableListOf<String>()

    /**
     * Creates a table for the given name/schema and optionally persists the given items into it. All tables created via
     * this method will be automatically dropped when the test spec completes (whether successfully or unsuccessfully).
     * @param name The name of the table to create
     * @param schema The schema for the table. This is used to derive the primary key and to map the [items] (if any).
     * @param items A collection of maps of strings to values which will be mapped and persisted to the new table before
     * returning. This can be used to pre-populate a table for a test.
     */
    suspend fun createTable(name: String, schema: ItemSchema<*>, vararg items: Item) = createTable(name, schema, mapOf(), mapOf(), items.toList())

    /**
     * Creates a table for the given name/schema and optionally persists the given items into it. All tables created via
     * this method will be automatically dropped when the test spec completes (whether successfully or unsuccessfully).
     * @param name The name of the table to create
     * @param schema The schema for the table. This is used to derive the primary key and to map the [items] (if any).
     * @param gsis A map of GSI names to schemas
     * @param lsis A map of LSI names to schemas
     * @param items A collection of maps of strings to values which will be mapped and persisted to the new table before
     * returning. This can be used to pre-populate a table for a test.
     */
    suspend fun createTable(
        name: String,
        schema: ItemSchema<*>,
        gsis: Map<String, ItemSchema<*>>,
        lsis: Map<String, ItemSchema<*>>,
        items: List<Item>,
    ) {
        lowLevelAccess {
            createTable(name, schema, gsis, lsis)
            tempTables += name
            putItems(name, items)
        }
    }

    /**
     * Returns a [DynamoDbMapper] instance utilizing the DynamoDB Local instance
     * @param config A function to set the configuration of the mapper before it's built
     */
    fun mapper(
        ddb: DynamoDbClient? = null,
        config: DynamoDbMapper.Config.Builder.() -> Unit = { },
    ) = DynamoDbMapper(ddb ?: this.ddb, config)

    @BeforeTest
    fun initializeTest() {
        requestInterceptor.enabled = true
    }

    /**
     * Executes requests on a low-level [DynamoDbClient] and _does not_ log any requests executed in [block]. (This
     * skips verifying that low-level requests contain the [AwsBusinessMetric.DDB_MAPPER] metric.)
     */
    protected suspend fun <T> lowLevelAccess(block: suspend DynamoDbClient.() -> T): T {
        requestInterceptor.enabled = false
        return block(ddb).also { requestInterceptor.enabled = true }
    }

    @AfterTest
    fun postVerify() {
        requests.forEach { req ->
            val uaString = requireNotNull(req.headers["User-Agent"]) {
                "Missing User-Agent header for request $req"
            }

            val components = uaString.split(" ")

            val metricsComponent = requireNotNull(components.find { it.startsWith("m/") }) {
                """User-Agent header "$uaString" doesn't contain business metrics for request $req"""
            }

            val metrics = metricsComponent.removePrefix("m/").split(",")

            assertContains(
                metrics,
                AwsBusinessMetric.DDB_MAPPER.identifier,
                """Mapper business metric not present in User-Agent header "$uaString" for request $req""",
            )
        }
    }

    @AfterAll
    fun cleanUp() {
        if (ddbHolder.isInitialized()) {
            runBlocking {
                tempTables.forEach { name ->
                    ddb.deleteTable { tableName = name }
                    ddb.waitUntilTableNotExists { tableName = name }
                }
            }

            ddb.close()
        }
    }
}

private class RequestCapturingInterceptor(val requests: MutableList<HttpRequest>) : HttpInterceptor {
    var enabled = true

    override fun readBeforeTransmit(context: ProtocolRequestInterceptorContext<Any, HttpRequest>) {
        if (enabled) {
            requests += context.protocolRequest
        }
    }
}
