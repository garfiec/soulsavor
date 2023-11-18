package com.garfiec.service

import com.aallam.openai.client.OpenAI
import com.garfiec.db.schema.*
import com.garfiec.repository.filestore.FileBuckets
import com.garfiec.repository.filestore.ObjectPrefix
import com.garfiec.service.generativeai.GptDishDescriptionPrompt
import com.garfiec.service.generativeai.GptDishPhotoPrompt
import com.garfiec.util.exception.ApiRequestException
import com.garfiec.util.gpt.toImageFile
import com.garfiec.util.image.convertToJpg
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class GenerateDishDescriptionResponse(
    val descriptionCandidates: List<String>
)

class DishGenAiService(
    openAI: OpenAI,
    private val dishDBService: DishDBService,
    private val merchantGroupMemberDBService: MerchantGroupMemberDBService,
    private val imageService: ImageService
) {
    private val gptDishDescriptionPrompt = GptDishDescriptionPrompt(openAI)
    private val gptDishPhotoPrompt = GptDishPhotoPrompt(openAI)
    suspend fun generateDishDescription(count: Int = 3, dishName: String): GenerateDishDescriptionResponse {
        val descriptions = (0 until count)
            .map {
                CoroutineScope(Dispatchers.IO).async {
                    gptDishDescriptionPrompt.runDishDescriptionCompletion(
                        dishName = dishName,
                        shortDescription = ""
                    ).await().trim()
                }
            }.awaitAll()
        return GenerateDishDescriptionResponse(
            descriptionCandidates = descriptions
        )
    }

    /**
     * No good way to generate image, store, then let user select yet. We'll just directly
     * apply it to the dish uuid for now.
     */
    suspend fun generateDishPhoto(
        loggedInUser: LoggedInUser,
        dishUuid: String,
    ) {
        val dish = dishDBService.getDish(dishUuid) ?: throw ApiRequestException("Dish does not exist")

        val isUserPermitted = dishDBService.getUserDishPermissions(userId = loggedInUser.userId, dishId = dish.id.value).hasEditPermissions()

        if (!isUserPermitted) throw ApiRequestException("User is not permitted to edit this dish")

        val photoFile = gptDishPhotoPrompt
            .runDishPhotoGeneration(dish.dishName, dish.dishShortDescription)
            .await()
            .firstOrNull()
            ?.toImageFile("${UUID.randomUUID()}.png")
            ?.convertToJpg()
            ?: throw ApiRequestException("Failed to generate photo")


        val imageReference = imageService.createImage(
            ownerUserId = loggedInUser.userId,
            imageFile = photoFile,
            imageDescription = "",
            bucket = FileBuckets.SOULSAVOR_INTERNAL,
            objectPrefix = ObjectPrefix.DISH_PHOTO
        ) ?: throw ApiRequestException("Image could not be uploaded")

        dishDBService.addDishPicture(dish.id.value, imageReference.imageId)
    }
}
