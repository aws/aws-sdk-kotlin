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

structure MessageWithEnumHeaders {
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

@streaming
union TestStream {
    MessageWithEnumHeaders: MessageWithEnumHeaders,
    MessageWithHeaderAndPayload: MessageWithHeaderAndPayload,
    MessageWithNoHeaderPayloadTraits: MessageWithNoHeaderPayloadTraits,
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
