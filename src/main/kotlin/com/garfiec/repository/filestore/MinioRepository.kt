package com.garfiec.repository.filestore

import com.garfiec.util.Environment
import com.garfiec.util.s3.endWithSlash
import io.minio.*
import io.minio.http.Method
import java.io.File
import kotlin.time.Duration

class MinioRepository(private val environment: Environment): S3Repository {
    private val minioClient = MinioClient.builder()
        .endpoint(environment.minioUrl)
        .credentials(environment.minioAccessKey, environment.minioSecretKey)
        .build()

    init {
        FileBuckets.entries.forEach { bucket ->
            // Check if bucket exists
            val doesBucketExist = minioClient.bucketExists(
                BucketExistsArgs
                    .builder()
                    .bucket(bucket.displayName)
                    .build()
            )

            val config = if (bucket.isPublic) {
                publicPolicy(bucket.displayName)
            } else {
                privatePolicy(bucket.displayName)
            }

            // Create if it doesn't exist
            if (!doesBucketExist) {
                minioClient.makeBucket(
                    MakeBucketArgs
                        .builder()
                        .bucket(bucket.displayName)
                        .build()
                )
            }

            // Update bucket policy
            minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                    .bucket(bucket.displayName)
                    .config(config)
                    .build()
            )
        }
    }

    private fun publicPolicy(bucketName: String) =
        """
        {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Effect": "Allow",
                    "Principal": "*",
                    "Action": ["s3:GetObject"],
                    "Resource": ["arn:aws:s3:::$bucketName/*"]
                }
            ]
        }
        """.trimIndent()

    private fun privatePolicy(bucketName: String) =
        """
        {
            "Version": "2012-10-17",
            "Statement": []
        }
        """.trimIndent()

    override suspend fun storeImage(
        image: File,
        imageDescription: String,
        bucketName: String,
        objectName: String,
        isPublic: Boolean
    ) {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .stream(
                    image.inputStream(),
                    image.length(),
                    -1
                )
                .contentType("application/octet-stream") // Set the appropriate content type
                .build()
        )
    }

    override suspend fun getImageUrl(
        bucketName: String,
        objectName: String,
        usePresignedUrl: Boolean,
        presignedUrlExpiration: Duration
    ): String {
        return if (usePresignedUrl) {
            minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .`object`(objectName)
                    .expiry(presignedUrlExpiration.inWholeSeconds.toInt())
                    .build()
            )
        } else {
            "${environment.minioUrl.endWithSlash()}${bucketName.endWithSlash()}$objectName"
        }
    }
}
