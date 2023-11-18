package com.garfiec.service

import com.garfiec.db.schema.*
import com.garfiec.repository.filestore.FileBuckets
import com.garfiec.repository.filestore.ObjectPrefix
import com.garfiec.util.exception.ApiRequestException
import com.garfiec.util.image.isImageValid
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class CreateDishRequest(
    val groupMembershipId: String,
    val dishName: String,
    val dishShortDescription: String = "",
    val dishDescription: String = "",
    val price: Int = 0,
    val spicinessLevel: Float = 0f,
    val isPublished: Boolean = false,
)
@Serializable
data class DishDetailsResponse(
    val merchantGroupMemberId: String,
    val merchantGroupMemberName: String,
    val dishUuid: String,
    val dishName: String,
    val dishShortDescription: String,
    val dishDescription: String,
    val price: Int,
    val spicinessLevel: Float,
    val dishPhotos: List<DishPhotoResponse>,
    val isPublished: Boolean
)

@Serializable
data class DishPhotoResponse(
    val dishPhotoUuid: String,
    val dishPhotoUrl: String,
    val dishPhotoDescription: String
)

class DishService(
    private val userDBService: UserDBService,
    private val merchantGroupDBService: MerchantGroupDBService,
    private val merchantGroupMemberDBService: MerchantGroupMemberDBService,
    private val dishDBService: DishDBService,
    private val imageService: ImageService
) {
    // Add dish
    suspend fun createDish(
        loggedInUserId: LoggedInUser,
        createDishRequest: CreateDishRequest
    ): DishDetailsResponse {
        val merchantGroupMember = merchantGroupMemberDBService.getMember(createDishRequest.groupMembershipId)
            ?: throw ApiRequestException("User is not a member of group")
        val dish = dishDBService.addDish(
            userId = loggedInUserId.userId,
            merchantGroupMemberId = merchantGroupMember.id.value,
            dishName = createDishRequest.dishName,
            dishShortDescription = createDishRequest.dishShortDescription,
            dishDescription = createDishRequest.dishDescription,
            price = createDishRequest.price,
            spicinessLevel = createDishRequest.spicinessLevel,
            isPublished = createDishRequest.isPublished,
        )
        return createDishResponse(dish, merchantGroupMember)
    }

    // Remove dish
    suspend fun removeDish(
        loggedInUserId: LoggedInUser,
        dishUuid: String,
    ) {
        val dish = dishDBService.getDish(dishUuid) ?: throw ApiRequestException("Dish does not exist")
        if (dish.userId.value != loggedInUserId.userId) throw ApiRequestException("User does not own dish")

        dishDBService.removeDish(dish.id.value)
    }

    // Get dish
    suspend fun getDish(
        loggedInUserId: LoggedInUser,
        dishUuid: String,
    ): DishDetailsResponse {
        val dish = dishDBService.getDish(dishUuid) ?: throw ApiRequestException("Dish does not exist")

        val isUserPermitted = loggedInUserId.hasPermissionToViewDish(dish)

        if (!isUserPermitted) throw ApiRequestException("User is not permitted to view this dish")

        return createDishResponse(dish)
    }

    // List dishes by merchant group member
    suspend fun getDishesByMerchantGroupMember(
        loggedInUserId: LoggedInUser,
        groupMembershipId: String,
    ): List<DishDetailsResponse> {
        val merchantGroupMember = merchantGroupMemberDBService.getMember(groupMembershipId)
            ?: throw ApiRequestException("Merchant group member does not exist")

        val merchantGroupOfDish = merchantGroupMember.group.value

        val isUserPermitted = merchantGroupMemberDBService
            .getGroupsOfUser(loggedInUserId.userId)
            .map { it.id.value }
            .contains(merchantGroupOfDish)

        if (!isUserPermitted) throw ApiRequestException("User is not permitted to view this dish")

        return dishDBService
            .getDishesFromMerchantMember(merchantGroupMember.id.value)
            .map { dish ->
                createDishResponse(dish, merchantGroupMember)
            }
    }

    suspend fun updateDishName(
        loggedInUser: LoggedInUser,
        dishUuid: String,
        dishName: String
    ) {
        val dish = dishDBService.getDish(dishUuid) ?: throw ApiRequestException("Dish does not exist")

        val isUserPermitted = loggedInUser.hasPermissionToEditDish(dish)

        if (!isUserPermitted) throw ApiRequestException("User is not permitted to edit this dish")

        dishDBService.updateDishName(dish.id.value, dishName)
    }

    suspend fun updateShortDescription(
        loggedInUser: LoggedInUser,
        dishUuid: String,
        dishShortDescription: String
    ) {
        val dish = dishDBService.getDish(dishUuid) ?: throw ApiRequestException("Dish does not exist")

        val isUserPermitted = loggedInUser.hasPermissionToEditDish(dish)

        if (!isUserPermitted) throw ApiRequestException("User is not permitted to edit this dish")

        dishDBService.updateShortDescription(dish.id.value, dishShortDescription)
    }

    suspend fun updateDescription(
        loggedInUser: LoggedInUser,
        dishUuid: String,
        dishDescription: String
    ) {
        val dish = dishDBService.getDish(dishUuid) ?: throw ApiRequestException("Dish does not exist")

        val isUserPermitted = loggedInUser.hasPermissionToEditDish(dish)

        if (!isUserPermitted) throw ApiRequestException("User is not permitted to edit this dish")

        dishDBService.updateDescription(dish.id.value, dishDescription)
    }

    suspend fun updatePrice(
        loggedInUser: LoggedInUser,
        dishUuid: String,
        dishPrice: Int
    ) {
        val dish = dishDBService.getDish(dishUuid) ?: throw ApiRequestException("Dish does not exist")

        val isUserPermitted = loggedInUser.hasPermissionToEditDish(dish)

        if (!isUserPermitted) throw ApiRequestException("User is not permitted to edit this dish")

        dishDBService.updatePrice(dish.id.value, dishPrice)
    }

    suspend fun updateSpicinessLevel(
        loggedInUser: LoggedInUser,
        dishUuid: String,
        spicinessLevel: Float
    ) {
        val dish = dishDBService.getDish(dishUuid) ?: throw ApiRequestException("Dish does not exist")

        val isUserPermitted = loggedInUser.hasPermissionToEditDish(dish)

        if (!isUserPermitted) throw ApiRequestException("User is not permitted to edit this dish")

        dishDBService.updateSpicinessLevel(dish.id.value, spicinessLevel)
    }

    suspend fun updateIsPublished(
        loggedInUser: LoggedInUser,
        dishUuid: String,
        isPublished: Boolean
    ) {
        val dish = dishDBService.getDish(dishUuid) ?: throw ApiRequestException("Dish does not exist")

        val isUserPermitted = loggedInUser.hasPermissionToEditDish(dish)

        if (!isUserPermitted) throw ApiRequestException("User is not permitted to edit this dish")

        dishDBService.updateIsPublished(dish.id.value, isPublished)
    }

    suspend fun addDishPhoto(
        loggedInUser: LoggedInUser,
        dishUuid: String,
        file: File
    ) {
        val dish = dishDBService.getDish(dishUuid) ?: throw ApiRequestException("Dish does not exist")

        val isUserPermitted = loggedInUser.hasPermissionToEditDish(dish)

        if (!isUserPermitted) throw ApiRequestException("User is not permitted to edit this dish")

        if (!file.isImageValid()) throw ApiRequestException("Image is not valid")

        val imageReference = imageService.createImage(
            ownerUserId = loggedInUser.userId,
            imageFile = file,
            imageDescription = "",
            bucket = FileBuckets.SOULSAVOR_INTERNAL,
            objectPrefix = ObjectPrefix.DISH_PHOTO
        ) ?: throw ApiRequestException("Image could not be uploaded")

        dishDBService.addDishPicture(dish.id.value, imageReference.imageId)
    }

    // ===================================================================

    // This is broken
    // TODO Refactor to use DishSchema#getUserDishPermissions instead
    suspend fun LoggedInUser.hasPermissionToViewDish(dish: Dish): Boolean {
        val merchantGroupOfDish = dishDBService.merchantGroupOfDish(dish.id.value)

        // Verify if user is in group that this dish is in
        val userMerchantGroups = merchantGroupMemberDBService.getGroupsOfUser(this.userId)

        return userMerchantGroups.firstOrNull { it.merchantGroupUuid == merchantGroupOfDish.merchantGroupUuid } != null
    }

    suspend fun LoggedInUser.hasPermissionToEditDish(dish: Dish): Boolean = dish.userId.value == this.userId

    private suspend fun createDishResponse(
        dish: Dish,
        merchantGroupMember: MerchantGroupMember? = null
    ): DishDetailsResponse {
        val internalMerchantGroupMember = merchantGroupMember
            ?: merchantGroupMemberDBService.getMember(dish.merchantGroupMemberId.value)
            ?: throw ApiRequestException("Merchant group member does not exist")

        val dishImages = dishDBService.getDishPictureIds(dish.id.value)
            .mapNotNull { imageId ->
                val imageReference = imageService.getImage(imageId) ?: return@mapNotNull null
                DishPhotoResponse(
                    dishPhotoUuid = imageReference.imageUuid,
                    dishPhotoUrl = imageReference.imageUrl,
                    dishPhotoDescription = imageReference.imageDescription
                )
            }

        return DishDetailsResponse(
            merchantGroupMemberId = internalMerchantGroupMember.groupMembershipUuid.toString(),
            merchantGroupMemberName = internalMerchantGroupMember.merchantName,
            dishUuid = dish.dishUuid.toString(),
            dishName = dish.dishName,
            dishShortDescription = dish.dishShortDescription,
            dishDescription = dish.dishDescription,
            price = dish.price,
            spicinessLevel = dish.spicinessLevel,
            dishPhotos = dishImages,
            isPublished = dish.isPublished
        )
    }

}
