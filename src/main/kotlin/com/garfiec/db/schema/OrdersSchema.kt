package com.garfiec.db.schema

import com.garfiec.util.db.dbQuery
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration

typealias OrderUuid = String

@Serializable
data class FulfillmentDetails(
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zip: String? = null,
    val phone: String? = null
)

@Serializable
data class OrderItem(
    val dishUuid: String,
    val quantity: Int,
    val specialInstructions: String,
    val price: Int
)

enum class OrderStatus(val status: String) {
    PENDING("pending"),                     // 1. Order has been placed but not yet confirmed
    ACCEPTED("accepted"),                   // 2. Order has been accepted by merchant
    REJECTED("rejected"),                   // 2. Order has been rejected by merchant
    IN_PROGRESS("in_progress"),             // 3. Order is being prepared
    SHIPPED("shipped"),                     // 4. Order has been shipped (for delivery
    READY_FOR_PICKUP("ready_for_pickup"),   // 4. Order is ready for pickup
    DELIVERED("delivered"),                 // 5. Order has been delivered
    PICKED_UP("picked_up"),                 // 5. Order has been picked up
    CANCELLED("cancelled");                 // 6. Order has been cancelled by user

    override fun toString() = status
}

@Serializable
enum class FulfillmentScheduleType(val type: String) {
    ASAP("asap"),
    SCHEDULED("scheduled");

    override fun toString() = type
}

enum class FulfillmentMethod(val method: String) {
    DELIVERY("delivery"),
    PICKUP("pickup"),
    DINE_IN("dine_in");

    override fun toString() = method
}

class Order(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Order>(OrderDBService.Orders)

    var requestUuid by OrderDBService.Orders.requestUuid
    var orderUuid by OrderDBService.Orders.orderUuid
    var customerUserId by OrderDBService.Orders.customerUserId
    var merchantUserId by OrderDBService.Orders.merchantUserId
    var customerMemberId by OrderDBService.Orders.customerMemberId
    var merchantMemberId by OrderDBService.Orders.merchantMemberId
    var merchantGroupId by OrderDBService.Orders.merchantGroupId
    var specialInstructions by OrderDBService.Orders.specialInstructions
    var orderDate by OrderDBService.Orders.orderDate
    var orderStatus by OrderDBService.Orders.orderStatus
    var orderTotal by OrderDBService.Orders.orderTotal
    var fulfillmentScheduleType by OrderDBService.Orders.fulfillmentScheduleType
    var fulfillmentDate by OrderDBService.Orders.fulfillmentDate
    var fulfillmentMethod by OrderDBService.Orders.fulfillmentMethod
    var fulfillmentDetails by OrderDBService.Orders.fulfillmentDetails
    var items by OrderDBService.Orders.items
}

class OrderDBService(private val database: Database) {
    // Only allow ordering from a single merchant in any given group at a time. Currency is different across groups. Order status would be complicated if order contains multiple merchants.

    // Fields to add:
    // - Order total
    // - Owner? (Maybe not needed)
    object Orders : IntIdTable() {
        val requestUuid = uuid("request_uuid")
        val orderUuid = uuid("order_uuid").autoGenerate().uniqueIndex()
        val customerUserId = reference("customer_id", UserDBService.Users)
        val merchantUserId = reference("merchant_id", UserDBService.Users)
        val customerMemberId = reference("customer_member_id", MerchantGroupMemberDBService.MerchantGroupMembers)
        val merchantMemberId = reference("merchant_member_id", MerchantGroupMemberDBService.MerchantGroupMembers)
        val merchantGroupId = reference("merchant_group_id", MerchantGroupDBService.MerchantGroups)
        val specialInstructions = text("special_instructions").default("")
        val orderDate = datetime("order_date").defaultExpression(CurrentDateTime)
        val orderStatus = enumerationByName("order_status", 50, OrderStatus::class).default(OrderStatus.PENDING)
        val orderTotal = integer("order_total").default(0)
        val fulfillmentScheduleType = enumerationByName("fulfillment_schedule_type", 50, FulfillmentScheduleType::class).default(FulfillmentScheduleType.ASAP)
        val fulfillmentDate = datetime("fulfillment_date").nullable()
        val fulfillmentMethod = enumerationByName("fulfillment_method", 50, FulfillmentMethod::class)
        val fulfillmentDetails = jsonb("fulfillment_details",
            serialize = { fulfillmentDetails ->
                Json.encodeToString(FulfillmentDetails.serializer(), fulfillmentDetails)
            }, deserialize = { jsonStr ->
                Json.decodeFromString(FulfillmentDetails.serializer(), jsonStr)
            })
        val items = jsonb("items",
            serialize = { orderItems ->
                Json.encodeToString(ListSerializer(OrderItem.serializer()), orderItems)
            },
            deserialize = { jsonStr ->
                Json.decodeFromString(ListSerializer(OrderItem.serializer()), jsonStr)
            }
        )
    }

    init {
        transaction(database) {
            SchemaUtils.create(Orders)
        }
    }

    suspend fun createOrder(
        requestUuid: String,
        customerUserId: Int,
        merchantUserId: Int,
        customerMemberId: Int,
        merchantMemberId: Int,
        merchantGroupId: Int,
        specialInstructions: String = "",
        orderTotal: Int,
        fulfillmentScheduleType: FulfillmentScheduleType?,
        fulfillmentDate: LocalDateTime?,
        fulfillmentMethod: FulfillmentMethod,
        fulfillmentDetails: FulfillmentDetails,
        items: List<OrderItem> = emptyList()
    ): Order {
        return dbQuery(database) {
            val order = Order.new {
                this.requestUuid = UUID.fromString(requestUuid)
                this.customerUserId = EntityID(customerUserId, UserDBService.Users)
                this.merchantUserId = EntityID(merchantUserId, UserDBService.Users)
                this.customerMemberId = EntityID(customerMemberId, MerchantGroupMemberDBService.MerchantGroupMembers)
                this.merchantMemberId = EntityID(merchantMemberId, MerchantGroupMemberDBService.MerchantGroupMembers)
                this.merchantGroupId = EntityID(merchantGroupId, MerchantGroupDBService.MerchantGroups)
                this.specialInstructions = specialInstructions
                this.orderTotal = orderTotal
                this.fulfillmentScheduleType = fulfillmentScheduleType ?: FulfillmentScheduleType.ASAP
                this.fulfillmentDate = fulfillmentDate
                this.fulfillmentMethod = fulfillmentMethod
                this.fulfillmentDetails = fulfillmentDetails
                this.items = items
            }

            Order.find { Orders.id eq order.id }.singleOrNull() ?: throw Exception("Order not saved")
        }
    }

    suspend fun getAllOrdersOfCustomer(customerUserId: Int, duration: Duration): List<Order> {
        return dbQuery(database) {
            Order.find {
                (Orders.customerUserId eq EntityID(customerUserId, UserDBService.Users)) and
                        (Orders.orderDate greaterEq LocalDateTime.now().minusDays(duration.inWholeDays))
            }.toList()
        }
    }

    suspend fun getOrdersOfCustomerMember(customerMemberId: Int): List<Order> {
        return dbQuery(database) {
            Order.find { Orders.customerMemberId eq EntityID(customerMemberId, MerchantGroupMemberDBService.MerchantGroupMembers) }.toList()
        }
    }

    suspend fun getOrder(orderUuid: OrderUuid): Order {
        return dbQuery(database) {
            Order.find { Orders.orderUuid eq UUID.fromString(orderUuid) }.singleOrNull()
        } ?: throw IllegalArgumentException("Order not found")
    }

    suspend fun doesOrderRequestExist(requestUuid: String): Boolean {
        return dbQuery(database) {
            Order.find { Orders.requestUuid eq UUID.fromString(requestUuid) }.singleOrNull() != null
        }
    }

    /**
     * TODO Detect invalid transitions
     */
    suspend fun transitionOrderStatus(orderUuid: String, newStatus: OrderStatus) {
        dbQuery(database) {
            Orders.update({ Orders.orderUuid eq UUID.fromString(orderUuid) }) {
                it[orderStatus] = newStatus
            }
        }
    }

    suspend fun deleteOrder(orderUuid: String) {
        dbQuery(database) {
            Orders.deleteWhere { Orders.orderUuid eq UUID.fromString(orderUuid) }
        }
    }
}
