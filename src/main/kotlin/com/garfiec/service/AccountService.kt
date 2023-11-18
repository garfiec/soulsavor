package com.garfiec.service

import com.garfiec.db.schema.LoggedInUser
import com.garfiec.db.schema.SessionDBService
import com.garfiec.db.schema.User
import com.garfiec.ktor.routes.SessionUuidResponse
import com.garfiec.db.schema.UserDBService
import com.garfiec.ktor.routes.LoggedInUserResponse
import com.garfiec.util.exception.ApiRequestException
import java.util.*

class NotLoggedInException: ApiRequestException("User not logged in")

class AccountService(
    private val userDBService: UserDBService,
    private val sessionDBService: SessionDBService
) {

    /**
     * Directly handles request
     */
    suspend fun createAccount(username: String, password: String, firstName: String, lastName: String): SessionUuidResponse {
        val user = try {
            userDBService.createUser(username, password, firstName, lastName)
        } catch (e: Exception) {
            throw ApiRequestException(e.message.orEmpty())
        }

        return SessionUuidResponse(
            sessionUuid = sessionDBService.createSession(user.id.value).toString(),
            userId = user.userUuid.toString()
        )
    }

    /**
     * Directly handles request
     */
    suspend fun login(username: String, password: String): SessionUuidResponse {
        return try {
            val user = userDBService.getUser(username, password)

            SessionUuidResponse(
                sessionUuid = sessionDBService.createSession(user.id.value).toString(),
                userId = user.userUuid.toString()
            )
        } catch (e: Exception) {
            throw ApiRequestException(e.message.orEmpty())
        }
    }

    /**
     * Directly handles request
     */
    suspend fun getLoggedInUserDetails(loggedInUser: LoggedInUser): LoggedInUserResponse {
        return try {
            LoggedInUserResponse(
                userId = loggedInUser.userUuid.toString(),
                firstName = loggedInUser.firstName,
                lastName = loggedInUser.lastName
            )
        } catch (e: Exception) {
            throw ApiRequestException(e.message.orEmpty())
        }
    }

    /**
     * Internal use function
     */
    suspend fun getLoggedInUser(sessionUuid: String): User {
        return try {
            val userId = sessionDBService.getUserId(UUID.fromString(sessionUuid))
            userDBService.getUser(userId)
        } catch (e: Exception) {
            throw NotLoggedInException()
        }
    }
}
