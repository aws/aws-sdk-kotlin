/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.example

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.createBucket
import aws.sdk.kotlin.services.s3.headBucket
import aws.sdk.kotlin.services.s3.headObject
import aws.sdk.kotlin.services.s3.listObjects
import aws.sdk.kotlin.services.s3.model.*
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.fromFile
import aws.smithy.kotlin.runtime.content.writeToFile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files

/**
 * This program reads media files from a specified directory and uploads media files to S3.
 * After uploading it will then download uploaded files back into a local directory.
 *
 * Any file with the extension `.avi` will be processed.  To test create a text file and
 * name it such that it matches the [FILENAME_METADATA_REGEX] regex, ex:
 * `title_2000.avi`.
 *
 * When running the sample adjust the following path constants as needed for your local environment.
 */
const val BUCKET_NAME = "s3-media-ingestion-example"
const val INGESTION_DIR_PATH = "/tmp/media-in"
const val COMPLETED_DIR_PATH = "/tmp/media-processed"
const val FAILED_DIR_PATH = "/tmp/media-failed"
const val DOWNLOAD_DIR_PATH = "/tmp/media-down"

// media metadata is extracted from filename: <title>_<year>.avi
val FILENAME_METADATA_REGEX = "([\\w\\s]+)_([\\d]+).avi".toRegex()

fun main(): Unit = runBlocking {
    val client = S3Client { region = "us-east-2" }

    try {
        // Setup
        client.ensureBucketExists(BUCKET_NAME)
        listOf(COMPLETED_DIR_PATH, FAILED_DIR_PATH, DOWNLOAD_DIR_PATH).forEach { validateDirectory(it) }
        val ingestionDir = validateDirectory(INGESTION_DIR_PATH)

        // Upload files
        val uploadResults = ingestionDir
            .walk()
            .asFlow()
            .mapNotNull(::mediaMetadataExtractor)
            .map { mediaMetadata -> client.uploadToS3(mediaMetadata) }
            .toList()

        if (uploadResults.isEmpty()) {
            println("Put non-empty files matching $FILENAME_METADATA_REGEX (ex: 'lassie_1943.avi') in $ingestionDir and run example again.")
            return@runBlocking
        }

        moveFiles(uploadResults)

        // Print results of operation
        val (successes, failures) = uploadResults.partition { it is Success }
        when (failures.isEmpty()) {
            true -> println("Media uploaded successfully: $successes")
            false -> println("Successfully uploaded: $successes \nFailed to upload: $failures")
        }

        // Download files to verify
        client.listObjects { bucket = BUCKET_NAME }.contents?.forEach { obj ->
            client.getObject(
                GetObjectRequest {
                    key = obj.key
                    bucket = BUCKET_NAME
                },
            ) { response ->
                val outputFile = File(DOWNLOAD_DIR_PATH, obj.key!!)
                response.body?.writeToFile(outputFile).also { size ->
                    println("Downloaded $outputFile ($size bytes) from S3")
                }
            }
        }
    } finally {
        client.close()
    }
}

/** Check for valid S3 configuration based on account */
suspend fun S3Client.ensureBucketExists(bucketName: String) {
    if (!bucketExists(bucketName)) {
        createBucket {
            bucket = bucketName
            createBucketConfiguration {
                locationConstraint = BucketLocationConstraint.UsEast2
            }
        }
    }
}

/** Upload to S3 if file not already uploaded */
suspend fun S3Client.uploadToS3(mediaMetadata: MediaMetadata): UploadResult {
    if (keyExists(bucketName, mediaMetadata.s3KeyName)) {
        return FileExistsError("${mediaMetadata.s3KeyName} already uploaded.", mediaMetadata)
    }

    return try {
        putObject {
            bucket = bucketName
            key = mediaMetadata.s3KeyName
            body = ByteStream.fromFile(mediaMetadata.file)
            metadata = mediaMetadata.toMap()
        }
        Success("$bucketName/${mediaMetadata.s3KeyName}", mediaMetadata)
    } catch (e: Exception) { // Checking Service Exception coming in future release
        UploadError(e, mediaMetadata)
    }
}

/** Determine if a object exists in a bucket */
suspend fun S3Client.keyExists(s3bucket: String, s3key: String) = try {
    headObject {
        bucket = s3bucket
        key = s3key
    }
    true
} catch (e: Exception) { // Checking Service Exception coming in future release
    false
}

/** Determine if a object exists in a bucket */
suspend fun S3Client.bucketExists(s3bucket: String) = try {
    headBucket { bucket = s3bucket }
    true
} catch (e: Exception) { // Checking Service Exception coming in future release
    false
}

/** Move files to directories based on upload results */
fun moveFiles(uploadResults: List<UploadResult>) = uploadResults
    .map { uploadResult -> uploadResult.mediaMetadata.file.toPath() to (uploadResult is Success) }
    .forEach { (file, uploadSuccess) ->
        val targetFilePath = if (uploadSuccess) COMPLETED_DIR_PATH else FAILED_DIR_PATH
        val targetPath = File(targetFilePath)
        Files.move(file, File(targetPath, file.fileName.toString()).toPath())
    }

// Classes for S3 upload results
sealed class UploadResult {
    abstract val mediaMetadata: MediaMetadata
}
data class Success(val location: String, override val mediaMetadata: MediaMetadata) : UploadResult()
data class UploadError(val error: Throwable, override val mediaMetadata: MediaMetadata) : UploadResult()
data class FileExistsError(val reason: String, override val mediaMetadata: MediaMetadata) : UploadResult()

// Classes, properties, and functions for media metadata
data class MediaMetadata(val title: String, val year: Int, val file: File)
val MediaMetadata.s3KeyName get() = "$title-$year"
fun MediaMetadata.toMap() = mapOf("title" to title, "year" to year.toString())
fun mediaMetadataExtractor(file: File): MediaMetadata? {
    if (!file.isFile || file.length() == 0L) return null

    val matchResult = FILENAME_METADATA_REGEX.find(file.name) ?: return null

    val (title, year) = matchResult.destructured
    return MediaMetadata(title, year.toInt(), file)
}

/** Validate file path and optionally create directory */
fun validateDirectory(dirPath: String): File {
    val dir = File(dirPath)

    require(dir.isDirectory || !dir.exists()) { "Unable to use $dir" }

    if (!dir.exists()) require(dir.mkdirs()) { "Unable to create $dir" }

    return dir
}
