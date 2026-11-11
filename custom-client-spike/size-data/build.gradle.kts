// Scratch: resolve the JVM runtime closure of the published S3 client, with and without the modules a trimmed
// 3-operation client no longer needs, and print each file's size.
plugins { base }

repositories {
    google()
    mavenCentral()
}

val sdkVersion = "1.9.32"

val full by configurations.creating
val trimmed by configurations.creating
val r8 by configurations.creating
val ddb by configurations.creating

listOf(full, trimmed, ddb).forEach { cfg ->
    cfg.attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named("standard-jvm"))
        attribute(Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java), "jvm")
    }
}

dependencies {
    full("aws.sdk.kotlin:s3:$sdkVersion")
    // The trimmed client's own jar is measured from the local build; here we only need its dependencies, which
    // are the full client's minus aws-event-stream (diff of the generated build files).
    trimmed("aws.sdk.kotlin:s3:$sdkVersion") {
        exclude(group = "aws.smithy.kotlin", module = "aws-event-stream")
    }
    r8("com.android.tools:r8:8.13.19")
    ddb("aws.sdk.kotlin:dynamodb:$sdkVersion")
}

fun report(cfg: Configuration, out: File) {
    val lines = cfg.incoming.artifacts.artifacts.map { a ->
        "${a.file.length()}\t${a.id.componentIdentifier.displayName}\t${a.file.name}"
    }.sorted()
    out.writeText(lines.joinToString("\n", postfix = "\n"))
}

tasks.register("sizes") {
    doLast {
        report(full, file("full.tsv"))
        report(trimmed, file("trimmed.tsv"))
        report(ddb, file("ddb.tsv"))
        file("r8.classpath").writeText(r8.resolve().joinToString(":"))
    }
}
