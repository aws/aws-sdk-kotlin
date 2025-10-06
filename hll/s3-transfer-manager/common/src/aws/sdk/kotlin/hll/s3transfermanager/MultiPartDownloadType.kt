package aws.sdk.kotlin.hll.s3transfermanager

/**
 * TODO
 */
public sealed interface MultiPartDownloadType

/**
 * TODO
 */
public object Range : MultiPartDownloadType

/**
 * TODO
 */
public object Part : MultiPartDownloadType