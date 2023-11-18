package com.garfiec.service

import com.garfiec.db.schema.ImageDBService
import com.garfiec.db.schema.UserDBService
import com.garfiec.repository.filestore.FileBuckets
import com.garfiec.repository.filestore.ObjectPrefix
import com.garfiec.repository.filestore.S3Repository
import com.garfiec.util.image.convertToJpg
import java.io.File
import java.util.UUID

/**
 * This class handles uploading images to s3 and persisting the reference in the DB
 * Functions in this class are for internal use. No public handling of images here.
 */
class ImageService(
    private val userDBService: UserDBService,
    private val imageDBService: ImageDBService,
    private val s3Repository: S3Repository
) {
    data class ImageReference(val imageId: Int, val imageUuid: String, val imageDescription: String, val imageUrl: String)

    /**
     * In the future, stream directly to s3
     */
    suspend fun createImage(
        ownerUserId: Int?,
        imageFile: File,
        imageDescription: String,
        bucket: FileBuckets,
        objectPrefix: ObjectPrefix
    ): ImageReference? {
        return try {
            // First validate that it's an image. Then convert it to webp.
            val jpgImage = imageFile.convertToJpg()

            // If ownerUserId is set, validate that user id exists


            // Create variables for names across s3 and db
            val description = imageDescription
            val bucketName = bucket.displayName
            val objectName = "${objectPrefix.displayName}/${UUID.randomUUID()}.jpg"

            // Upload to s3 storage
            s3Repository.storeImage(
                image = jpgImage,
                imageDescription = imageDescription,
                bucketName = bucketName,
                objectName = objectName,
                isPublic = ownerUserId == null
            )

            // Retrieve s3 url
            val imageUrl = s3Repository.getImageUrl(
                bucketName = bucketName,
                objectName = objectName,
                usePresignedUrl = ownerUserId != null
            )

            // Store in DB
            val dbImage = imageDBService.addImage(
                ownerId = ownerUserId,
                description = description,
                bucketName = bucketName,
                objectKey = objectName,
                isPublic = ownerUserId == null

            )
            ImageReference(
                imageId = dbImage.id.value,
                imageUuid = dbImage.imageUuid.toString(),
                imageDescription = dbImage.imageDescription,
                imageUrl = imageUrl
            )
        } catch (e: Exception) {
            // Should this be caught? Perhaps we should continue to throw so that the error gets seen? Or maybe this should just be handled internally?
            println(e)
            null
        }

    }

    suspend fun getImage(imageId: Int): ImageReference? {
        return try {
            val dbImage = imageDBService.getImage(imageId = imageId) ?: return null // Maybe throw no image exist exception?

            val imageUrl = s3Repository.getImageUrl(
                bucketName = dbImage.bucket,
                objectName = dbImage.objectKey,
                usePresignedUrl = !dbImage.isPublic
            )

            ImageReference(
                imageId = dbImage.id.value,
                imageUuid = dbImage.imageUuid.toString(),
                imageDescription = dbImage.imageDescription,
                imageUrl = imageUrl
            )
        } catch (e: Exception) {

            null
        }
    }
}
