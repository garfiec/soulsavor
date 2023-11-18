package com.garfiec.util.image

import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO

private val maxImageSizeInBytes = 15L * 1024L * 1024L  // 15 MB
private val allowedImageExtensions = listOf("jpg", "jpeg", "png")

data class ImageDimensions(val width: Int, val height: Int)
fun File.convertToJpg(fileName: String = UUID.randomUUID().toString(), imageDimensions: ImageDimensions? = null): File {
    val outputFile = File.createTempFile(fileName, ".jpg")
    val image = ImageIO.read(this)

    // TODO Resize image
    val resizedImage = if (imageDimensions != null) {
        image.resizeImage(imageDimensions.width, imageDimensions.height)
    } else {
        image
    }

    // The "jpg" string here tells ImageIO to use the JPG writer
    ImageIO.write(resizedImage, "jpg", outputFile)

    return outputFile
}

fun File.isImageValid(allowedExtensions: List<String> = allowedImageExtensions, maxSizeInBytes: Long = maxImageSizeInBytes): Boolean {
    // 1. Check file extension
    val fileExtension = extension.lowercase(Locale.getDefault())
    if (fileExtension !in allowedExtensions) return false

    // 2. Check file size
    if (length() > maxSizeInBytes) return false

    // 3. Inspect the content to see if it's a genuine image
    return try {
        val image = ImageIO.read(this)
        image != null
    } catch (e: Exception) {
        false
    }
}

fun BufferedImage.resizeImage(width: Int, height: Int): BufferedImage {
    val resizedImage = BufferedImage(width, height, type)
    val g = resizedImage.createGraphics()
    g.drawImage(this, 0, 0, width, height, null)
    g.dispose()
    return resizedImage
}
