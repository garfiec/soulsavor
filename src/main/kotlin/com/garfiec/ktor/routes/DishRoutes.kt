package com.garfiec.ktor.routes

import com.garfiec.di
import com.garfiec.service.CreateDishRequest
import com.garfiec.util.ktor.performApiRequest
import com.garfiec.util.ktor.requireLoggedIn
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import java.io.File
import java.util.UUID

fun Application.dishBrowseRoutes() {
    routing {
        // Create dish
        post("/dish/create") {
            requireLoggedIn {
                performApiRequest {
                    val addDishRequest = call.receive<CreateDishRequest>()
                    di.dishService.createDish(this, addDishRequest)
                }
            }
        }

        // Remove dish
        delete("/dish/{dishUuid}/remove") {
            requireLoggedIn {
                performApiRequest {
                    val dishUuid = call.parameters["dishUuid"] ?: throw IllegalArgumentException("dishUuid is required")
                    di.dishService.removeDish(this, dishUuid)
                }
            }
        }


        // Get Dish
        get("/dish/{dishUuid}/details") {
            requireLoggedIn {
                performApiRequest {
                    val dishUuid = call.parameters["dishUuid"] ?: throw IllegalArgumentException("dishUuid is required")
                    di.dishService.getDish(this, dishUuid)
                }
            }
        }

        // Update dish name
        patch("/dish/{dishUuid}/details/dish-name") {
            requireLoggedIn {
                performApiRequest {
                    val dishUuid = call.parameters["dishUuid"] ?: throw IllegalArgumentException("dishUuid is required")
                    val dishName = call.receive<String>()
                    di.dishService.updateDishName(this, dishUuid, dishName)
                }
            }
        }

        patch("/dish/{dishUuid}/details/dish-short-description") {
            requireLoggedIn {
                performApiRequest {
                    val dishUuid = call.parameters["dishUuid"] ?: throw IllegalArgumentException("dishUuid is required")
                    val dishShortDescription = call.receive<String>()
                    di.dishService.updateShortDescription(this, dishUuid, dishShortDescription)
                }
            }
        }

        patch("/dish/{dishUuid}/details/dish-description") {
            requireLoggedIn {
                performApiRequest {
                    val dishUuid = call.parameters["dishUuid"] ?: throw IllegalArgumentException("dishUuid is required")
                    val dishDescription = call.receive<String>()
                    di.dishService.updateDescription(this, dishUuid, dishDescription)
                }
            }
        }

        patch("/dish/{dishUuid}/details/dish-price") {
            requireLoggedIn {
                performApiRequest {
                    val dishUuid = call.parameters["dishUuid"] ?: throw IllegalArgumentException("dishUuid is required")
                    val dishPrice = call.receive<String>().toIntOrNull() ?: throw IllegalArgumentException("dishPrice must be an integer")
                    di.dishService.updatePrice(this, dishUuid, dishPrice)
                }
            }
        }

        patch("/dish/{dishUuid}/details/dish-spiciness") {
            requireLoggedIn {
                performApiRequest {
                    val dishUuid = call.parameters["dishUuid"] ?: throw IllegalArgumentException("dishUuid is required")
                    val dishSpiciness = call.receive<String>().toFloatOrNull() ?: throw IllegalArgumentException("dishSpiciness must be a float")
                    di.dishService.updateSpicinessLevel(this, dishUuid, dishSpiciness)
                }
            }
        }

        patch("/dish/{dishUuid}/details/is-published") {
            requireLoggedIn {
                performApiRequest {
                    val dishUuid = call.parameters["dishUuid"] ?: throw IllegalArgumentException("dishUuid is required")
                    val isPublished = call.receive<String>().toBooleanStrictOrNull() ?: throw IllegalArgumentException("isPublished must be a boolean")
                    di.dishService.updateIsPublished(this, dishUuid, isPublished)
                }
            }
        }

        // Upload dish photo
        post("/dish/{dishUuid}/details/photo-upload") {
            requireLoggedIn {
                performApiRequest {
                    val dishUuid = call.parameters["dishUuid"] ?: throw IllegalArgumentException("dishUuid is required")

                    val multipart = call.receiveMultipart()

                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                val fileUuid = UUID.randomUUID().toString()
                                val ext = File(part.originalFileName ?: fileUuid).extension
                                val tempFile = File.createTempFile(fileUuid, ".$ext")
                                part.streamProvider().use { input ->
                                    tempFile
                                        .outputStream()
                                        .buffered()
                                        .use { output -> input.copyTo(output) }
                                }
                                part.dispose()


                                di.dishService.addDishPhoto(this, dishUuid, tempFile)

                                tempFile.delete()
                            }

                            is PartData.BinaryChannelItem -> TODO()
                            is PartData.BinaryItem -> TODO()
                            is PartData.FormItem -> TODO()
                        }
                    }
                }
            }
        }

        // Generate dish description
        get("/dish/{dishUuid}/details/generator/description") {
            requireLoggedIn {
                performApiRequest {
                    val dishUuid = call.parameters["dishUuid"] ?: throw IllegalArgumentException("dishUuid is required")

                    val dish = di.dishService.getDish(this, dishUuid)
                    di.dishGenAiService.generateDishDescription(dishName = dish.dishName)
                }
            }
        }

        // Generate dish photo
        post("/dish/{dishUuid}/details/generator/generated-photo-submit") {
            requireLoggedIn {
                performApiRequest {
                    val dishUuid = call.parameters["dishUuid"] ?: throw IllegalArgumentException("dishUuid is required")

                    di.dishGenAiService.generateDishPhoto(this, dishUuid)
                }
            }
        }
    }
}
