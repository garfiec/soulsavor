package com.garfiec.db.schema

import com.garfiec.db.schema.DishDBService.DishPictures
import com.garfiec.db.schema.ImageDBService.Images
import com.garfiec.util.db.dbQuery
import com.garfiec.util.db.dbQueryAsync
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

typealias DishUuid = String

class Dish(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<Dish>(DishDBService.Dishes)
    var userId by DishDBService.Dishes.userId
    var merchantGroupMemberId by DishDBService.Dishes.merchantGroupMemberId
    var dishUuid by DishDBService.Dishes.dishUuid
    var dishName by DishDBService.Dishes.dishName
    var dishShortDescription by DishDBService.Dishes.dishShortDescription
    var dishDescription by DishDBService.Dishes.dishDescription
    var price by DishDBService.Dishes.price
    var spicinessLevel by DishDBService.Dishes.spicinessLevel
    var isPublished by DishDBService.Dishes.isPublished
}

class DishPicture(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<DishPicture>(DishPictures)

    var dish by Dish referencedOn DishPictures.dishId
    var imageId by DishPictures.imageId
    var order by DishPictures.order
}

class DishDBService(private val database: Database) {

    object Dishes : IntIdTable() {
        val userId = reference("user_id", UserDBService.Users)
        val merchantGroupMemberId = reference("merchant_group_member_id", MerchantGroupMemberDBService.MerchantGroupMembers)
        val dishUuid: Column<UUID> = uuid("dish_uuid").autoGenerate().uniqueIndex()
        val dishName = varchar("dish_name", 255)
        val dishShortDescription = text("dish_short_description")
        val dishDescription = text("dish_description")
        val price = integer("price")
//        val rating = integer("rating") // This needs to be changed in the future. Should have a ratings table where it averages this.
        val spicinessLevel = float("spiciness_level")
        val isPublished = bool("is_public").default(false)
    }

    object DishPictures : IntIdTable() {
        val dishId = reference("dish_id", Dishes)
        val imageId = reference("image_id", Images)
        val order = integer("order").default(0)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Dishes, DishPictures)
        }
    }

    suspend fun merchantGroupOfDish(dishId: Int): MerchantGroup {
        return dbQuery(database) {
            val dish = Dish.findById(dishId) ?: throw Exception("Dish does not exist")
            (Dishes innerJoin MerchantGroupMemberDBService.MerchantGroupMembers innerJoin MerchantGroupDBService.MerchantGroups)
//                .slice(MerchantGroupMemberDBService.MerchantGroupMembers.group)
                .select { MerchantGroupMemberDBService.MerchantGroupMembers.id eq dish.merchantGroupMemberId }
                .map { row -> MerchantGroup.wrapRow(row) }
                .singleOrNull()
                ?: throw Exception("Merchant group does not exist")
        }
    }

    suspend fun addDish(
        userId: Int,
        merchantGroupMemberId: Int,
        dishName: String,
        dishShortDescription: String = "",
        dishDescription: String = "",
        price: Int = 0,
        spicinessLevel: Float = 5f,
        isPublished: Boolean = false
    ): Dish {
        return dbQuery(database) {
            Dish.new {
                this.userId = EntityID(userId, UserDBService.Users)
                this.merchantGroupMemberId =
                    EntityID(merchantGroupMemberId, MerchantGroupMemberDBService.MerchantGroupMembers)
                this.dishName = dishName
                this.dishShortDescription = dishShortDescription
                this.dishDescription = dishDescription
                this.price = price
                this.spicinessLevel = spicinessLevel
                this.isPublished = isPublished
            }
        }
    }

    suspend fun getDish(dishId: Int): Dish? {
        return dbQuery(database) {
            Dish.findById(dishId)
        }
    }

    suspend fun getDish(dishUuid: DishUuid): Dish? {
        return dbQuery(database) {
            Dish.find { Dishes.dishUuid eq UUID.fromString(dishUuid) }.singleOrNull()
        }
    }

    suspend fun getDishesFromMerchantMember(merchantGroupMemberId: Int): List<Dish> {
        return dbQuery(database) {
            Dish.find { Dishes.merchantGroupMemberId eq merchantGroupMemberId }.toList()
        }
    }

    suspend fun removeDish(dishId: Int) {
        return dbQuery(database) {
            Dish.findById(dishId)?.delete()
        }
    }

    suspend fun updateDishName(dishId: Int, dishName: String) {
        return dbQuery(database) {
            Dish.findById(dishId)?.dishName = dishName
        }
    }

    suspend fun updateShortDescription(dishId: Int, shortDescription: String) {
        return dbQuery(database) {
            Dish.findById(dishId)?.dishShortDescription = shortDescription
        }
    }

    suspend fun updateDescription(dishId: Int, description: String) {
        return dbQuery(database) {
            Dish.findById(dishId)?.dishDescription = description
        }
    }

    suspend fun updatePrice(dishId: Int, price: Int) {
        return dbQuery(database) {
            Dish.findById(dishId)?.price = price
        }
    }

    suspend fun updateSpicinessLevel(dishId: Int, spicinessLevel: Float) {
        return dbQuery(database) {
            Dish.findById(dishId)?.spicinessLevel = spicinessLevel
        }
    }

    suspend fun updateIsPublished(dishId: Int, isPublished: Boolean) {
        return dbQuery(database) {
            Dish.findById(dishId)?.isPublished = isPublished
        }
    }

    suspend fun addDishPicture(dishId: Int, imageId: Int) {
        return dbQuery(database) {
            val dish = Dish.findById(dishId) ?: throw Exception("Dish does not exist")
            val dishPicturesCount = DishPictures.select { DishPictures.dishId eq dishId }.count().toInt()
            DishPicture.new {
                this.dish = dish
                this.imageId = EntityID(imageId, Images)
                this.order = dishPicturesCount
            }
        }
    }

    /**
     * Returns a list of image ids in order of their order
     */
    suspend fun getDishPictureIds(dishId: Int): List<Int> {
        return dbQuery(database) {
            (Dishes innerJoin DishPictures)
//                .slice(DishPictures.imageId, DishPictures.order)
                .select(Dishes.id eq dishId)
                .map { row ->
                    val dishPicture = DishPicture.wrapRow(row)
                    dishPicture.order to dishPicture.imageId
                }
                .sortedBy { it.first }
                .map { it.second.value }
        }
    }

    suspend fun getDishPicture(dishId: Int, index: Int): Int {
        val dishPictures = getDishPictureIds(dishId)
        if (index > dishPictures.size) throw Exception("Index out of bounds")

        return dishPictures[index]
    }

    suspend fun removeDishPicture(dishId: Int, index: Int) {
        val dishPictures = getDishPictureIds(dishId)
        if (index > dishPictures.size) throw Exception("Index out of bounds")

        return dbQuery(database) {
            // Remove image at index
            val dishIndexToRemove = dishPictures[index]
            DishPictures.deleteWhere { imageId eq dishIndexToRemove }
            dishPictures.subList(index + 1, dishPictures.size).forEach { imageId ->
                DishPictures.update({ DishPictures.imageId eq imageId }) {
                    with(SqlExpressionBuilder) {
                        it.update(order, order - 1)
                    }
                }
            }
        }
    }

    /**
     * TODO This request needs to be optimized to reduce the number of independent queries
     */
    suspend fun getUserDishPermissions(
        userId: Int,
        dishId: Int
    ): DishPermissions {
        val userAsync = dbQueryAsync(database) {
            User.find { UserDBService.Users.id eq userId }.singleOrNull()
        }
        val dishAsync = dbQueryAsync(database) {
            Dish.findById(dishId)
        }

        val dish = dishAsync.await() ?: throw IllegalArgumentException("Dish not found")

        val merchantGroupOfDish = merchantGroupOfDish(dish.id.value)

        // Verify if user is in group that this dish is in
        val userMerchantGroups = dbQuery(database) {
            (MerchantGroupMemberDBService.MerchantGroupMembers innerJoin MerchantGroupDBService.MerchantGroups)
                .select { MerchantGroupMemberDBService.MerchantGroupMembers.user eq userId }
                .mapNotNull { MerchantGroup.wrapRow(it) }
        }

        return if (userAsync.await()?.id?.value == dish.userId.value) {
            DishPermissions.READ_WRITE
        } else if (userMerchantGroups.firstOrNull { it.merchantGroupUuid == merchantGroupOfDish.merchantGroupUuid } != null) {
            DishPermissions.READ
        } else {
            DishPermissions.NONE
        }
    }
}

enum class DishPermissions {
    READ,
    READ_WRITE,
    NONE;

    fun hasEditPermissions(): Boolean = this == READ_WRITE

    fun hasViewPermissions(): Boolean = this == READ_WRITE || this == READ
}
