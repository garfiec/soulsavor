package com.garfiec.ktor.routes

import com.garfiec.util.ktor.performApiRequest
import com.garfiec.util.ktor.requireLoggedIn
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlin.time.Duration.Companion.days

data class OrderStatusUpdateRequest(
    val orderStatus: String
)

fun Application.merchantOrderRoutes() {
    routing {
        /**
         * Received orders
         */
        get("/merchant/order/browse") {
            requireLoggedIn {
                performApiRequest {
                    val duration = call.parameters["days"]?.toInt()?.days


                }
            }
        }

        post("/merchant/order/{orderId}/order-update") {
            requireLoggedIn {
                performApiRequest {
                    val orderId = call.parameters["orderId"]?: throw IllegalArgumentException("orderId is required")
                    val orderUpdateRequest = call.receive<OrderStatusUpdateRequest>()

                }
            }
        }
    }
}
