package com.garfiec.service.generativeai

import com.aallam.openai.api.LegacyOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.garfiec.util.gpt.OpenAIConstants
import com.garfiec.util.gpt.output
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class GptDishDescriptionPrompt(
    private val openAI: OpenAI
) {
    private fun createDishDescriptionPrompt(dishName: String, shortDescription: String): String = """
        Write a one paragraph description about this dish keep it to about 75 words. 
        
        Dish name:
        ###
        $dishName
        ###

    """.trimIndent()
    suspend fun runDishDescriptionCompletion(dishName: String, shortDescription: String) =
        CoroutineScope(Dispatchers.IO).async {
            openAI.chatCompletion(
                ChatCompletionRequest(
                    model = ModelId(OpenAIConstants.GPT_3_5_TURBO),
                    messages = listOf(
                        ChatMessage(
                            role = ChatRole.User,
                            content = createDishDescriptionPrompt(dishName, shortDescription)
                        )
                    )
                )
            ).output()
        }

    @OptIn(LegacyOpenAI::class)
    suspend fun runDishDescriptionInstrCompletion(dishName: String, shortDescription: String) =
        CoroutineScope(Dispatchers.IO).async {
            openAI.completion(
                CompletionRequest(
                    model = ModelId("gpt-3.5-turbo-instruct"),
                    prompt = createDishDescriptionPrompt(dishName, shortDescription),
                    maxTokens = 1000
                )
            ).output()
        }
}
