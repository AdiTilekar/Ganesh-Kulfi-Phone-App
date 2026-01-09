package com.ganeshkulfi.backend.routes

import com.ganeshkulfi.backend.data.dto.ApiResponse
import com.ganeshkulfi.backend.data.dto.ErrorResponse
import com.ganeshkulfi.backend.data.models.OrderStatus
import com.ganeshkulfi.backend.data.models.UserRole
import com.ganeshkulfi.backend.data.repository.OrderRepository
import com.ganeshkulfi.backend.data.repository.OrderTimelineRepository
import com.ganeshkulfi.backend.data.repository.ProductRepository
import com.ganeshkulfi.backend.data.repository.UserRepository
import com.ganeshkulfi.backend.services.NotificationService
import com.ganeshkulfi.backend.services.OrderService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Day 11: Factory Order Status Update Routes
 * Admin/Factory owner can update order status with automatic notifications
 * Auto-reduces stock when order is confirmed
 */
fun Route.factoryOrderStatusRoutes(
    orderService: OrderService,
    orderRepository: OrderRepository,
    orderTimelineRepository: OrderTimelineRepository,
    userRepository: UserRepository,
    notificationService: NotificationService,
    productRepository: ProductRepository
) {
    
    authenticate("auth-jwt") {
        route("/api/orders/{orderId}") {
            
            /**
             * Confirm Order - Auto reduces stock
             * POST /api/orders/{orderId}/confirm
             */
            post("/confirm") {
                handleOrderStatusUpdate(
                    call = call,
                    orderId = call.parameters["orderId"] ?: "",
                    newStatus = "CONFIRMED",
                    requireRole = UserRole.ADMIN,
                    orderRepository = orderRepository,
                    orderTimelineRepository = orderTimelineRepository,
                    userRepository = userRepository,
                    notificationService = notificationService,
                    productRepository = productRepository
                )
            }
            
            /**
             * Mark Order as Packed
             * POST /api/orders/{orderId}/pack
             */
            post("/pack") {
                handleOrderStatusUpdate(
                    call = call,
                    orderId = call.parameters["orderId"] ?: "",
                    newStatus = "PACKED",
                    requireRole = UserRole.ADMIN,
                    orderRepository = orderRepository,
                    orderTimelineRepository = orderTimelineRepository,
                    userRepository = userRepository,
                    notificationService = notificationService,
                    productRepository = productRepository
                )
            }
            
            /**
             * Mark Order as Out for Delivery
             * POST /api/orders/{orderId}/out-for-delivery
             */
            post("/out-for-delivery") {
                handleOrderStatusUpdate(
                    call = call,
                    orderId = call.parameters["orderId"] ?: "",
                    newStatus = "OUT_FOR_DELIVERY",
                    requireRole = UserRole.ADMIN,
                    orderRepository = orderRepository,
                    orderTimelineRepository = orderTimelineRepository,
                    userRepository = userRepository,
                    notificationService = notificationService,
                    productRepository = productRepository
                )
            }
            
            /**
             * Mark Order as Delivered
             * POST /api/orders/{orderId}/deliver
             */
            post("/deliver") {
                handleOrderStatusUpdate(
                    call = call,
                    orderId = call.parameters["orderId"] ?: "",
                    newStatus = "DELIVERED",
                    requireRole = UserRole.ADMIN,
                    orderRepository = orderRepository,
                    orderTimelineRepository = orderTimelineRepository,
                    userRepository = userRepository,
                    notificationService = notificationService,
                    productRepository = productRepository
                )
            }
            
            /**
             * Get Order Timeline
             * GET /api/orders/{orderId}/timeline
             */
            get("/timeline") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class)
                    val userRole = principal?.getClaim("role", String::class)
                        ?.let { UserRole.valueOf(it) }
                    
                    if (userId == null || userRole == null) {
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
                        return@get
                    }
                    
                    val orderId = call.parameters["orderId"] ?: ""
                    
                    // Verify order exists and user has permission
                    val order = orderRepository.findById(orderId)
                    if (order == null) {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("Order not found"))
                        return@get
                    }
                    
                    // Check permission: Admin can see all, Retailer can see their own
                    if (userRole != UserRole.ADMIN && order.retailerId != userId) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied"))
                        return@get
                    }
                    
                    val timeline = orderTimelineRepository.getTimelineForOrder(orderId)
                    
                    call.respond(
                        HttpStatusCode.OK,
                        OrderTimelineResponse(
                            success = true,
                            message = "Timeline retrieved successfully",
                            timeline = timeline.map { TimelineEntryDTO.from(it) }
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(e.message ?: "Failed to retrieve timeline")
                    )
                }
            }
        }
    }
}

/**
 * Handle order status update with notification and auto stock reduction
 */
private suspend fun handleOrderStatusUpdate(
    call: ApplicationCall,
    orderId: String,
    newStatus: String,
    requireRole: UserRole,
    orderRepository: OrderRepository,
    orderTimelineRepository: OrderTimelineRepository,
    userRepository: UserRepository,
    notificationService: NotificationService,
    productRepository: ProductRepository
) {
    try {
        val principal = call.principal<JWTPrincipal>()
        val userId = principal?.getClaim("userId", String::class)
        val userRole = principal?.getClaim("role", String::class)
            ?.let { UserRole.valueOf(it) }
        
        if (userId == null || userRole == null) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
            return
        }
        
        // Check role permission
        if (userRole != requireRole) {
            call.respond(
                HttpStatusCode.Forbidden,
                ErrorResponse("Only ${requireRole.name} can perform this action")
            )
            return
        }
        
        // Get order
        val order = orderRepository.findById(orderId)
        if (order == null) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("Order not found"))
            return
        }
        
        // Update order status
        val orderStatus = try {
            OrderStatus.valueOf(newStatus)
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid order status: $newStatus"))
            return
        }
        
        // Auto-reduce stock when order is CONFIRMED (first confirmation only)
        if (newStatus == "CONFIRMED" && order.status != OrderStatus.CONFIRMED) {
            val orderItems = orderRepository.getOrderItems(orderId)
            orderItems.forEach { item ->
                try {
                    val product = productRepository.findById(item.productId)
                    if (product != null) {
                        val newStock = (product.stockQuantity - item.quantity).coerceAtLeast(0)
                        productRepository.updateProductStock(item.productId, newStock)
                    }
                } catch (e: Exception) {
                }
            }
        }
        
        val updateResult = orderRepository.updateStatus(orderId, orderStatus, userId)
        if (updateResult == null) {
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Failed to update order"))
            return
        }
        
        // Create timeline entry
        val message = when (newStatus) {
            "CONFIRMED" -> "Order has been confirmed by factory"
            "PACKED" -> "Order has been packed and ready for dispatch"
            "OUT_FOR_DELIVERY" -> "Order is out for delivery"
            "DELIVERED" -> "Order has been delivered successfully"
            else -> "Order status updated to $newStatus"
        }
        
        orderTimelineRepository.create(
            orderId = orderId,
            status = newStatus,
            message = message,
            createdBy = userId,
            createdByRole = userRole.name
        )
        
        // Log notification event (Android app will poll for updates)
        notificationService.logOrderStatusNotification(
            userId = order.retailerId.toString(),
            orderId = orderId,
            orderNumber = orderId.takeLast(8),
            newStatus = newStatus
        )
        
        call.respond(
            HttpStatusCode.OK,
            ApiResponse<Unit>(
                success = true,
                message = "Order status updated to $newStatus successfully",
                data = null
            )
        )
    } catch (e: Exception) {
        call.respond(
            HttpStatusCode.InternalServerError,
            ErrorResponse(e.message ?: "Failed to update order status")
        )
    }
}

@Serializable
data class OrderTimelineResponse(
    val success: Boolean,
    val message: String,
    val timeline: List<TimelineEntryDTO>
)

@Serializable
data class TimelineEntryDTO(
    val id: String,
    val orderId: String,
    val status: String,
    val message: String?,
    val createdBy: String?,
    val createdByRole: String?,
    val notificationSent: Boolean,
    val createdAt: String
) {
    companion object {
        fun from(timeline: com.ganeshkulfi.backend.data.models.OrderTimeline): TimelineEntryDTO {
            return TimelineEntryDTO(
                id = timeline.id,
                orderId = timeline.orderId,
                status = timeline.status,
                message = timeline.message,
                createdBy = timeline.createdBy,
                createdByRole = timeline.createdByRole,
                notificationSent = timeline.notificationSent,
                createdAt = timeline.createdAt.toString()
            )
        }
    }
}
