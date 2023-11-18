package com.garfiec.util.ktor

import com.garfiec.db.schema.LoggedInUser
import com.garfiec.di
import com.garfiec.ktor.models.ErrorResponse
import com.garfiec.service.NotLoggedInException
import com.garfiec.util.exception.ApiRequestException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.util.*
import io.ktor.util.pipeline.*

/**
 * Performs actions for endpoint and responds accordingly
 */
suspend inline fun <reified T : Any> PipelineContext<*, ApplicationCall>.performApiRequest(action: () -> T) {
    try {
        call.respond(HttpStatusCode.OK, action())
    } catch (e: ApiRequestException) {
        val response = ErrorResponse(failureReason = e.displayText)
        call.respond(HttpStatusCode.BadRequest, response)
    } catch (e: BadRequestException) {
        var rootCause: Throwable? = e
        while (rootCause?.cause != null) {
            rootCause = rootCause.cause
        }

        when (rootCause) {
            null -> call.respond(HttpStatusCode.BadRequest, ErrorResponse(failureReason = e.message.orEmpty()))
            else -> call.respond(HttpStatusCode.BadRequest, ErrorResponse(failureReason = rootCause.message.orEmpty()))
        }
    } catch (e: Exception) {
        val response = ErrorResponse(failureReason = e.message.orEmpty())
        call.respond(HttpStatusCode.InternalServerError, response)
    }
}
/**
 * Requires a logged-in session; otherwise, respond with require login.
 * If used with `performApiRequest`, `requireLoggedIn` needs to be on top.
 */
suspend fun PipelineContext<*, ApplicationCall>.requireLoggedIn(action: suspend LoggedInUser.() -> Unit) {
    val sessionUuid = call.request.headers["sessionUuid"] ?: return call.respond(HttpStatusCode.Unauthorized)

    val user = try {
        di.accountService.getLoggedInUser(sessionUuid)
    } catch (e: NotLoggedInException) {
        val response = ErrorResponse(failureReason = e.displayText)
        return call.respond(HttpStatusCode.Unauthorized, response)
    } catch (e: Exception) {
        val response = ErrorResponse(failureReason = e.message.orEmpty())
        return call.respond(HttpStatusCode.InternalServerError, response)
    }

    action(LoggedInUser.fromUser(user))
}
