package com.garfiec.ktor.models

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val failureReason: String
)
