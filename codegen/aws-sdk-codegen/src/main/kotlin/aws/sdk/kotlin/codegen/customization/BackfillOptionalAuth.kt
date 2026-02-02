/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization

import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.codegen.core.CodegenException
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.traits.OptionalAuthTrait
import software.amazon.smithy.model.transform.ModelTransformer

/**
 * Several services have operations that do not/should not be signed and need
 * to have the auth trait manually set to `[]`.
 *
 * See https://github.com/awslabs/aws-sdk-kotlin/issues/280 and https://github.com/awslabs/aws-sdk-kotlin/issues/553
 */
class BackfillOptionalAuth : KotlinIntegration {

    // service shape id -> operations that should have optional auth trait applied
    private val disabledAuthOperationsByService = mapOf(
        "com.amazonaws.sts#AWSSecurityTokenServiceV20110615" to setOf(
            "com.amazonaws.sts#AssumeRoleWithSAML",
            "com.amazonaws.sts#AssumeRoleWithWebIdentity",
        ),
        "com.amazonaws.cognitoidentity#AWSCognitoIdentityService" to setOf(
            "com.amazonaws.cognitoidentity#GetId",
            "com.amazonaws.cognitoidentity#GetOpenIdToken",
            "com.amazonaws.cognitoidentity#UnlinkIdentity",
            "com.amazonaws.cognitoidentity#GetCredentialsForIdentity",
        ),
        // https://docs.aws.amazon.com/cognito/latest/developerguide/security_iam_service-with-iam.html
        "com.amazonaws.cognitoidentityprovider#AWSCognitoIdentityProviderService" to setOf(
            "com.amazonaws.cognitoidentityprovider#AssociateSoftwareToken",
            "com.amazonaws.cognitoidentityprovider#ChangePassword",
            "com.amazonaws.cognitoidentityprovider#ConfirmDevice",
            "com.amazonaws.cognitoidentityprovider#ConfirmForgotPassword",
            "com.amazonaws.cognitoidentityprovider#ConfirmSignUp",
            "com.amazonaws.cognitoidentityprovider#DeleteUser",
            "com.amazonaws.cognitoidentityprovider#DeleteUserAttributes",
            "com.amazonaws.cognitoidentityprovider#ForgetDevice",
            "com.amazonaws.cognitoidentityprovider#ForgotPassword",
            "com.amazonaws.cognitoidentityprovider#GetDevice",
            "com.amazonaws.cognitoidentityprovider#GetUser",
            "com.amazonaws.cognitoidentityprovider#GetUserAttributeVerificationCode",
            "com.amazonaws.cognitoidentityprovider#GlobalSignOut",
            "com.amazonaws.cognitoidentityprovider#InitiateAuth",
            "com.amazonaws.cognitoidentityprovider#ListDevices",
            "com.amazonaws.cognitoidentityprovider#ResendConfirmationCode",
            "com.amazonaws.cognitoidentityprovider#RespondToAuthChallenge",
            "com.amazonaws.cognitoidentityprovider#RevokeToken",
            "com.amazonaws.cognitoidentityprovider#SetUserMFAPreference",
            "com.amazonaws.cognitoidentityprovider#SetUserSettings",
            "com.amazonaws.cognitoidentityprovider#SignUp",
            "com.amazonaws.cognitoidentityprovider#UpdateAuthEventFeedback",
            "com.amazonaws.cognitoidentityprovider#UpdateDeviceStatus",
            "com.amazonaws.cognitoidentityprovider#UpdateUserAttributes",
            "com.amazonaws.cognitoidentityprovider#VerifySoftwareToken",
            "com.amazonaws.cognitoidentityprovider#VerifyUserAttribute",
        ),
    )

    // this should happen prior to most other integrations that could rely on the presence of this trait
    override val order: Byte = -60

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        val serviceId = settings.service.toString()
        return serviceId in disabledAuthOperationsByService
    }

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val serviceId = settings.service.toString()
        val optionalAuthOperations = disabledAuthOperationsByService[serviceId] ?: throw CodegenException("expected $serviceId in disabled operations map")
        return ModelTransformer.create()
            .mapShapes(model) {
                if (optionalAuthOperations.contains(it.id.toString()) && it is OperationShape && !it.hasTrait<OptionalAuthTrait>()) {
                    it.toBuilder().addTrait(OptionalAuthTrait()).build()
                } else {
                    it
                }
            }
    }
}
