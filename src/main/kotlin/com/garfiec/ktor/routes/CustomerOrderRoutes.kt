package com.garfiec.ktor.routes

import com.garfiec.di
import com.garfiec.service.PlaceOrderRequest
import com.garfiec.service.ValidateOrderRequest
import com.garfiec.util.ktor.performApiRequest
import com.garfiec.util.ktor.requireLoggedIn
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlin.time.Duration.Companion.days

fun Application.customerOrderRoutes() {
    routing {
        /**
         * Placed orders
         */
        get("/customer/order/browse") {
            requireLoggedIn {
                performApiRequest {
                    val duration = call.parameters["days"]?.toInt()?.days ?: 30.days
                    di.customerOrderService.getOrders(this, duration)
                }
            }
        }

        get("/customer/order/{{orderId}}/details") {
            requireLoggedIn {
                performApiRequest {
                    val orderId = call.parameters["orderId"]?: throw IllegalArgumentException("orderId is required")

                }
            }
        }

        get("/customer/order/validate") {
            requireLoggedIn {
                performApiRequest {
                    val validateOrderRequest = call.receive<ValidateOrderRequest>()
                    di.customerOrderService.validateOrder(this, validateOrderRequest)
                }
            }
        }

        post("/customer/order/create") {
            requireLoggedIn {
                performApiRequest {
                    val createOrderRequest = call.receive<PlaceOrderRequest>()
                    di.customerOrderService.placeOrder(this, createOrderRequest)
                }
            }
        }
    }
}