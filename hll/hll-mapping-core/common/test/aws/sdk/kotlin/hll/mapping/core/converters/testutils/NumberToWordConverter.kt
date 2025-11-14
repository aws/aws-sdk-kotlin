package aws.sdk.kotlin.hll.mapping.core.converters.testutils

import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.hll.mapping.core.converters.reversedBy

private val numberToWordConverter = MonoConverter<Int, String> {
    when (it) {
        0 -> "zero"
        1 -> "one"
        2 -> "two"
        3 -> "three"
        4 -> "four"
        5 -> "five"
        6 -> "six"
        7 -> "seven"
        8 -> "eight"
        9 -> "nine"
        10 -> "ten"
        else -> error("Unsupported number value $it")
    }
}

private val wordToNumberConverter = MonoConverter<String, Int> {
    when (it) {
        "zero" -> 0
        "one" -> 1
        "two" -> 2
        "three" -> 3
        "four" -> 4
        "five" -> 5
        "six" -> 6
        "seven" -> 7
        "eight" -> 8
        "nine" -> 9
        "ten" -> 10
        else -> error("""Unknown number string "$it"""")
    }
}

val NumberToWordConverter = numberToWordConverter reversedBy wordToNumberConverter
