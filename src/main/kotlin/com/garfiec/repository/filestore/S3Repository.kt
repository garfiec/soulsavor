package com.garfiec.repository.filestore

import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

interface S3Repository {

    suspend fun storeImage(
        image: File,
        imageDescription: String,
        bucketName: String,
        objectName: String,
        isPublic: Boolean = false
    )

    suspend fun getImageUrl(
        bucketName: String,
        objectName: String,
        usePresignedUrl: Boolean = true,
        presignedUrlExpiration: Duration = 1.hours
    ): String

}
