# DynamoDB Mapper Operations Code-gen module

This module generates the code for high-level operations (e.g., `GetItem`, `Query`, etc.) in the DynamoDB Mapper. These
operations are code-generated because of the subtlety involved in transforming high-level requests/responses to/from
low-level requests/responses.

The following concepts are helpful to understand when reading/modifying the code base:

## Key projections

Many low-level DynamoDB operations have request/response types which contain fields representing item keys, such as
`GetItemRequest`'s `key` field and `QueryResponse`'s `lastEvaluatedKey` field. The low-level types for these fields are
`Map<String, AttributeValue>` which provides an untyped view of the data. Since DynamoDB Mapper exists to provide
tighter integration with domain types in user code, these low-level fields must be transformed into types which better
model the key structures in higher-level types. Specifically, key fields in high-level types should be a subtype of
`KeyType` and should be broken into separate fields for partition keys and sort keys.

In this context, a key projection is a technique for generating multiple specialized type variants from a single
low-level DynamoDB type, based on how the type handles item keys (partition keys and sort keys).

### Projection types

This module creates up to three projections for each low-level DynamoDB type, denoted by the 
[`KeyProjectionType`](src/main/kotlin/aws/sdk/kotlin/hll/dynamodbmapper/codegen/operations/model/KeyProjectionType.kt)
enumeration:

1. `NONE` (unkeyed): The base type with no key-specific fields
2. `PARTITION_KEY`: A variant that includes partition key fields
3. `COMPOSITE_KEY`: A variant that includes both partition key and sort key fields

### How they're used

DynamoDB operations can work with keys in different ways:

- **Unkeyed structures**: Types like `GetItemResponse` don't contain key fields (they just return the item data)
- **Keyed structures**: Types like `DeleteItemRequest` contain a key field used to identify which item to operate on

For keyed structures, the code generator creates specialized nested interfaces that expose type-safe key access:

```kotlin
sealed interface DeleteItemRequest {
    // Common fields shared by all projections
    val returnConsumedCapacity: ReturnConsumedCapacity?

    interface PartitionKey<PK : KeyType> : DeleteItemRequest {
        val partitionKey: PK?
    }

    interface CompositeKey<PK : KeyType, SK : KeyType> : DeleteItemRequest {
        val partitionKey: PK?
        val sortKey: SK?
    }
}
```

### How it works

The projection process first identifies low-level members with `MemberCodegenBehavior.MapToKeys` (i.e., fields that
represent item keys). If any exist (i.e., this is a _keyed_ structure), three key projections are derived with different
type hierarchies. If none exist (i.e., this is an _untyped_ structure), only one key projection is derived—the `NONE`
(or unkeyed) projection.

In the case of keyed projections, low-level fields are renamed according to the projection type. For instance, a
low-level `key` field would become `partitionKey` for a `PARTITION_KEY` projection. For a `COMPOSITE_KEY` projection,
the `key` field would become `partitionKey` and `sortKey` (two separate fields). The types of these new fields are
`PK : KeyType` and `SK : KeyType`.

Finally, each projection gets a derived set of related types: an implementation class, a builder class, and optionally a
parent class (only applies to keyed projections). This abstraction provides the necessary building blocks to generate a
high-level API that can correctly comprehend key structure in a type-safe way.
