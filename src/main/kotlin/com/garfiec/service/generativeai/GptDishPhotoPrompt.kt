package com.garfiec.service.generativeai

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.image.ImageSize
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.garfiec.util.gpt.OpenAIConstants
import com.garfiec.util.gpt.output
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class GptDishPhotoPrompt(
    private val openAI: OpenAI
) {
    private fun createDishPhotoPrompt(dishName: String, shortDescription: String): String = """
        Raw photography, high detail images for this dish. 
        
        Dish:
        ###
        $dishName
        ###
    """.trimIndent()

    suspend fun runDishPhotoGeneration(dishName: String, shortDescription: String) =
        CoroutineScope(Dispatchers.IO).async {
            // TODO Waiting on proper dalle3 support from SDK
            openAI.imageJSON(
                ImageCreation(
                    prompt = createDishPhotoPrompt(dishName, shortDescription),
                    n = 1,
                    size = ImageSize.is1024x1024,
                    user = null
                )
            )
        }
}
