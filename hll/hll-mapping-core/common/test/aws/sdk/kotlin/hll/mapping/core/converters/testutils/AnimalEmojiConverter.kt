/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters.testutils

import aws.sdk.kotlin.hll.mapping.core.converters.ConverterImpl

val AnimalEmojiConverter = ConverterImpl<String, String>(
    {
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
    },
    {
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
    },
)
