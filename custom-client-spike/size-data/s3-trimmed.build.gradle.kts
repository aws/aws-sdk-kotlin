
description = "The AWS SDK for Kotlin client for S3"
project.ext.set("aws.sdk.id", "S3")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("aws.smithy.kotlin:aws-credentials:1.7.13")
                implementation(project(":aws-runtime:aws-http"))
                implementation("aws.smithy.kotlin:aws-protocol-core:1.7.13")
                implementation("aws.smithy.kotlin:aws-signing-common:1.7.13")
                implementation("aws.smithy.kotlin:aws-signing-default:1.7.13")
                implementation("aws.smithy.kotlin:aws-xml-protocols:1.7.13")
                implementation("aws.smithy.kotlin:http:1.7.13")
                implementation("aws.smithy.kotlin:http-auth:1.7.13")
                implementation("aws.smithy.kotlin:http-auth-aws:1.7.13")
                implementation("aws.smithy.kotlin:http-client-engine-default:1.7.13")
                implementation("aws.smithy.kotlin:identity-api:1.7.13")
                implementation("aws.smithy.kotlin:serde:1.7.13")
                implementation("aws.smithy.kotlin:serde-xml:1.7.13")
                implementation("aws.smithy.kotlin:telemetry-defaults:1.7.13")
                api(project(":aws-runtime:aws-config"))
                api(project(":aws-runtime:aws-core"))
                api(project(":aws-runtime:aws-endpoint"))
                api("aws.smithy.kotlin:http-client:1.7.13")
                api("aws.smithy.kotlin:runtime-core:1.7.13")
                api("aws.smithy.kotlin:smithy-client:1.7.13")
                api("aws.smithy.kotlin:telemetry-api:1.7.13")
            }
        }
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test:2.4.0")
                implementation("aws.smithy.kotlin:smithy-test:1.7.13")
            }
        }
    }
}
