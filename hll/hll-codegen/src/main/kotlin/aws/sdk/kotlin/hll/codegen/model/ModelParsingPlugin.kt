/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.model

import aws.sdk.kotlin.runtime.InternalSdkApi
import java.util.ServiceLoader

/**
 * Represents a plugin to the model parsing phase of code generation. Plugins have the opportunity to customize how
 * models are parsed or customized. Implementations of this interface are loaded by
 * [SPI](https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html).
 */
@InternalSdkApi
public interface ModelParsingPlugin {
    @InternalSdkApi
    public companion object {
        /**
         * Gets the list of plugins available on the classpath. Note that this list is loaded only once and doesn't
         * refresh.
         */
        public val instances: List<ModelParsingPlugin> by lazy {
            val classLoader = ModelParsingPlugin::class.java.classLoader
            ServiceLoader.load(ModelParsingPlugin::class.java, classLoader).toList()
        }

        /**
         * Executes a series of transformations from all loaded plugin instances by accepting an input [shape] and
         * folding over the [processor] reference from each plugin. Plugin processors are executed in the order they
         * were loaded, as visible in [instances].
         * @param S The type of shape this operation will transform
         * @param shape The initial input shape
         * @param processor A function which accepts a [ModelParsingPlugin] and an [S] shape which will be run for each
         * plugin
         */
        @InternalSdkApi
        public fun <S> transform(shape: S, processor: (ModelParsingPlugin, S) -> S): S = instances.fold(shape) { prev, plugin -> processor(plugin, prev) }
    }

    /**
     * Perform some processing of an [Operation] after it's been parsed by the base codegen layer. Implementors should
     * return a modified version of [operation] if changes are required or the exact same [operation] if no changes are
     * necessary.
     * @param operation The [Operation] to potentially modify
     * @return A modified version of [operation] if changes are required or the exact same [operation] if no changes are
     * necessary.
     */
    public fun postProcessOperation(operation: Operation): Operation = operation

    /**
     * Perform some processing of a [Structure] after it's been parsed by the base codegen layer. Implementors should
     * return a modified version of [struct] if changes are required or the exact same [struct] if no changes are
     * necessary.
     * @param struct The [Structure] to potentially modify
     * @return A modified version of [struct] if changes are required or the exact same [struct] if no changes are
     * necessary.
     */
    public fun postProcessStructure(struct: Structure): Structure = struct

    /**
     * Perform some processing of a [Member] after it's been parsed by the base codegen layer. Implementors should
     * return a modified version of [member] if changes are required or the exact same [member] if no changes are
     * necessary.
     * @param member The [Member] to potentially modify
     * @return A modified version of [member] if changes are required or the exact same [member] if no changes are
     * necessary.
     */
    public fun postProcessMember(member: Member): Member = member
}
