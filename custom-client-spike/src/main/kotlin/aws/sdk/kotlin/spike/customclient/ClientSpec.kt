/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.spike.customclient

import org.gradle.api.GradleException
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

public abstract class ClientSpec {
    public abstract val visibility: Property<String>
    public abstract val generatePaginators: Property<Boolean>

    // Public only because Gradle's managed-property generation doesn't see Kotlin's name-mangled internal getters.
    // Values are comma-joined: Gradle can't decorate MapProperty<String, List<String>> (Kotlin emits a wildcard).
    public abstract val services: MapProperty<String, String>
    public abstract val index: Property<IndexService>

    internal var activate: (() -> Unit)? = null

    public fun service(name: String, operations: List<String>) {
        val idx = index.get()
        val service = idx.resolveService(name)
        val resolved = operations.map { idx.resolveOperation(service, it) }
        val existing = services.getting(service).orNull?.split(",").orEmpty()
        services.put(service, (existing + resolved).distinct().joinToString(","))
    }
}

/**
 * Stand-in for the baked `index.json`: parsed once per build and shared through a build service.
 */
public abstract class IndexService : BuildService<BuildServiceParameters.None> {
    private val operations: Map<String, List<String>> by lazy {
        val text = checkNotNull(IndexService::class.java.getResource("index.txt")) { "index.txt missing" }.readText()
        text.lines().filter { it.isNotBlank() }.associate { line ->
            val (svc, ops) = line.split("=", limit = 2)
            svc.trim() to ops.split(",").map { it.trim() }
        }
    }

    internal fun resolveService(name: String): String = operations.keys.firstOrNull { it.equals(name, ignoreCase = true) }
        ?: throw GradleException("Unknown service '$name'. Available services: ${operations.keys.joinToString()}")

    internal fun resolveOperation(service: String, name: String): String {
        val ops = operations.getValue(service)
        return ops.firstOrNull { it.equals(name, ignoreCase = true) }
            ?: throw GradleException("Unknown operation '$name' for service $service. Available operations: ${ops.joinToString()}")
    }
}
