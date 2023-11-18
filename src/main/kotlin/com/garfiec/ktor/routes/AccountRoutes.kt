package com.garfiec.ktor.routes

import com.garfiec.di
import com.garfiec.util.ktor.performApiRequest
import com.garfiec.util.ktor.requireLoggedIn
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(
    val username: String,
    val password: String,
    val firstName: String,
    val lastName: String
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class SessionUuidResponse(
    val sessionUuid: String,
    val userId: String
)

@Serializable
data class LoggedInUserResponse(
    val userId: String,
    val firstName: String,
    val lastName: String
)

fun Application.accountRoutes() {
    routing {
        post("/account/register") {
            performApiRequest {
                val user = call.receive<CreateUserRequest>()
                di.accountService.createAccount(
                    user.username,
                    user.password,
                    user.firstName,
                    user.lastName
                )
            }
        }

        post("/account/login") {
            performApiRequest {
                val user = call.receive<LoginRequest>()
                di.accountService.login(user.username, user.password)
            }
        }

        // Get User
        get("/account/details") {
            requireLoggedIn {
                performApiRequest {
                    di.accountService.getLoggedInUserDetails(this)
                }
            }
        }

//        // Update user
//        put("/users/{id}") {
//            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
//            val user = call.receive<ExposedUser>()
//            userDBService.update(id, user)
//            call.respond(HttpStatusCode.OK)
//        }
//        // Delete user
//        delete("/users/{id}") {
//            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
//            userDBService.delete(id)
//            call.respond(HttpStatusCode.OK)
//        }
    }
}
