/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters.testutils

import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.hll.mapping.core.converters.reversedBy

private val emojiToAnimalConverter = MonoConverter<String, String> {
    when (it) {
        "🐜" -> "ant"
        "🦇" -> "bat"
        "🐊" -> "crocodile"
        "🐕" -> "dog"
        "🦅" -> "eagle"
        "🐟" -> "fish"
        "🦒" -> "giraffe"
        "🐎" -> "horse"
        else -> error("""Unknown animal emoji "$it"""")
    }
}

private val animalToEmojiConverter = MonoConverter<String, String> {
    when (it) {
        "ant" -> "🐜"
        "bat" -> "🦇"
        "crocodile" -> "🐊"
        "dog" -> "🐕"
        "eagle" -> "🦅"
        "fish" -> "🐟"
        "giraffe" -> "🦒"
        "horse" -> "🐎"
        else -> error("""Unknown animal name "$it"""")
    }
}

val AnimalEmojiConverter = animalToEmojiConverter reversedBy emojiToAnimalConverter
