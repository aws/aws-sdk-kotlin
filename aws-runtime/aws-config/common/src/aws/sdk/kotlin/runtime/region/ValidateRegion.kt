package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi

internal fun charSet(chars: String) = chars.toCharArray().toSet()
internal fun charSet(range: CharRange) = range.toSet()

private object Rfc3986CharSets {
    val alpha = charSet('A'..'Z') + charSet('a'..'z')
    val digit = charSet('0'..'9')
    val unreserved = alpha + digit + charSet("-.+~")
    val hexdig = digit + charSet('A'..'F')
    val pctEncoded = hexdig + '%'
    val subDelims = charSet("!$&'()*+,;=")
    val regName = unreserved + pctEncoded + subDelims
}

@InternalSdkApi
public fun isRegionValid(region: String): Boolean = region.isNotEmpty() && region.all(Rfc3986CharSets.regName::contains)

@InternalSdkApi
public fun validateRegion(region: String): String = region.also {
    if (!isRegionValid(region)) {
        throw ConfigurationException("""Configured region "$region" is invalid. A region must be a valid URI host component.""")
    }
}
