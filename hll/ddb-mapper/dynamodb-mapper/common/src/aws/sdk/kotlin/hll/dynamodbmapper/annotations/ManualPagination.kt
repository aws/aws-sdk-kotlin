/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.annotations

/**
 * Indicates that an operation invocation method (e.g., `query`) does not provide automatic pagination. While desirable
 * in advanced scenarios, the typical use case should favor the paginated equivalent (e.g., `queryPaginated`).
 */
@RequiresOptIn(message = "This method does not provide automatic pagination over results and should only be used in advanced scenarios. Where possible, consider using the paginated equivalent. To explicitly opt into using this method, annotate the call site with `@OptIn(ManualPagination::class)`.")
public annotation class ManualPagination(val paginatedEquivalent: String)
