package com.garfiec.db.schema

import com.garfiec.util.db.dbQuery
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class Image(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<Image>(ImageDBService.Images)

    var owner by ImageDBService.Images.ownerId
    var imageUuid by ImageDBService.Images.imageUuid
    var imageDescription by ImageDBService.Images.imageDescription
    var bucket by ImageDBService.Images.bucket
    var objectKey by ImageDBService.Images.objectKey
    var isPublic by ImageDBService.Images.isPublic
}

class ImageDBService(private val database: Database) {
    object Images : IntIdTable() {
        val ownerId = reference("userId", UserDBService.Users).nullable()
        val imageUuid: Column<UUID> = uuid("external_uuid").autoGenerate().uniqueIndex()
        val imageDescription = text("image_description")
        val bucket = varchar("bucket", 255)
        val objectKey = varchar("object_key", 255)
        val isPublic = bool("is_public").default(false)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Images)
        }
    }

    suspend fun addImage(ownerId: Int?, description: String, bucketName: String, objectKey: String, isPublic: Boolean): Image {
        return dbQuery(database) {
            Image.new {
                owner = ownerId?.let { EntityID(it, UserDBService.Users) }
                imageDescription = description
                bucket = bucketName
                this.objectKey = objectKey
                this.isPublic = isPublic
            }
        }
    }

    suspend fun getImage(imageId: Int): Image? {
        return dbQuery(database) {
            Image.findById(imageId)
        }
    }

    suspend fun deleteImage(imageId: Int) {
        dbQuery(database) {
            Image.findById(imageId)?.delete()
        }
    }

    suspend fun updateImageDescription(imageId: Int, description: String?) {
        dbQuery(database) {
            Image.findById(imageId)?.apply {
                description?.let { this.imageDescription = it }
            }
        }
    }

    suspend fun getAllImages(): List<Image> {
        return dbQuery(database) {
            Image.all().toList()
        }
    }

    // TODO
    suspend fun getDanglingImages(): List<Image> {
        // Assuming a dangling image is an image that's not linked to any group
        return dbQuery(database) {
            emptyList()
        }
    }
}
