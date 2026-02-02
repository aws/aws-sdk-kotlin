/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.sdk

import org.gradle.kotlin.dsl.provideDelegate

/**
 * Settings related to bootstrapping codegen tasks for AWS service code generation.
 *
 * Services and protocols can be included or excluded by `+` or `-` prefix. If no prefix is found then it is
 * considered included (implicit `+`).
 *
 * @param services the service names to bootstrap. Services are named by either their model filename without
 * the extension or by their artifact/package name.
 * @param protocols the names of protocols to bootstrap
 */
class BootstrapConfig(
    services: String? = null,
    protocols: String? = null,
) {
    companion object {
        /**
         * A bootstrap configuration that includes everything by default
         */
        val ALL: BootstrapConfig = BootstrapConfig()
    }

    val serviceMembership: Membership by lazy { parseMembership(services) }
    val protocolMembership: Membership by lazy { parseMembership(protocols) }
    override fun toString(): String = "BootstrapConfig(serviceMembership=$serviceMembership, protocolMembership=$protocolMembership)"
}
