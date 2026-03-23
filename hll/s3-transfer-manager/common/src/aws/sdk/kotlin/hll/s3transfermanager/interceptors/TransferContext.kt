/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.interceptors

// TODO: Create a sealed classes to hold possible s3 and tm request and response types. Should eliminate use of casting.
/**
 * The context around an [aws.sdk.kotlin.hll.s3transfermanager.S3TransferManager] transfer.
 * Used to track transfer progress or to modify in progress transfers.
 */
public interface TransferInterceptorContext {
    /**
     * Current low level S3 request
     */
    public val s3Request: Any?

    /**
     * Current low level S3 response
     */
    public val s3Response: Any?

    /**
     * Current high level transfer manager request
     */
    public val tmRequest: Any?

    /**
     * Current high level transfer manager response
     */
    public val tmResponse: Any?

    /**
     * The amount of transferable bytes for an object
     */
    public val transferableBytes: Long?

    /**
     * The amount of transferred bytes for an object
     */
    public val transferredBytes: Long?

    /**
     * The amount of transferable objects for a directory
     */
    public val transferableObjects: Long?

    /**
     * The amount of transferred objects for a directory
     */
    public val transferredObjects: Long?
}

/**
 * The context around a [aws.sdk.kotlin.hll.s3transfermanager.S3TransferManager] transfer.
 * Used to track in progress transfers.
 */
public class TransferContext(
    override val s3Request: Any? = null,
    override val s3Response: Any? = null,
    override val tmRequest: Any? = null,
    override val tmResponse: Any? = null,
    override val transferableBytes: Long? = null,
    override val transferredBytes: Long? = null,
    override val transferableObjects: Long? = null,
    override val transferredObjects: Long? = null,
) : TransferInterceptorContext

/**
 * The context around a [aws.sdk.kotlin.hll.s3transfermanager.S3TransferManager] transfer.
 * Used to modify in progress transfers.
 */
public class MutableTransferContext(
    override var s3Request: Any? = null,
    override var s3Response: Any? = null,
    override var tmRequest: Any? = null,
    override var tmResponse: Any? = null,
    override var transferableBytes: Long? = null,
    override var transferredBytes: Long? = null,
    override var transferableObjects: Long? = null,
    override var transferredObjects: Long? = null,
) : TransferInterceptorContext {
    internal fun immutableCopy() = TransferContext(
        s3Request,
        s3Response,
        tmRequest,
        tmResponse,
        transferableBytes,
        transferredBytes,
        transferableObjects,
        transferredObjects,
    )

    internal fun copy() = MutableTransferContext(
        s3Request,
        s3Response,
        tmRequest,
        tmResponse,
        transferableBytes,
        transferredBytes,
        transferableObjects,
        transferredObjects,
    )
}
