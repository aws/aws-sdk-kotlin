/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials.profile

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials

/**
 * A standalone member of the profile chain. Leaf providers do not require
 * input credentials to provide their own credentials (e.g. IMDS, ECS, Environment, etc)
 */
internal sealed class LeafProvider {

    /**
     * A profile that specifies a named credentials source
     * e.g. `credential_source = Ec2InstanceMetadata`
     */
    data class NamedSource(val name: String) : LeafProvider()

    /**
     * A profile with explicitly configured access keys
     *
     * Example
     * ```ini
     * [profile C]
     * aws_access_key_id = AKID
     * aws_secret_access_key = secret
     * ```
     */
    data class AccessKey(val credentials: Credentials) : LeafProvider()

    /**
     * A provider that uses OIDC web identity tokens
     *
     * Example
     * ```ini
     * [profile W]
     * role_arn = arn:aws:iam:123456789:role/example
     * web_identity_token_file = /path/to/token.jwt
     * ```
     */
    data class WebIdentityTokenRole(
        val roleArn: String,
        val webIdentityTokenFile: String,
        val sessionName: String? = null,
    ) : LeafProvider()

    /**
     * A provider that uses legacy AWS SSO profile
     *
     * Example
     * ```ini
     * [profile W]
     * sso_start_url = https://my-sso-portal.awsapps.com/start
     * sso_region = us-east-1
     * sso_account_id = 123456789011
     * sso_role_name = readOnly
     * region = us-west-2
     * ```
     */
    data class LegacySso(
        val ssoStartUrl: String,
        val ssoRegion: String,
        val ssoAccountId: String,
        val ssoRoleName: String,
    ) : LeafProvider()

    /**
     * A provider that uses AWS SSO token provider
     *
     * Example
     * ```ini
     * [profile W]
     * region = us-west-2
     * sso_session = my-session
     * sso_account_id = 123456789011
     * sso_role_name = readOnly
     *
     * [sso-session my-session]
     * sso_start_url = https://my-sso-portal.awsapps.com/start
     * sso_region = us-east-1
     * ```
     */
    data class SsoSession(
        val ssoSessionName: String,
        val ssoStartUrl: String,
        val ssoRegion: String,
        val ssoAccountId: String,
        val ssoRoleName: String,
    ) : LeafProvider()

    /**
     * A provider that uses for AWS login
     *
     * Example
     * ```ini
     * [profile W]
     * login_session = arn:aws:iam::0123456789012:user/Admin
     * ```
     */
    data class LoginSession(
        val loginSessionName: String,
    ) : LeafProvider()

    /**
     * A provider that invokes a command and reads its standard output to parse credentials.
     */
    data class Process(val command: String) : LeafProvider()
}
