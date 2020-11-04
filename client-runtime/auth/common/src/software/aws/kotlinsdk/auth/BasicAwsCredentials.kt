package software.aws.kotlinsdk.auth

/**
 * Note: the code in this file is a placeholder for another implementation with CRT taken into account.
 */

/*
This implementation is modeled from the Java SDK v2 Credentials APIs.  The API is reduced from the Java version.
 */
interface AwsCredentials {
    val accessKey: String
    val secretKey: String
}

/**
 * Basic type to hold access/secret pair.
 */
data class BasicAwsCredentials(override val accessKey: String, override val secretKey: String) : AwsCredentials

/**
 * Aws Credentials with session token.
 */
data class SessionAwsCredentials(override val accessKey: String, override val secretKey: String, val sessionToken: String) : AwsCredentials

/**
 * A special instance of AWS credentials that signals that no credentials are to be supplied in association with an operation.
 */
object ANONYMOUS_AWS_CREDENTIALS : AwsCredentials {
    override val accessKey: String
        get() = error("Anonymous AWS Credentials supplies no data.")
    override val secretKey: String
        get() = error("Anonymous AWS Credentials supplies no data.")
}

/**
 * A function that may return AWS credentials or return null if none available or found.
 */
typealias AwsCredentialsProvider = () -> AwsCredentials?

/**
 * A list that contains functions that may return AWS credentials.  It is expected that the list
 * will be evaluated in order and the first function to return non-null value will be used to supply
 * the credentials.
 */
typealias AwsCredentialsProviders = List<AwsCredentialsProvider>

/**
 * Scan a list of AwsCredentialProviders and return the first one that is able to resolve credentials.
 */
fun AwsCredentialsProviders.find(): AwsCredentials? =
    asSequence().map { it.invoke() }.first { it != null }

/**
 * Platform function to pass a file to an input function as a sequence of lines.
 */
expect fun <T> platformFileReader(customFilePath: String? = null, block: (Sequence<String>) -> T): T?

/**
 * Parses the AWS credentials file for aws access and secret keys for default or specified profile.
 *
 * @param profileFilePath A custom path to look for the credentials file.  If not passed, the default path is used.
 * @param profileName The profile name in the credentials file to return.
 */
class ProfileAwsCredentialsProvider(private val profileFilePath: String? = null, private val profileName: String = "default") : AwsCredentialsProvider {

    override fun invoke(): AwsCredentials? =
        platformFileReader(profileFilePath, ::loadCredentials)?.let { BasicAwsCredentials(it.first, it.second) }

    private fun loadCredentials(credentialsFile: Sequence<String>): Pair<String, String>? {
        var profileFound = false
        var accessKey: String? = null
        var secretKey: String? = null

        credentialsFile.forEach lit@{ line ->
            val trimmedLine = line.trim()
            when (profileFound) {
                false -> if (trimmedLine.profileNameMatch(profileName)) profileFound = true
                true -> when {
                    accessKey == null && trimmedLine.accessKeyMatch() -> accessKey = trimmedLine.parseValue()
                    secretKey == null && trimmedLine.secretKeyMatch() -> secretKey = trimmedLine.parseValue()
                }
            }

            if (accessKey != null && secretKey != null) return@lit
        }

        return if (accessKey != null && secretKey != null) accessKey!! to secretKey!! else null
    }

    private fun String.profileNameMatch(name: String): Boolean = this.startsWith("[") && this.endsWith("]") && this.removeSurrounding("[", "]") == name
    private fun String.accessKeyMatch(): Boolean = this.startsWith("aws_access_key_id") && this.contains('=')
    private fun String.secretKeyMatch(): Boolean = this.startsWith("aws_secret_access_key") && this.contains('=')
    private fun String.parseValue(): String = this.split('=')[1].trim()
}
