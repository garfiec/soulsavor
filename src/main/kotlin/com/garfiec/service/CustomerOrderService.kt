package com.garfiec.service

import com.garfiec.db.schema.*
import com.garfiec.util.exception.ApiRequestException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@Serializable
data class OrderRequestData(
    @SerialName("merchantId")
    val merchantUuid: String,
    val specialInstructions: String,
    val fulfillmentScheduleType: String = FulfillmentScheduleType.ASAP.name,
    val fulfillmentDate: String? = null,
    val fulfillmentMethod: String = FulfillmentMethod.DINE_IN.name, // "delivery" or "pickup" or "dine_in"
    val fulfillmentDetails: FulfillmentDetails = FulfillmentDetails(
        address = null,
        city = null,
        state = null,
        zip = null,
        phone = null
    ),
    val items: List<OrderItemRequest>
)

@Serializable
data class OrderItemRequest(
    @SerialName("dishId")
    val dishUuid: String,
    val quantity: Int,
    val specialInstructions: String
)

internal data class InternalValidatedOrder(
    val merchantMember: MerchantGroupMember,
    val customerMember: MerchantGroupMember,
    val specialInstructions: String,
    val fulfillmentScheduleType: FulfillmentScheduleType,
    val fulfillmentDate: LocalDateTime?,
    val fulfillmentMethod: FulfillmentMethod,
    val fulfillmentDetails: FulfillmentDetails,
    val items: List<InternalValidatedOrderItem>,
    val orderTotal: Int
)

internal data class InternalValidatedOrderItem(
    val dish: Dish,
    val quantity: Int,
    val specialInstructions: String,
    val price: Int
)

/**
 * Properties from a placed order
 */
internal data class InternalOrderExtras(
    val order: Order,
    val orderStatus: OrderStatus,
    val orderDate: LocalDateTime
)

@Serializable
data class OrderResponseData(
    // Available for validated orders
    @SerialName("merchantId")
    val merchantUuid: String, // Merchant UUID
    val specialInstructions: String,
    val fulfillmentScheduleType: String,
    val fulfillmentDate: String?,
    val fulfillmentMethod: String,
    val fulfillmentDetails: FulfillmentDetails? = FulfillmentDetails(
        address = null,
        city = null,
        state = null,
        zip = null,
        phone = null,
    ),
    val items: List<OrderItem>,

    // Available for created orders
    val orderId: String? = null,
    val orderDate: String? = null,
    val orderStatus: String? = null
)

@Serializable
data class ValidateOrderRequest(
    val order: OrderRequestData
)

@Serializable
data class PlaceOrderRequest(
    val requestUuid: String,
    val order: OrderRequestData
)

class CustomerOrderService(
    private val orderDBService: OrderDBService,
    private val dishDBService: DishDBService,
    private val merchantGroupMemberDBService: MerchantGroupMemberDBService
) {
    suspend fun getOrders(loggedInUser: LoggedInUser, duration: Duration = 30.days): List<OrderResponseData> {
        // Verify that the user exists


        // Verify that the user has access to the order
        return orderDBService.getAllOrdersOfCustomer(loggedInUser.userId, duration)
            .map {  order ->
            val merchant = merchantGroupMemberDBService.getMember(order.merchantUserId.value, order.merchantGroupId.value)
                ?: throw Exception("Merchant does not exist")
            OrderResponseData(
                merchantUuid = merchant.groupMembershipUuid.toString(),
                specialInstructions = order.specialInstructions,
                fulfillmentScheduleType = order.fulfillmentScheduleType.type.lowercase(),
                fulfillmentDate = order.fulfillmentDate?.format(DateTimeFormatter.ISO_DATE_TIME),
                fulfillmentMethod = order.fulfillmentMethod.name.lowercase(),
                fulfillmentDetails = order.fulfillmentDetails,
                items = order.items.map { orderItem ->
                    OrderItem(
                        dishUuid = orderItem.dishUuid,
                        quantity = orderItem.quantity,
                        specialInstructions = orderItem.specialInstructions,
                        price = orderItem.price
                    )
                },
                orderId = order.orderUuid.toString(),
                orderDate = order.orderDate.format(DateTimeFormatter.ISO_DATE_TIME),
                orderStatus = order.orderStatus.status.lowercase()

            )
        }

    }

    suspend fun getOrder(orderId: String) {
        // Verify that the order exists

        // Verify that the user has access to the order

    }

    suspend fun validateOrder(loggedInUser: LoggedInUser, validateOrderRequest: ValidateOrderRequest): OrderResponseData {
        val validatedOrder = createValidatedOrder(loggedInUser, validateOrderRequest.order)

        // Return cost estimate if order is valid
        return createOrderResponse(validatedOrder)
    }

    suspend fun placeOrder(loggedInUser: LoggedInUser, placeOrderRequest: PlaceOrderRequest): OrderResponseData {
        // Verify that the request Uuid doesn't already exist for that user
        orderDBService.doesOrderRequestExist(placeOrderRequest.requestUuid)

        // Run standard verification
        val validatedOrder = createValidatedOrder(loggedInUser, placeOrderRequest.order)

        // Place order. Retain info like order id to pass back to user.
        val order = orderDBService.createOrder(
            requestUuid = placeOrderRequest.requestUuid,
            customerUserId = validatedOrder.customerMember.user.value,
            merchantUserId = validatedOrder.merchantMember.user.value,
            customerMemberId = validatedOrder.customerMember.id.value,
            merchantMemberId = validatedOrder.merchantMember.id.value,
            merchantGroupId = validatedOrder.merchantMember.group.value,
            specialInstructions = validatedOrder.specialInstructions,
            orderTotal = validatedOrder.orderTotal,
            fulfillmentScheduleType = validatedOrder.fulfillmentScheduleType,
            fulfillmentDate = validatedOrder.fulfillmentDate,
            fulfillmentMethod = validatedOrder.fulfillmentMethod,
            fulfillmentDetails = validatedOrder.fulfillmentDetails,
            items = validatedOrder.items.map { validatedOrderItem ->
                OrderItem(
                    dishUuid = validatedOrderItem.dish.dishUuid.toString(),
                    quantity = validatedOrderItem.quantity,
                    specialInstructions = validatedOrderItem.specialInstructions,
                    price = validatedOrderItem.price
                )
            }
        )

        val orderExtras = InternalOrderExtras(
            order = order,
            orderStatus = order.orderStatus,
            orderDate = order.orderDate
        )

        return createOrderResponse(validatedOrder, orderExtras)
    }

    /**
     * Creates a validated order from an order request or throws an exception if the order request is invalid.
     * TODO run certain DB queries asynchronously
     */
    private suspend fun createValidatedOrder(loggedInUser: LoggedInUser, orderRequest: OrderRequestData): InternalValidatedOrder {
        // Verify that the merchant exists
        val merchantMember = merchantGroupMemberDBService.getMember(orderRequest.merchantUuid)
            ?: throw ApiRequestException("Merchant does not exist")
        val customerMember = merchantGroupMemberDBService.getMember(loggedInUser.userId, merchantMember.group.value)
            ?: throw ApiRequestException("User is not a member of the merchant's group")

        // Validate general inputs like fulfillment date, fulfillment method, etc.
        val fulfillmentScheduleType = FulfillmentScheduleType.valueOf(
            orderRequest.fulfillmentScheduleType.uppercase(Locale.getDefault())
        )

        var fulfillmentDate: LocalDateTime? = null
        when (fulfillmentScheduleType) {
            FulfillmentScheduleType.ASAP -> {
                // Verify that the date is null
                if (orderRequest.fulfillmentDate != null) {
                    throw ApiRequestException("Fulfillment date must be null for ASAP orders")
                }
            }

            FulfillmentScheduleType.SCHEDULED -> {
                // Verify that the date is not null
                if (orderRequest.fulfillmentDate == null) {
                    throw ApiRequestException("Fulfillment date must not be null for scheduled orders")
                }

                // Verify that the date is in the future
                fulfillmentDate = LocalDateTime.parse(
                    orderRequest.fulfillmentDate,
                    DateTimeFormatter.ISO_DATE_TIME
                )
                val isFutureDate = fulfillmentDate.isAfter(LocalDateTime.now())
                if (!isFutureDate) {
                    throw ApiRequestException("Fulfillment date must be in the future")
                }
            }
        }

        // Verify that the fulfillment method is valid
        val fulfillmentMethod = try {
            FulfillmentMethod.valueOf(
                orderRequest.fulfillmentMethod.uppercase(Locale.getDefault())
            )
        } catch (e: Exception) {
            throw ApiRequestException("Invalid fulfillment method")
        }

        // Verify that all dishes exist
        val validatedDishItems = orderRequest.items.map { orderItemRequest ->
            val dish = dishDBService.getDish(orderItemRequest.dishUuid)
                ?: throw ApiRequestException("Dish ${orderItemRequest.dishUuid} does not exist")
            if (orderItemRequest.quantity < 0) throw ApiRequestException("Dish ${orderItemRequest.dishUuid} has invalid quantity. Quantities must be > 0.")
            InternalValidatedOrderItem(
                dish = dish,
                quantity = orderItemRequest.quantity,
                specialInstructions = orderItemRequest.specialInstructions,
                price = dish.price
            )
        }

        // Verify that all dishes are from a single merchant
        val areAllOrdersFromSameMerchant = validatedDishItems.all { dishItem -> dishItem.dish.merchantGroupMemberId == merchantMember.id }
        if (!areAllOrdersFromSameMerchant) throw ApiRequestException("All dishes must be from the same merchant")

        // Verify that user has the funds to execute the order
        val totalCost = validatedDishItems.sumOf { dishItem -> dishItem.dish.price * dishItem.quantity }
        if (totalCost > customerMember.credits) throw ApiRequestException("Insufficient funds")

        return InternalValidatedOrder(
            merchantMember = merchantMember,
            customerMember = customerMember,
            specialInstructions = orderRequest.specialInstructions,
            fulfillmentScheduleType = fulfillmentScheduleType,
            fulfillmentDate = fulfillmentDate,
            fulfillmentMethod = fulfillmentMethod,
            fulfillmentDetails = orderRequest.fulfillmentDetails,
            items = validatedDishItems,
            orderTotal = totalCost
        )
    }

    private fun createOrderResponse(
        validatedOrder: InternalValidatedOrder,
        internalOrderExtras: InternalOrderExtras? = null
    ): OrderResponseData {
        return OrderResponseData(
            merchantUuid = validatedOrder.merchantMember.groupMembershipUuid.toString(),
            specialInstructions = validatedOrder.specialInstructions,
            fulfillmentDate = validatedOrder.fulfillmentDate?.format(DateTimeFormatter.ISO_DATE_TIME),
            fulfillmentMethod = validatedOrder.fulfillmentMethod.name.lowercase(),
            fulfillmentDetails = validatedOrder.fulfillmentDetails,
            fulfillmentScheduleType = validatedOrder.fulfillmentScheduleType.type.lowercase(),
            items = validatedOrder.items.map { validatedOrderItem ->
                OrderItem(
                    dishUuid = validatedOrderItem.dish.dishUuid.toString(),
                    quantity = validatedOrderItem.quantity,
                    specialInstructions = validatedOrderItem.specialInstructions,
                    price = validatedOrderItem.price
                )
            },
            orderId = internalOrderExtras?.order?.id?.value?.toString(),
            orderDate = internalOrderExtras?.orderDate?.format(DateTimeFormatter.ISO_DATE_TIME),
            orderStatus = internalOrderExtras?.orderStatus?.status?.lowercase(),
        )
    }
}
