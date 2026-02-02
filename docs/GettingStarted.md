# Getting Started

Releases of the AWS SDK for Kotlin are published to Maven Central . 
**NOTE: Beta releases ending with the `-beta` qualifier are not meant for production workloads**.
Consult the [stability guide](../VERSIONING.md#stability-of-the-aws-sdk-for-kotlin) for more information on SDK stability and maintenance.

1. Add the repository to your Gradle or Maven configuration

    **Gradle Users**

    ```kt
    # file: my-project/build.gradle.kts

    repositories {
        mavenCentral()
    }
    ```


2. Add services to your project

    ```kt
    # file: my-project/build.gradle.kts

    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.3")
        
        // The following line adds a dependency on the dynamodb client.
        implementation("aws.sdk.kotlin:dynamodb:1.+")
    }
    ```

3. Configure a service client
   
    Example for `DynamoDB`:

    ```kotlin
    import kotlinx.coroutines.runBlocking
    import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
    import aws.sdk.kotlin.services.dynamodb.listTables

    fun main() = runBlocking {
        val client = DynamoDbClient.fromEnvironment()
        val resp = client.listTables { limit = 10 }

        println("Current DynamoDB tables: ")
        resp.tableNames?.forEach { println(it) }

        client.close()
    }
    ```



## Additional Resources

* [Additional examples](https://github.com/aws/aws-sdk-kotlin/tree/main/examples)
* [Getting started](https://github.com/aws/aws-sdk-kotlin#getting-started)
* [Debugging](debugging.md)
* [Android support](targets.md#android)
