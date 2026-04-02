/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance

interface ResourceDescriptor<T : Any> {
    suspend fun initialize(): T
    suspend fun finalize(instance: T)
}

private data class Resource<T : Any>(val descriptor: ResourceDescriptor<T>) {
    private lateinit var instance: T

    suspend fun initialize(): T {
        instance = descriptor.initialize()
        return instance
    }

    suspend fun finalize() {
        descriptor.finalize(instance)
    }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class ResourceTrackingTest {
    private val resources = mutableListOf<Resource<*>>()

    suspend fun <T : Any> createResource(descriptor: ResourceDescriptor<T>): T {
        val resource = Resource(descriptor)
        val instance = resource.initialize()
        resources += resource
        return instance
    }

    @AfterAll
    fun tearDown() = runBlocking {
        resources.reversed().forEach { it.finalize() }
    }
}
