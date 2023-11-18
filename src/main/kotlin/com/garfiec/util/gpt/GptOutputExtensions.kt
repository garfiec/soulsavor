package com.garfiec.util.gpt


import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.completion.TextCompletion
import com.aallam.openai.api.image.ImageJSON
import java.io.File
import java.util.Base64


fun ChatCompletion.output(): String = choices.firstOrNull()?.message?.content.orEmpty()

fun TextCompletion.output(): String = choices.firstOrNull()?.text.orEmpty()

fun ImageJSON.toImageFile(fileName: String): File = File
    .createTempFile(fileName, ".png")
    .apply {
        outputStream()
            .use { fos ->
                fos.write(Base64.getDecoder().decode(b64JSON))
            }
    }
