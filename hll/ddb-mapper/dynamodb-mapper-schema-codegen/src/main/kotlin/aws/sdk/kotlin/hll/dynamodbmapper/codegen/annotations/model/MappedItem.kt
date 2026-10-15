/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model

import aws.sdk.kotlin.hll.codegen.model.TypeRef

/**
 * Everything required to generate code for a single annotated class. Instances are fully resolved: renderers make no
 * further decisions and never consult the symbol-processing framework.
 * @param name The simple name of the annotated class, used to derive generated declaration names (e.g., `UserSchema`)
 * @param type The annotated class itself
 * @param kind Whether this is a table item or a nested-only item
 * @param itemConverter The item converter used by this class's schema and value converter
 * @param builder How to render this class's builder, or `null` if no builder should be generated. This is the sole
 * source of truth for the builder decision.
 * @param valueConverter The item-level value converter to generate, or `null` if none is needed (no other mapped type
 * references this one). References from other items are derived from the same source, so they always match.
 * @param metadata Table-level metadata (keys, TTL, counters). Empty for [Kind.Nested] items.
 * @param generateGetTable Whether to generate a `getTable` extension. Only ever `true` for [Kind.Table] items.
 * @param destinationPkg The package into which code for this class is generated
 * @param isObject Whether the annotated class is an `object`, whose single instance is never constructed or mutated
 * @param fileName The name of the generated file. Defaults to `<name>Schema`.
 */
internal data class MappedItem(
    val name: String,
    val type: TypeRef,
    val kind: Kind,
    val itemConverter: ItemConverterSpec,
    val builder: BuilderSpec?,
    val valueConverter: TypeRef?,
    val metadata: SchemaMetadata,
    val generateGetTable: Boolean,
    val destinationPkg: String,
    val isObject: Boolean = false,
    val fileName: String = "${name}Schema",
) {
    /**
     * Identifies the role of a mapped class
     */
    sealed interface Kind {
        /**
         * A table item (annotated with `@DynamoDbItem`), which has keys and generates a schema
         */
        data object Table : Kind

        /**
         * A nested-only item (annotated with `@DynamoDbMappable` but not `@DynamoDbItem`), which generates only
         * converters
         */
        data object Nested : Kind
    }
}
