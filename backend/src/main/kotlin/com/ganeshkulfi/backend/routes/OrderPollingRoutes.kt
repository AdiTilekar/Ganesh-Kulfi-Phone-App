package com.ganeshkulfi.backend.routes

import com.ganeshkulfi.backend.data.dto.ApiResponse
import com.ganeshkulfi.backend.data.dto.ErrorResponse
import com.ganeshkulfi.backend.data.models.UserRole
import com.ganeshkulfi.backend.data.repository.OrderRepository
import com.ganeshkulfi.backend.data.repository.OrderTimelineRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Order Polling Routes
 * Allows retailers to poll for their order updates
 */
fun Route.orderPollingRoutes(
    orderRepository: OrderRepository,
    orderTimelineRepository: OrderTimelineRepository
) {
    
    authenticate("auth-jwt") {
        
        /**
         * Get recent order updates for retailer
         * GET /api/retailer/orders/updates?since={timestamp}
         */
        get("/api/retailer/orders/updates") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("userId", String::class)
                val userRole = principal?.getClaim("role", String::class)
                    ?.let { UserRole.valueOf(it) }
                
                if (userId == null || userRole != UserRole.RETAILER) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Only retailers can access this endpoint"))
                    return@get
                }
                
                // Get timestamp parameter (optional)
                val sinceParam = call.request.queryParameters["since"]
                val sinceTimestamp = sinceParam?.toLongOrNull() ?: 0L
                
                // Get retailer's orders
                val orders = orderRepository.findByRetailerId(userId)
                
                // Get updates for each order
                val updates = mutableListOf<OrderUpdateDTO>()
                
                for (order in orders) {
                    val timeline = orderTimelineRepository.getTimelineForOrder(order.id)
                    
                    // Filter timeline entries newer than 'since' timestamp
                    val recentEntries = timeline.filter { 
                        it.createdAt.toEpochMilli() > sinceTimestamp 
                    }
                    
                    if (recentEntries.isNotEmpty() || sinceTimestamp == 0L) {
                        updates.add(
                            OrderUpdateDTO(
                                orderId = order.id,
                                orderNumber = order.id.takeLast(8),
                                status = order.status.name,
                                paymentStatus = order.paymentStatus?.name ?: "UNPAID",
                                totalAmount = order.totalAmount,
                                recentTimeline = recentEntries.map { TimelineEntryDTO.from(it) },
                                lastUpdated = order.updatedAt.toEpochMilli()
                            )
                        )
                    }
                }
                
                call.respond(
                    HttpStatusCode.OK,
                    OrderUpdatesResponse(
                        success = true,
                        message = "Order updates retrieved successfully",
                        updates = updates,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(e.message ?: "Failed to retrieve updates")
                )
            }
        }
        
        /**
         * Check if there are any new updates
         * GET /api/retailer/orders/has-updates?since={timestamp}
         */
        get("/api/retailer/orders/has-updates") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("userId", String::class)
                val userRole = principal?.getClaim("role", String::class)
                    ?.let { UserRole.valueOf(it) }
                
                if (userId == null || userRole != UserRole.RETAILER) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Only retailers can access this endpoint"))
                    return@get
                }
                
                val sinceParam = call.request.queryParameters["since"]
                val sinceTimestamp = sinceParam?.toLongOrNull() ?: 0L
                
                val orders = orderRepository.findByRetailerId(userId)
                
                var hasUpdates = false
                for (order in orders) {
                    if (order.updatedAt.toEpochMilli() > sinceTimestamp) {
                        hasUpdates = true
                        break
                    }
                }
                
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        message = if (hasUpdates) "New updates available" else "No new updates",
                        data = mapOf(
                            "hasUpdates" to hasUpdates,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(e.message ?: "Failed to check updates")
                )
            }
        }
    }
}

@Serializable
data class OrderUpdateDTO(
    val orderId: String,
    val orderNumber: String,
    val status: String,
    val paymentStatus: String,
    val totalAmount: Double,
    val recentTimeline: List<TimelineEntryDTO>,
    val lastUpdated: Long
)

@Serializable
data class OrderUpdatesResponse(
    val success: Boolean,
    val message: String,
    val updates: List<OrderUpdateDTO>,
    val timestamp: Long
)

// Timeline DTO reused from FactoryOrderStatusRoutes.kt
// Uses com.ganeshkulfi.backend.routes.TimelineEntryDTO
