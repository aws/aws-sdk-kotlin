# Configuring AWS Lambda SnapStart

The AWS SDK for Kotlin automatically prepares its default JVM resources for AWS Lambda SnapStart. No SDK-specific
configuration is required. When Lambda sets `AWS_LAMBDA_INITIALIZATION_TYPE=snap-start`, SDK clients created during the
function initialization phase register checkpoint and restore hooks through the CRaC API.

Create reusable SDK clients outside the invocation handler so that client construction is included in the snapshot:

```kotlin
class Handler : RequestHandler<Unit, String> {
    private val s3 = S3Client { region = "us-east-1" }

    override fun handleRequest(input: Unit, context: Context): String = runBlocking {
        s3.listBuckets().buckets.orEmpty().joinToString { it.name.orEmpty() }
    }
}
```

Before Lambda creates the snapshot, the SDK closes its default credential provider chain, resets the default bearer-token
provider's loaded configuration, and shuts down SDK-managed OkHttp engines. After Lambda restores the function, the SDK
recreates those resources. This discards credentials cached before the snapshot and avoids reusing HTTP connection-pool
state captured at checkpoint.

Before Lambda creates the snapshot, the SDK stops accepting new requests and waits up to ten seconds for active requests
to finish before closing its default HTTP engine. Complete initialization-time SDK calls before the checkpoint whenever
possible. If requests do not become idle within the timeout, checkpoint creation fails and the SDK recreates the engine
so the initialization environment remains usable.

The default OkHttp engine and the SDK-managed OkHttp4 engine are restored automatically. Other SDK-managed engines,
including CRT, are not restored automatically. Resources explicitly supplied in client configuration remain caller-owned.
If you configure another HTTP engine, credentials provider, or bearer-token provider, register any required CRaC hooks
for that resource yourself.

SnapStart still establishes network connections and resolves credentials as needed after restore. Consult the
[AWS Lambda SnapStart documentation](https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html) for supported runtimes,
deployment configuration, and restore-time constraints.
