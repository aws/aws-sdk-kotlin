package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.ConfigurationException
import kotlin.test.*

/**
 * Forms the [combinations](https://en.wikipedia.org/wiki/Combination) of a given length for the given set
 */
private fun combinations(ofSet: Set<Char>, length: Int): Set<String> {
    if (length <= 0) return emptySet()
    if (length == 1) return ofSet.map { it.toString() }.toSet()

    val elements = ofSet.toList()

    return buildSet {
        fun generate(current: String, startIndex: Int) {
            if (current.length == length) {
                add(current)
            } else {
                for (i in startIndex until elements.size) {
                    generate(current + elements[i], i + 1)
                }
            }
        }

        generate("", 0)
    }
}

private object TestData {
    private val validChars = charSet("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-.+~%!$&'()*+,;=")

    /**
     * Non-exhaustive set of [actual AWS regions][1].
     *
     * [1]: https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Concepts.RegionsAndAvailabilityZones.html
     */
    private val realRegions = setOf(
        "af-south-1",
        "ap-east-1",
        "ap-east-2",
        "ap-northeast-1",
        "ap-northeast-2",
        "ap-northeast-3",
        "ap-south-1",
        "ap-south-2",
        "ap-southeast-1",
        "ap-southeast-2",
        "ap-southeast-3",
        "ap-southeast-4",
        "ap-southeast-5",
        "ap-southeast-6",
        "ap-southeast-7",
        "ca-central-1",
        "ca-west-1",
        "eu-central-1",
        "eu-central-2",
        "eu-north-1",
        "eu-south-1",
        "eu-south-2",
        "eu-west-1",
        "eu-west-2",
        "eu-west-3",
        "il-central-1",
        "me-central-1",
        "me-south-1",
        "mx-central-1",
        "sa-east-1",
        "us-east-1",
        "us-east-2",
        "us-west-1",
        "us-west-2",
    )

    private val kitchenSinkRegion = validChars.joinToString("") // ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-.+~%!$&'()*+,;=
    private val regionsWithSpecialChars = combinations(validChars, 3).map { "region-$it" }.toSet() // region-XXX
    val validRegions = realRegions + regionsWithSpecialChars + kitchenSinkRegion

    private val printableAsciiChars = charSet(32.toChar()..126.toChar()) // ASCII codepoints 32-126 (inclusive)
    private val invalidChars = printableAsciiChars - validChars
    val invalidRegions = combinations(invalidChars, 3).map { "region-$it" }.toSet() // region-XXX
}

class ValidateRegionTest {
    @Test
    fun testIsRegionValid() {
        TestData.validRegions.forEach {
            println("Valid region: $it")
            assertTrue(isRegionValid(it))
        }
        TestData.invalidRegions.forEach {
            println("Invalid region: $it")
            assertFalse(isRegionValid(it))
        }
    }

    @Test
    fun testValidateRegion() {
        TestData.validRegions.forEach {
            assertEquals(it, validateRegion(it))
        }

        TestData.invalidRegions.forEach {
            assertFailsWith<ConfigurationException> {
                validateRegion(it)
            }
        }
    }
}