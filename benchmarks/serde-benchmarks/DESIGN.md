# Benchmark 2: Serialization & Deserialization

## Goal

Measure serialization and deserialization performance across all protocols the Kotlin SDK supports (aws-rest-json, aws-json-rpc-1-0, smithy-rpc-v2-cbor, aws-rest-xml, aws-query) using standardized test cases from AwsSdkPerformanceBenchmarkModels. Serde runs on every request and is one of the largest CPU costs visible in profiling.

## What needs to be done

- Consume AwsSdkPerformanceBenchmarkModels to generate mock service clients for each protocol.
- For each test case tagged `serde-benchmark`: create a client with mock HTTP engine, invoke the operation, capture serialization and deserialization time at the spec-defined boundaries.
- Timing points per spec:
  - Serialization start: immediately before client invocation (e.g., right before `client.operationName(input)`).
  - Serialization end: immediately after the request object is no longer mutated — right before the HTTP request is sent to the transport implementation.
  - Deserialization start: immediately before the HTTP response is handed off to SDK deserialization code (the response body MUST NOT be buffered prior to this point).
  - Deserialization end: immediately after the final output object is returned to the caller.

## SDK Request Pipeline (relevant hooks)

When a user calls `client.putObject(input)`:

```
readBeforeExecution                    ← SER START (spec)
  serializer.serialize(context, input)
  middleware (UserAgent, retry setup, request deep copy)
  endpoint resolution + signing + checksum
readBeforeTransmit                     ← SER END (spec)
══════ TRANSPORT BOUNDARY ══════
engine.roundTrip(request)              ← network I/O (mocked)
readBeforeDeserialization               ← DESER START (spec)
  response.body.readAll()
  deserializer.deserialize(context, call, payload)
readAfterDeserialization                ← DESER END (spec)
  completion interceptors
return output to caller
```

## Problem

The benchmark must go through a real SDK client (per spec: "serde benchmarks MUST go through a client"), but we need to capture timing at precise boundaries inside the pipeline — not the entire method wall-clock.

For serialization, the measurement must span from `readBeforeExecution` to `readBeforeTransmit` (everything from client entry to the transport boundary). For deserialization, the measurement must span from `readBeforeDeserialization` to `readAfterDeserialization` only (response handoff to output return).

JMH measures the entire `@Benchmark` method from entry to exit, which means:
- For serialization: it includes `runBlocking` overhead, engine coroutine setup, and exception unwind — none of which a real user pays for serde.
- For deserialization: it includes the entire serialization pipeline (from `readBeforeExecution` through the transport boundary) before deserialization even begins, inflating the measurement by 2,000-5,000ns of signing and middleware work that has nothing to do with deserialization.

## Implementation

### Option A: JMH with HaltException (previous)

Uses JMH via kotlinx-benchmark. For serialization, a `TestEngine` throws `HaltException` at the transport boundary to halt execution. For deserialization, the `TestEngine` returns a canned response and JMH measures the full round-trip.

```kotlin
// Serialization
@Benchmark
fun restJson1_PutObject_S() = runBlocking {
    val input = PutObjectRequest { bucket = "test-bucket"; key = "test-key"; ... }
    try {
        client.putObject(input)
    } catch (_: HaltException) {}
}

// Deserialization
@Benchmark
fun restJson1_GetObject_S() = runBlocking {
    val input = GetObjectRequest { bucket = ""; key = "" }
    client.getObject(input)
}
```

### Option B: Custom harness with interceptor timestamps (current)

Uses a custom timing loop (no JMH) with a `BenchmarkInterceptor` that captures `System.nanoTime()` at the exact spec-defined boundaries inside the pipeline. Inputs are pre-constructed outside the timed loop.

```kotlin
class BenchmarkInterceptor : HttpInterceptor {
    var serializationStartNanos: Long = 0L
    var serializationEndNanos: Long = 0L
    var deserializationStartNanos: Long = 0L
    var deserializationEndNanos: Long = 0L

    fun serializationNanos(): Long = serializationEndNanos - serializationStartNanos
    fun deserializationNanos(): Long = deserializationEndNanos - deserializationStartNanos

    override fun readBeforeExecution(context: ...) {
        serializationStartNanos = System.nanoTime()
    }
    override fun readBeforeTransmit(context: ...) {
        serializationEndNanos = System.nanoTime()
    }
    override fun readBeforeDeserialization(context: ...) {
        deserializationStartNanos = System.nanoTime()
    }
    override fun readAfterDeserialization(context: ...) {
        deserializationEndNanos = System.nanoTime()
    }
}

// Setup: interceptor is registered on the client so it fires on every operation call
val interceptor = BenchmarkInterceptor()
val client = RestJsonDataPlaneClient {
    httpClient = TestEngine { _, request -> /* return canned 200 response */ }
    interceptors.add(interceptor)
    // ...
}

// Input is pre-constructed outside the timing loop
val input = PutObjectRequest { bucket = "test-bucket"; key = "test-key"; ... }

// For serialization: extractNanos reads the serialization delta from the interceptor
BenchmarkHarness.run(
    id = "restJson1_PutObject_S",
    interceptor = interceptor,
    extractNanos = BenchmarkInterceptor::serializationNanos,
) { client.putObject(input) }

// For deserialization: extractNanos reads the deserialization delta instead
BenchmarkHarness.run(
    id = "restJson1_GetObject_S",
    interceptor = interceptor,
    extractNanos = BenchmarkInterceptor::deserializationNanos,
) { client.getObject(input) }
```

The harness calls `operation()` on each iteration, then calls `extractNanos(interceptor)` to read the relevant duration captured by the interceptor hooks during that iteration.

## Comparison

| Consideration | Option A (JMH + HaltException) | Option B (Interceptor timestamps) |
|---|---|---|
| Serialization start matches spec | ⚠️ includes `runBlocking` overhead (~40ns) before pipeline entry | ✅ `readBeforeExecution` = first SDK hook (exact) |
| Serialization end matches spec | ⚠️ exception thrown inside engine (after coroutine setup, past transport boundary) | ✅ `readBeforeTransmit` = right before transport (exact) |
| Deserialization start matches spec | ❌ measures from method entry (includes entire serialization pipeline through transport boundary) | ✅ `readBeforeDeserialization` = right before deser code (exact) |
| Deserialization end matches spec | ⚠️ includes completion interceptors after deserialization | ✅ `readAfterDeserialization` = right after output produced (exact) |
| Input constructed outside timing | ❌ built per iteration inside `@Benchmark` method | ✅ pre-constructed as class field |
| Exception overhead in measurement | ❌ HaltException throw + retry eval + completion interceptors (~5-10ns) | ✅ no exception in pipeline |
| Goes through a real client | ✅ | ✅ |
| Iteration control matches spec | ❌ JMH controls (fixed warmup/measurement seconds) | ✅ min 1K, max 10K, 30s cap, 50% warmup discard |
| Output format matches spec | ❌ JMH JSON schema | ✅ spec-compliant JSON (id, n, mean, p50, p90, p95, p99, std_dev) |
| Dead code elimination prevention | ✅ JMH Blackhole | ✅ `System.nanoTime()` in interceptor + virtual dispatch through engine interface are opaque to JIT |
| Fork isolation (JIT variance) | ✅ JMH forks new JVMs | ⚠️ single process — can be mitigated by running the harness multiple times as separate JVM invocations |

## Decision

Option B is chosen: The spec requires precise timing at boundaries that exist inside the SDK pipeline, not at the method boundary. JMH can only measure the entire method, which is fundamentally incompatible with the spec's deserialization requirement ("start immediately before the HTTP response is handed to deserialization code"). With JMH, the deserialization measurement includes 2,000-5,000ns of serialization + signing overhead that has nothing to do with deserialization — making cross-SDK comparisons invalid.

Option B captures timestamps at the exact hooks the SDK already provides (`readBeforeExecution`, `readBeforeTransmit`, `readBeforeDeserialization`, `readAfterDeserialization`), matching the spec's timing points precisely. The custom timing loop also matches the spec's prescribed iteration methodology (min 1,000 iterations, 30-second cap, 50% warmup discard) and produces the required JSON output format directly.

The loss of JMH's fork isolation is acceptable because serde benchmarks measure microsecond-scale operations where JIT compilation effects are stable after warmup, and the harness can be run multiple times for validation.

## Note

- Interceptor: `src/main/kotlin/aws/sdk/kotlin/benchmarks/serde/BenchmarkInterceptor.kt`
- Harness: `src/main/kotlin/aws/sdk/kotlin/benchmarks/serde/BenchmarkHarness.kt`
- Runner: `src/main/kotlin/aws/sdk/kotlin/benchmarks/serde/BenchmarkRunner.kt`
- Codegen: `smithy-kotlin/.../rendering/protocol/HttpProtocolSerdeBenchmarkGenerator.kt`
- Configuration: min 1,000 iterations, max 10,000 iterations, 30s time cap, 50% warmup discard. Configurable via `-Pbenchmark.minIterations`, `-Pbenchmark.maxIterations`, `-Pbenchmark.maxDurationSeconds`, `-Pbenchmark.warmupFraction`.
- Run: `./gradlew :benchmarks:serde-benchmarks:runAllBenchmarks -PbenchmarkModelsDir=/path/to/model`
- Fork isolation: The current harness runs in a single JVM process. If results are not stable across JVM restarts (indicating JIT compilation variance), the mitigation is to add fork support — spawn N child JVM processes from a parent runner, collect their JSON outputs, and aggregate the statistics. This is the same mechanism JMH uses internally. Not implemented in the initial version; can be added via a `-Pbenchmark.forks=N` parameter if cross-run variance is observed in practice.
