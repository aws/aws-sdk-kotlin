$version: "2"

namespace aws.sdk.kotlin.test

use aws.protocols#restJson1
use aws.api#service
use aws.auth#sigv4

@restJson1
@sigv4(name: "event-stream-test")
@service(sdkId: "EventStreamTest")
service TestService { version: "123", operations: [TestStreamOp] }

@http(method: "POST", uri: "/test-eventstream", code: 200)
operation TestStreamOp {
    input: TestStreamInputOutput,
    output: TestStreamInputOutput,
    errors: [SomeError],
}

structure TestStreamInputOutput {
    @httpPayload
    @required
    value: TestStream
}

@error("client")
structure SomeError {
    Message: String,
}

union TestUnion {
    Foo: String,
    Bar: Integer,
}

structure TestStruct {
    someString: String,
    someInt: Integer,
}

structure MessageWithBlob { @eventPayload data: Blob }

structure MessageWithString { @eventPayload data: String }

structure MessageWithStruct { @eventPayload someStruct: TestStruct }

structure MessageWithUnion { @eventPayload someUnion: TestUnion }

structure MessageWithHeaders {
    @eventHeader blob: Blob,
    @eventHeader boolean: Boolean,
    @eventHeader byte: Byte,
    @eventHeader int: Integer,
    @eventHeader long: Long,
    @eventHeader short: Short,
    @eventHeader string: String,
    @eventHeader timestamp: Timestamp,
    @eventHeader enum: Enum,
    @eventHeader intEnum: IntEnum,
}
structure MessageWithHeaderAndPayload {
    @eventHeader header: String,
    @eventPayload payload: Blob,
}
structure MessageWithNoHeaderPayloadTraits {
    someInt: Integer,
    someString: String,
}

structure MessageWithUnboundPayloadTraits {
    @eventHeader header: String,
    unboundString: String,
}

@streaming
union TestStream {
    MessageWithBlob: MessageWithBlob,
    MessageWithString: MessageWithString,
    MessageWithStruct: MessageWithStruct,
    MessageWithUnion: MessageWithUnion,
    MessageWithHeaders: MessageWithHeaders,
    MessageWithHeaderAndPayload: MessageWithHeaderAndPayload,
    MessageWithNoHeaderPayloadTraits: MessageWithNoHeaderPayloadTraits,
    MessageWithUnboundPayloadTraits: MessageWithUnboundPayloadTraits,
    SomeError: SomeError,
}

enum Enum {
    DIAMOND
    CLUB
    HEART
    SPADE
}

intEnum IntEnum {
    JACK = 1
    QUEEN = 2
    KING = 3
    ACE = 4
    JOKER = 5
}
