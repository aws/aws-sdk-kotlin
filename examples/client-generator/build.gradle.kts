/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    kotlin("jvm")
    id("software.amazon.smithy").version("0.5.3")
}

val awsSdkKotlinVersion: String by project

dependencies {
    implementation("software.amazon.smithy.kotlin:aws-codegen:$awsSdkKotlinVersion")
    // NOTE: More smithy dependencies may be required depending on what's referenced by your API models.
}
