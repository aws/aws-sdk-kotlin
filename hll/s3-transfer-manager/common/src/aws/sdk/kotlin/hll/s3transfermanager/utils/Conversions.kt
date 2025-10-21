/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.utils

import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileResponse
import aws.sdk.kotlin.services.s3.model.CompleteMultipartUploadResponse
import aws.sdk.kotlin.services.s3.model.CreateMultipartUploadRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectResponse

internal fun PutObjectResponse.toUploadFileResponse(): UploadFileResponse =
    UploadFileResponse {
        bucketKeyEnabled = this@toUploadFileResponse.bucketKeyEnabled
        checksumCrc32 = this@toUploadFileResponse.checksumCrc32
        checksumCrc32C = this@toUploadFileResponse.checksumCrc32C
        checksumCrc64Nvme = this@toUploadFileResponse.checksumCrc64Nvme
        checksumSha1 = this@toUploadFileResponse.checksumSha1
        checksumSha256 = this@toUploadFileResponse.checksumSha256
        checksumType = this@toUploadFileResponse.checksumType
        eTag = this@toUploadFileResponse.eTag
        expiration = this@toUploadFileResponse.expiration
        requestCharged = this@toUploadFileResponse.requestCharged
        sseCustomerAlgorithm = this@toUploadFileResponse.sseCustomerAlgorithm
        sseCustomerKeyMd5 = this@toUploadFileResponse.sseCustomerKeyMd5
        ssekmsEncryptionContext = this@toUploadFileResponse.ssekmsEncryptionContext
        ssekmsKeyId = this@toUploadFileResponse.ssekmsKeyId
        serverSideEncryption = this@toUploadFileResponse.serverSideEncryption
        versionId = this@toUploadFileResponse.versionId
    }

internal fun CompleteMultipartUploadResponse.toUploadFileResponse(): UploadFileResponse =
    UploadFileResponse {
        bucketKeyEnabled = this@toUploadFileResponse.bucketKeyEnabled
        checksumCrc32 = this@toUploadFileResponse.checksumCrc32
        checksumCrc32C = this@toUploadFileResponse.checksumCrc32C
        checksumCrc64Nvme = this@toUploadFileResponse.checksumCrc64Nvme
        checksumSha1 = this@toUploadFileResponse.checksumSha1
        checksumSha256 = this@toUploadFileResponse.checksumSha256
        checksumType = this@toUploadFileResponse.checksumType
        eTag = this@toUploadFileResponse.eTag
        expiration = this@toUploadFileResponse.expiration
        requestCharged = this@toUploadFileResponse.requestCharged
        ssekmsKeyId = this@toUploadFileResponse.ssekmsKeyId
        serverSideEncryption = this@toUploadFileResponse.serverSideEncryption
        versionId = this@toUploadFileResponse.versionId
    }

internal fun UploadFileRequest.toPutObjectRequest(): PutObjectRequest =
    PutObjectRequest {
        acl = this@toPutObjectRequest.acl
        body = this@toPutObjectRequest.body
        bucket = this@toPutObjectRequest.bucket
        bucketKeyEnabled = this@toPutObjectRequest.bucketKeyEnabled
        cacheControl = this@toPutObjectRequest.cacheControl
        checksumAlgorithm = this@toPutObjectRequest.checksumAlgorithm
        checksumCrc32 = this@toPutObjectRequest.checksumCrc32
        checksumCrc32C = this@toPutObjectRequest.checksumCrc32C
        checksumCrc64Nvme = this@toPutObjectRequest.checksumCrc64Nvme
        checksumSha1 = this@toPutObjectRequest.checksumSha1
        checksumSha256 = this@toPutObjectRequest.checksumSha256
        contentDisposition = this@toPutObjectRequest.contentDisposition
        contentEncoding = this@toPutObjectRequest.contentEncoding
        contentLanguage = this@toPutObjectRequest.contentLanguage
        contentLength = this@toPutObjectRequest.body?.contentLength
        contentType = this@toPutObjectRequest.contentType
        expectedBucketOwner = this@toPutObjectRequest.expectedBucketOwner
        expires = this@toPutObjectRequest.expires
        grantFullControl = this@toPutObjectRequest.grantFullControl
        grantRead = this@toPutObjectRequest.grantRead
        grantReadAcp = this@toPutObjectRequest.grantReadAcp
        grantWriteAcp = this@toPutObjectRequest.grantWriteAcp
        ifMatch = this@toPutObjectRequest.ifMatch
        ifNoneMatch = this@toPutObjectRequest.ifNoneMatch
        key = this@toPutObjectRequest.key
        metadata = this@toPutObjectRequest.metadata
        objectLockLegalHoldStatus = this@toPutObjectRequest.objectLockLegalHoldStatus
        objectLockMode = this@toPutObjectRequest.objectLockMode
        objectLockRetainUntilDate = this@toPutObjectRequest.objectLockRetainUntilDate
        requestPayer = this@toPutObjectRequest.requestPayer
        sseCustomerAlgorithm = this@toPutObjectRequest.sseCustomerAlgorithm
        sseCustomerKey = this@toPutObjectRequest.sseCustomerKey
        sseCustomerKeyMd5 = this@toPutObjectRequest.sseCustomerKeyMd5
        ssekmsEncryptionContext = this@toPutObjectRequest.ssekmsEncryptionContext
        ssekmsKeyId = this@toPutObjectRequest.ssekmsKeyId
        serverSideEncryption = this@toPutObjectRequest.serverSideEncryption
        storageClass = this@toPutObjectRequest.storageClass
        tagging = this@toPutObjectRequest.tagging
        websiteRedirectLocation = this@toPutObjectRequest.websiteRedirectLocation
    }

internal fun UploadFileRequest.toCreateMultiPartUploadRequest(): CreateMultipartUploadRequest =
    CreateMultipartUploadRequest {
        acl = this@toCreateMultiPartUploadRequest.acl
        bucket = this@toCreateMultiPartUploadRequest.bucket
        bucketKeyEnabled = this@toCreateMultiPartUploadRequest.bucketKeyEnabled
        cacheControl = this@toCreateMultiPartUploadRequest.cacheControl
        checksumAlgorithm = this@toCreateMultiPartUploadRequest.checksumAlgorithm
        contentDisposition = this@toCreateMultiPartUploadRequest.contentDisposition
        contentEncoding = this@toCreateMultiPartUploadRequest.contentEncoding
        contentLanguage = this@toCreateMultiPartUploadRequest.contentLanguage
        contentType = this@toCreateMultiPartUploadRequest.contentType
        expectedBucketOwner = this@toCreateMultiPartUploadRequest.expectedBucketOwner
        expires = this@toCreateMultiPartUploadRequest.expires
        grantFullControl = this@toCreateMultiPartUploadRequest.grantFullControl
        grantRead = this@toCreateMultiPartUploadRequest.grantRead
        grantReadAcp = this@toCreateMultiPartUploadRequest.grantReadAcp
        grantWriteAcp = this@toCreateMultiPartUploadRequest.grantWriteAcp
        key = this@toCreateMultiPartUploadRequest.key
        metadata = this@toCreateMultiPartUploadRequest.metadata
        objectLockLegalHoldStatus = this@toCreateMultiPartUploadRequest.objectLockLegalHoldStatus
        objectLockMode = this@toCreateMultiPartUploadRequest.objectLockMode
        objectLockRetainUntilDate = this@toCreateMultiPartUploadRequest.objectLockRetainUntilDate
        requestPayer = this@toCreateMultiPartUploadRequest.requestPayer
        sseCustomerAlgorithm = this@toCreateMultiPartUploadRequest.sseCustomerAlgorithm
        sseCustomerKey = this@toCreateMultiPartUploadRequest.sseCustomerKey
        sseCustomerKeyMd5 = this@toCreateMultiPartUploadRequest.sseCustomerKeyMd5
        ssekmsEncryptionContext = this@toCreateMultiPartUploadRequest.ssekmsEncryptionContext
        ssekmsKeyId = this@toCreateMultiPartUploadRequest.ssekmsKeyId
        serverSideEncryption = this@toCreateMultiPartUploadRequest.serverSideEncryption
        storageClass = this@toCreateMultiPartUploadRequest.storageClass
        tagging = this@toCreateMultiPartUploadRequest.tagging
        websiteRedirectLocation = this@toCreateMultiPartUploadRequest.websiteRedirectLocation
    }
