package aws.sdk.kotlin.hll.mapping.core.converters.internal

import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter

internal class ConverterImpl<L, R>(
    override val right: MonoConverter<L, R>,
    override val left: MonoConverter<R, L>,
) : Converter<L, R>
