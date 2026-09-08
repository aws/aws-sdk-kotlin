/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.test.codegen.smoketest

import aws.sdk.kotlin.codegen.smoketests.AWS_SERVICE_FILTER
import aws.sdk.kotlin.codegen.smoketests.AWS_SKIP_TAGS
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmokeTestE2ETest {
    @Test
    fun successService() = runTest {
        val output = StringBuilder()
        val runner = aws.sdk.kotlin.test.codegen.smoketest.successService.smoketests.SmokeTestRunner(
            TestPlatformProvider.of(),
            output,
        )
        val success = runner.runAllTests()
        assertTrue(success, "Unexpected failures running successService E2E smoke test. Full output: $output")

        assertContains(output, "ok SuccessService SuccessTest - no error expected from service")
        assertContains(output, "ok SuccessService SuccessTestWithTags - no error expected from service")
    }

    @Test
    fun failureService() = runTest {
        val output = StringBuilder()
        val runner = aws.sdk.kotlin.test.codegen.smoketest.failureService.smoketests.SmokeTestRunner(
            TestPlatformProvider.of(),
            output,
        )
        val success = runner.runAllTests()
        assertTrue(success, "Unexpected failures running failureService E2E smoke test. Full output: $output")

        assertContains(output, "ok FailureService FailuresTest - error expected from service")
    }

    @Test
    fun exceptionService() = runTest {
        val output = StringBuilder()
        val runner = aws.sdk.kotlin.test.codegen.smoketest.exceptionService.smoketests.SmokeTestRunner(
            TestPlatformProvider.of(),
            output,
        )
        val success = runner.runAllTests()
        assertFalse(success, "Unexpected success running exceptionService E2E smoke test. Full output: $output")

        assertContains(output, "not ok ExceptionService ExceptionTest - no error expected from service")
        assertContains(
            output,
            "# aws.smithy.kotlin.runtime.http.interceptors.SmokeTestsFailureException: Smoke test failed with HTTP status code: 400",
        )
    }

    @Test
    fun successServiceSkipTags() = runTest {
        val output = StringBuilder()
        val runner = aws.sdk.kotlin.test.codegen.smoketest.successService.smoketests.SmokeTestRunner(
            TestPlatformProvider.of(
                env = mapOf(AWS_SKIP_TAGS to "success"),
            ),
            output,
        )
        val success = runner.runAllTests()
        assertTrue(success, "Unexpected failures running successService E2E smoke test. Full output: $output")

        assertContains(output, "ok SuccessService SuccessTest - no error expected from service")
        assertContains(output, "ok SuccessService SuccessTestWithTags - no error expected from service # skip")
    }

    @Test
    fun successServiceServiceFilter() = runTest {
        val output = StringBuilder()
        val runner = aws.sdk.kotlin.test.codegen.smoketest.successService.smoketests.SmokeTestRunner(
            TestPlatformProvider.of(
                env = mapOf(AWS_SERVICE_FILTER to "Failure"), // Only run tests for services with this SDK ID
            ),
            output,
        )
        val success = runner.runAllTests()
        assertTrue(success, "Unexpected failures running successService E2E smoke test. Full output: $output")

        assertContains(output, "ok SuccessService SuccessTest - no error expected from service # skip")
        assertContains(output, "ok SuccessService SuccessTestWithTags - no error expected from service # skip")
    }
}
