/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.profile.LeafProvider
import aws.sdk.kotlin.runtime.auth.credentials.profile.ProfileChain
import aws.sdk.kotlin.runtime.auth.credentials.profile.RoleArn
import aws.sdk.kotlin.runtime.auth.credentials.profile.RoleArnSource
import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.sdk.kotlin.runtime.config.profile.AwsConfigurationSource
import aws.sdk.kotlin.runtime.config.profile.FileType
import aws.sdk.kotlin.runtime.config.profile.parse
import aws.sdk.kotlin.runtime.config.profile.toSharedConfig
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.collections.attributesOf
import aws.smithy.kotlin.runtime.telemetry.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ProfileChainTest {

    private sealed class TestOutput {
        data class Chain(val chain: ProfileChain) : TestOutput()
        data class Error(val message: String) : TestOutput()
    }

    private class TestCase(
        val description: String,
        val profile: String,
        val output: TestOutput,
        val activeProfile: String = "A",
    )

    private fun chain(leaf: LeafProvider, vararg roles: RoleArn): TestOutput = TestOutput.Chain(ProfileChain(leaf, roles.toList()))

    private val kitchenSinkTests = listOf(
        TestCase(
            "basic role arn backed by static credentials",
            """
            [profile A]
            role_arn = arn:aws:iam::123456789:role/RoleA
            source_profile = B

            [profile B]
            aws_access_key_id = abc123
            aws_secret_access_key = def456
            """,
            chain(
                LeafProvider.AccessKey(Credentials("abc123", "def456")),
                RoleArn("arn:aws:iam::123456789:role/RoleA", RoleArnSource.SOURCE_PROFILE),
            ),
        ),
        TestCase(
            "ignore source profile when explicit credentials are specified",
            """
            [profile A]
            aws_access_key_id = abc123
            aws_secret_access_key = def456
            role_arn = arn:aws:iam::123456789:role/RoleA
            source_profile = B

            [profile B]
            aws_access_key_id = ghi890
            aws_secret_access_key = jkl123
            """,
            chain(
                LeafProvider.AccessKey(Credentials("abc123", "def456")),
            ),
        ),
        TestCase(
            "load role_session_name for the AssumeRole provider",
            """
            [profile A]
            role_arn = arn:aws:iam::123456789:role/RoleA
            role_session_name = my_session_name
            source_profile = B

            [profile B]
            aws_access_key_id = abc123
            aws_secret_access_key = def456
            """,
            chain(
                LeafProvider.AccessKey(Credentials("abc123", "def456")),
                RoleArn("arn:aws:iam::123456789:role/RoleA", RoleArnSource.SOURCE_PROFILE, "my_session_name"),
            ),
        ),
        TestCase(
            "load external id for the AssumeRole provider",
            """
            [profile A]
            role_arn = arn:aws:iam::123456789:role/RoleA
            external_id = my_external_id
            source_profile = B

            [profile B]
            aws_access_key_id = abc123
            aws_secret_access_key = def456
            """,
            chain(
                LeafProvider.AccessKey(Credentials("abc123", "def456")),
                RoleArn("arn:aws:iam::123456789:role/RoleA", RoleArnSource.SOURCE_PROFILE, externalId = "my_external_id"),
            ),
        ),
        TestCase(
            "self referential profile (load base credentials, ignore role)",
            """
            [profile A]
            aws_access_key_id = abc123
            aws_secret_access_key = def456
            role_arn = arn:aws:iam::123456789:role/RoleA
            source_profile = A
            """,
            chain(
                LeafProvider.AccessKey(Credentials("abc123", "def456")),
            ),
        ),
        TestCase(
            "Load credentials from a credential_source",
            """
            [profile A]
            role_arn = arn:aws:iam::123456789:role/RoleA
            credential_source = Ec2InstanceMetadata
            """,
            chain(
                LeafProvider.NamedSource("Ec2InstanceMetadata"),
                RoleArn("arn:aws:iam::123456789:role/RoleA", RoleArnSource.CREDENTIALS_SOURCE),
            ),
        ),
        TestCase(
            "role_arn without source source_profile",
            """
            [profile A]
            role_arn = arn:aws:iam::123456789:role/RoleA
            """,
            TestOutput.Error("profile (A) must contain `source_profile` or `credential_source` but neither were defined"),
        ),
        TestCase(
            "source profile and credential source both present",
            """
            [profile A]
            role_arn = arn:aws:iam::123456789:role/RoleA
            credential_source = Environment
            source_profile = B

            [profile B]
            aws_access_key_id = !23
            aws_secret_access_key = 456
            """,
            TestOutput.Error("profile (A) contained both `source_profile` and `credential_source`. Only one or the other can be defined."),
        ),
        TestCase(
            "partial static credentials (missing secret) in source profile leads to credentials not being found",
            """
            [profile A]
            role_arn = arn:foo
            source_profile = B

            [profile B]
            aws_access_key_id = abc123
            """,
            TestOutput.Error("profile (B) did not contain credential information"),
        ),
        TestCase(
            "partial static credentials (missing access key) in source profile leads to credentials not being found",
            """
            [profile A]
            role_arn = arn:foo
            source_profile = B

            [profile B]
            aws_secret_access_key = abc123
            """,
            TestOutput.Error("profile (B) did not contain credential information"),
        ),
        TestCase(
            "missing credentials error (empty source profile)",
            """
            [profile A]
            role_arn = arn:foo
            source_profile = B

            [profile B]
            """,
            TestOutput.Error("profile (B) did not contain credential information"),
        ),
        TestCase(
            "profile only contains configuration",
            """
            [profile A]
            ec2_metadata_service_endpoint_mode = IPv6
            """,
            TestOutput.Error("profile (A) did not contain credential information"),
        ),
        TestCase(
            "missing source profile",
            """
            [profile A]
            role_arn = arn:foo
            source_profile = B
            """,
            TestOutput.Error("could not find source profile B referenced from A"),
        ),
        TestCase(
            "multiple chained assume role profiles",
            """
            [profile A]
            role_arn = arn:aws:iam::123456789:role/RoleA
            source_profile = B

            [profile B]
            role_arn = arn:aws:iam::123456789:role/RoleB
            source_profile = C

            [profile C]
            aws_access_key_id = mno456
            aws_secret_access_key = pqr789
            """,
            chain(
                LeafProvider.AccessKey(Credentials("mno456", "pqr789")),
                RoleArn("arn:aws:iam::123456789:role/RoleB", RoleArnSource.SOURCE_PROFILE),
                RoleArn("arn:aws:iam::123456789:role/RoleA", RoleArnSource.SOURCE_PROFILE),
            ),
        ),
        TestCase(
            "chained assume role profiles with static credentials (ignore assume role when static credentials present)",
            """
            [profile A]
            role_arn = arn:aws:iam::123456789:role/RoleA
            web_identity_token_file = bug/if/returned
            source_profile = B

            [profile B]
            role_arn = arn:aws:iam::123456789:role/RoleB
            source_profile = C
            aws_access_key_id = profile_b_key
            aws_secret_access_key = profile_b_secret

            [profile C]
            aws_access_key_id = bug_if_returned
            aws_secret_access_key = bug_if_returned
            """,
            chain(
                LeafProvider.AccessKey(Credentials("profile_b_key", "profile_b_secret")),
                RoleArn("arn:aws:iam::123456789:role/RoleA", RoleArnSource.SOURCE_PROFILE),
            ),
        ),
        TestCase(
            "assume role profile infinite loop",
            """
            [profile A]
            role_arn = arn:aws:iam::123456789:role/RoleA
            source_profile = B

            [profile B]
            role_arn = arn:aws:iam::123456789:role/RoleB
            source_profile = A
            """,
            TestOutput.Error("profile formed an infinite loop: A -> B -> A"),
        ),
        TestCase(
            "infinite loop with web identity token",
            """
            [profile A]
            role_arn = arn:aws:iam::123456789:role/RoleA
            web_identity_token_file=bug/if/returned
            source_profile = B

            [profile B]
            role_arn = arn:aws:iam::123456789:role/RoleB
            source_profile = A
            """,
            TestOutput.Error("profile formed an infinite loop: A -> B -> A"),
        ),
        TestCase(
            "web identity role",
            """
            [profile A]
            role_arn = arn:aws:iam::123456789:role/RoleA
            web_identity_token_file = /var/token.jwt
            """,
            chain(
                LeafProvider.WebIdentityTokenRole("arn:aws:iam::123456789:role/RoleA", "/var/token.jwt"),
            ),
        ),
        TestCase(
            "web identity role with session name",
            """
            [profile A]
            role_arn = arn:aws:iam::123456789:role/RoleA
            web_identity_token_file = /var/token.jwt
            role_session_name = some_session_name
            """,
            chain(
                LeafProvider.WebIdentityTokenRole("arn:aws:iam::123456789:role/RoleA", "/var/token.jwt", "some_session_name"),
            ),
        ),
        TestCase(
            "web identity missing role arn",
            """
            [profile A]
            web_identity_token_file = /var/token.jwt
            """,
            TestOutput.Error("profile (A) missing `role_arn`"),
        ),
        TestCase(
            "web identity token as source profile",
            """
            [profile A]
            role_arn = arn:aws:iam::123456789:role/RoleA
            source_profile = B

            [profile B]
            role_arn = arn:aws:iam::123456789:role/RoleB
            web_identity_token_file = /var/token.jwt
            role_session_name = some_session_name
            """,
            chain(
                LeafProvider.WebIdentityTokenRole("arn:aws:iam::123456789:role/RoleB", "/var/token.jwt", "some_session_name"),
                RoleArn("arn:aws:iam::123456789:role/RoleA", RoleArnSource.SOURCE_PROFILE),
            ),
        ),
        TestCase(
            "single sign on",
            """
            [profile A]
            sso_start_url = https://d-92671207e4.awsapps.com/start
            sso_region = us-east-2
            sso_account_id = 1234567
            sso_role_name = RoleA
            """,
            chain(
                LeafProvider.LegacySso("https://d-92671207e4.awsapps.com/start", "us-east-2", "1234567", "RoleA"),
            ),
        ),
        TestCase(
            "single sign on as source profile",
            """
            [profile A]
            role_arn = arn:aws:iam::123456789:role/RoleA
            region = us-west-1
            source_profile = B
            
            [profile B]
            sso_start_url = https://d-92671207e4.awsapps.com/start
            sso_region = us-east-2
            sso_account_id = 1234567
            sso_role_name = RoleA
            """,
            chain(
                LeafProvider.LegacySso("https://d-92671207e4.awsapps.com/start", "us-east-2", "1234567", "RoleA"),
                RoleArn("arn:aws:iam::123456789:role/RoleA", RoleArnSource.SOURCE_PROFILE),
            ),
        ),
        TestCase(
            "single sign on missing start url",
            """
            [profile A]
            sso_region = us-east-2
            sso_account_id = 1234567
            sso_role_name = RoleA
            """,
            TestOutput.Error("profile (A) missing `sso_start_url`"),
        ),
        TestCase(
            "single sign on missing sso region",
            """
            [profile A]
            sso_start_url = https://d-92671207e4.awsapps.com/start
            sso_account_id = 1234567
            sso_role_name = RoleA
            """,
            TestOutput.Error("profile (A) missing `sso_region`"),
        ),
        TestCase(
            "single sign on missing account id",
            """
            [profile A]
            sso_start_url = https://d-92671207e4.awsapps.com/start
            sso_region = us-east-2
            sso_role_name = RoleA
            """,
            TestOutput.Error("profile (A) missing `sso_account_id`"),
        ),
        TestCase(
            "single sign on missing role name",
            """
            [profile A]
            sso_start_url = https://d-92671207e4.awsapps.com/start
            sso_region = us-east-2
            sso_account_id = 1234567
            """,
            TestOutput.Error("profile (A) missing `sso_role_name`"),
        ),
        TestCase(
            "process credentials with absolute path",
            """
            [profile A]
            credential_process = /home/ec2-user/credential_provider.o
            """,
            chain(
                LeafProvider.Process("/home/ec2-user/credential_provider.o"),
            ),
        ),
        TestCase(
            "process credentials resolved from PATH with additional arguments",
            """
            [profile A]
            credential_process = credential_provider --flag true
            """,
            chain(
                LeafProvider.Process("credential_provider --flag true"),
            ),
        ),
        TestCase(
            "sso-session as source profile",
            """
            [profile A]
            role_arn = arn:aws:iam::123456789:role/RoleA
            region = us-west-1
            source_profile = B
            
            [profile B]
            sso_role_name = RoleA
            sso_account_id = 1234567
            sso_session = my-session
            
            [sso-session my-session]
            sso_region = us-east-2
            sso_start_url = https://d-92671207e4.awsapps.com/start
            """,
            chain(
                LeafProvider.SsoSession("my-session", "https://d-92671207e4.awsapps.com/start", "us-east-2", "1234567", "RoleA"),
                RoleArn("arn:aws:iam::123456789:role/RoleA", RoleArnSource.SOURCE_PROFILE),
            ),
        ),
        TestCase(
            "sso-session missing start url",
            """
            [profile A]
            sso_account_id = 1234567
            sso_role_name = RoleA
            sso_session = my-session
            
            [sso-session my-session]
            sso_region = us-east-2
            """,
            TestOutput.Error("sso-session (my-session) missing `sso_start_url`"),
        ),
        TestCase(
            "sso-session missing sso_region",
            """
            [profile A]
            sso_account_id = 1234567
            sso_role_name = RoleA
            sso_session = my-session
            
            [sso-session my-session]
            sso_start_url = https://d-92671207e4.awsapps.com/start
            """,
            TestOutput.Error("sso-session (my-session) missing `sso_region`"),
        ),
        TestCase(
            "sso-session and profile define differing sso_region",
            """
            [profile A]
            sso_account_id = 1234567
            sso_role_name = RoleA
            sso_region = us-east-1
            sso_session = my-session
            
            [sso-session my-session]
            sso_start_url = https://d-92671207e4.awsapps.com/start
            sso_region = us-west-2
            """,
            TestOutput.Error("sso-session (my-session) sso_region = `us-west-2` does not match profile (A) sso_region = `us-east-1`"),
        ),
        TestCase(
            "sso-session and profile define differing sso_start_url",
            """
            [profile A]
            sso_account_id = 1234567
            sso_role_name = RoleA
            sso_start_url = https://d-1
            sso_session = my-session
            
            [sso-session my-session]
            sso_start_url = https://d-2
            sso_region = us-west-2
            """,
            TestOutput.Error("sso-session (my-session) sso_start_url = `https://d-2` does not match profile (A) sso_start_url = `https://d-1`"),
        ),
    )

    private val precedenceTests = listOf(
        TestCase(
            "static credentials precedence test",
            """
            [profile A]
            aws_access_key_id = 1
            aws_secret_access_key = 2
            aws_session_token = 3
            aws_account_id = 4
            source_profile = B
            role_arn = some-arn
            web_identity_token_file = /some/path
            sso_session = dev\nsso_account_id = 12345678901
            sso_role_name = role
            sso_region = us-west-2
            sso_start_url = https://some.url
            credential_process = some/process
            
            [profile B]
            aws_access_key_id = 0
            aws_secret_access_key = 0
            aws_session_token = 0
            aws_account_id = 0
            
            [sso-session dev]
            sso_region = us-west-2
            sso_start_url = https://some.url
            """,
            chain(
                LeafProvider.AccessKey(
                    Credentials(
                        "1",
                        "2",
                        "3",
                        attributes = attributesOf {
                            AwsClientOption.AccountId to "4"
                        },
                    ),
                ),
            ),
        ),
        TestCase(
            "assume role with source profile precedence test",
            """
            [profile A]
            source_profile = B
            role_arn = some-arn
            web_identity_token_file = /some/path
            sso_session = dev
            sso_account_id = 12345678901
            sso_role_name = role
            sso_region = us-west-2
            sso_start_url = https://some.url
            credential_process = some/process
            
            [profile B]
            aws_access_key_id = 1
            aws_secret_access_key = 2
            aws_session_token = 3
            aws_account_id = 4
            
            [sso-session dev]
            sso_region = us-west-2
            sso_start_url = https://some.url
            """,
            chain(
                LeafProvider.AccessKey(
                    Credentials(
                        "1",
                        "2",
                        "3",
                        attributes = attributesOf {
                            AwsClientOption.AccountId to "4"
                        },
                    ),
                ),
                RoleArn("some-arn", RoleArnSource.SOURCE_PROFILE),
            ),
        ),
        TestCase(
            "assume role with named provider / credential source precedence test",
            """
            [profile A]
            role_arn = some-arn
            credential_source = Ec2InstanceMetadata
            web_identity_token_file = /some/path
            sso_session = dev
            sso_account_id = 12345678901
            sso_role_name = role
            sso_region = us-west-2
            sso_start_url = https://some.url
            credential_process = some/process
            
            [sso-session dev]
            sso_region = us-west-2
            sso_start_url = https://some.url
            """,
            chain(
                LeafProvider.NamedSource("Ec2InstanceMetadata"),
                RoleArn("some-arn", RoleArnSource.CREDENTIALS_SOURCE),
            ),
        ),
        TestCase(
            "web identity token / STS precedence test",
            """
            [profile A]
            role_arn = some-arn
            web_identity_token_file = /some/path
            sso_session = dev
            sso_account_id = 12345678901
            sso_role_name = role
            sso_region = us-west-2
            sso_start_url = https://some.url
            credential_process = some/process
            
            [sso-session dev]
            sso_region = us-west-2
            sso_start_url = https://some.url
            """,
            chain(
                LeafProvider.WebIdentityTokenRole("some-arn", "/some/path"),
            ),
        ),
        TestCase(
            "SSO role precedence test",
            """
            [profile A]
            sso_session = dev
            sso_account_id = 12345678901
            sso_role_name = role
            sso_region = us-west-2
            sso_start_url = https://some.url
            credential_process = some/process
            
            [sso-session dev]
            sso_region = us-west-2
            sso_start_url = https://some.url
            """,
            chain(
                LeafProvider.SsoSession("dev", "https://some.url", "us-west-2", "12345678901", "role"),
            ),
        ),
        TestCase(
            "legacy SSO precedence test",
            """
            [profile A]
            sso_account_id = 12345678901
            sso_role_name = role
            sso_region = us-west-2
            sso_start_url = https://some.url
            credential_process = some/process
            """,
            chain(
                LeafProvider.LegacySso("https://some.url", "us-west-2", "12345678901", "role"),
            ),
        ),
        TestCase(
            "process precedence test",
            """
            [profile A]
            credential_process = some/process
            """,
            chain(
                LeafProvider.Process("some/process"),
            ),
        ),
        TestCase(
            "empty profile",
            """
            [profile A]
            """,
            TestOutput.Error("profile (A) did not contain credential information"),
        ),
    )

    private fun List<TestCase>.run() = this.forEachIndexed { idx, test ->
        val profiles = parse(Logger.None, FileType.CONFIGURATION, test.profile.trimIndent())
        val source = AwsConfigurationSource(test.activeProfile, "not-needed", "not-needed")
        val config = profiles.toSharedConfig(source)
        val result = runCatching { ProfileChain.resolve(config) }

        when {
            result.isFailure && test.output is TestOutput.Chain -> fail("[idx=$idx, desc=${test.description}]: expected success but chain failed to resolve: $result")
            result.isSuccess && test.output is TestOutput.Error -> fail("[idx=$idx, desc=${test.description}]: expected failure but chain resolved successfully: $result")
            result.isFailure && test.output is TestOutput.Error -> {
                val ex = result.exceptionOrNull() ?: fail("[idx=$idx, desc=${test.description}]: expected exception")
                assertEquals(test.output.message, ex.message, "[idx=$idx, desc=${test.description}]: expected exception")
            }
            else -> {
                val actual = result.getOrThrow()
                val expected = test.output as TestOutput.Chain
                assertEquals(expected.chain, actual, "[idx=$idx, desc=${test.description}]: chains not equal")
            }
        }
    }

    @Test
    fun testProfileChainResolution() {
        kitchenSinkTests.run()
        precedenceTests.run()
    }
}
