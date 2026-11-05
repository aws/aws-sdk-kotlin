# Supported Targets

## JVM

The AWS SDK for Kotlin supports running on JDK8+ on the following platforms and architectures:

* Windows: `x86_32`, `x86_64`
* Linux: `x86_32`, `x86_64`, `armv6`, `armv7`, `armv8`
* macOS: `x86_64`

If there is a platform or architecture not supported please submit an issue with details about your use case.

## Android

The AWS SDK for Kotlin supports Android API 24+ (`minSdk = 24`).

NOTE: Later versions of Android may contain security fixes so consider reviewing known vulnerabilities
for the Android versions you choose to support in your application.

Additional requirements:

* Enable [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring)

Example config fragments:

```kotlin
// build.gradle.kts

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
    compilerOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")
}
```

## GraalVM

The AWS SDK for Kotlin supports [native image compilation using GraalVM](https://www.graalvm.org/latest/reference-manual/native-image/).

Compiling native images is easily accomplished by applying [the GraalVM build plugin](https://github.com/graalvm/native-build-tools) to your application and running `./gradlew nativeCompile`.
View [their getting started guide](https://graalvm.github.io/native-build-tools/latest/gradle-plugin-quickstart.html) for further details.

NOTE: [log4j2 does not currently support](https://github.com/apache/logging-log4j2/issues/1539) GraalVM native image compilation, so we recommend selecting a different logger implementation such as Logback.

## Kotlin/Native

The AWS SDK for Kotlin is a Kotlin Multiplatform project. In addition to Kotlin/JVM, it supports the following
[Kotlin/Native targets](https://kotlinlang.org/docs/native-target-support.html):

| Target              | Built on |
|---------------------|----------|
| `linuxX64`          | Linux    |
| `linuxArm64`        | Linux    |
| `macosX64`          | macOS    |
| `macosArm64`        | macOS    |
| `iosArm64`          | macOS    |
| `iosX64`            | macOS    |
| `iosSimulatorArm64` | macOS    |
| `mingwX64`          | Windows  |

Hosts that run the Kotlin/Native tests: [INCOMPLETE: the CI host matrix for Kotlin/Native test runs is not yet defined]

Minimum supported Xcode and Apple OS versions: [INCOMPLETE: not yet defined by the SDK]

To use the SDK from Kotlin/Native, apply the [Kotlin Multiplatform Gradle plugin](https://kotlinlang.org/docs/multiplatform/multiplatform-dsl-reference.html)
and add SDK dependencies to the `commonMain` source set. See [Getting Started](GettingStarted.md) for an example.

When building the SDK from source, only the Kotlin/Native targets the current host can build are enabled. Set the Gradle
property `aws.kotlin.native.enableAllTargets=true` to enable all of them, or `aws.kotlin.native=false` to disable
Kotlin/Native entirely.

### HTTP engine

The default HTTP engine on Kotlin/Native is the CRT engine (`CrtHttpEngine`). On Kotlin/JVM the default is the OkHttp
engine (`OkHttpEngine`).

### Limitations

The following features are not available on Kotlin/Native:

* File-based `ByteStream` helpers (`ByteStream.fromFile`, `File.asByteStream`, `Path.asByteStream` and
  `ByteStream.writeToFile`) are Kotlin/JVM-only.
* The `credential_process` profile setting is not supported on iOS.
* JVM system properties (e.g. `aws.region`) are not read as a configuration source. Use environment variables or the
  shared config and credentials files instead.
* DynamoDB Mapper value converters for `java.math.BigDecimal` and `java.math.BigInteger` are Kotlin/JVM-only. Use the
  multiplatform `aws.smithy.kotlin.runtime.content.BigDecimal` and `aws.smithy.kotlin.runtime.content.BigInteger` types
  in common code.
