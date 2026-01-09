package com.ganeshkulfi.backend.routes

import com.ganeshkulfi.backend.data.dto.*
import com.ganeshkulfi.backend.data.models.UserRole
import com.ganeshkulfi.backend.services.OrderService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Admin Order Routes (Day 8)
 * 
 * Admin-only endpoints for order management:
 * - PATCH /api/admin/orders/:id/status - Update order status with message
 */
fun Route.adminOrderRoutes(orderService: OrderService) {
    
    authenticate("auth-jwt") {
        route("/api/admin/orders") {
            
            /**
             * PATCH /api/admin/orders/:id/status
             * Update order status (Admin only)
             * Supports: PENDING -> CONFIRMED or PENDING -> REJECTED
             */
            patch("/{id}/status") {
                try {
                    // Check admin role
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString()
                    val role = principal?.payload?.getClaim("role")?.asString()
                    
                    if (userId == null) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ErrorResponse("User ID not found in token")
                        )
                        return@patch
                    }
                    
                    if (role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Admin access required")
                        )
                        return@patch
                    }
                    
                    val orderId = call.parameters["id"] ?: run {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Order ID is required")
                        )
                        return@patch
                    }
                    
                    val request = call.receive<AdminStatusUpdateRequest>()
                    
                    // Validate status
                    if (request.status.uppercase() != "CONFIRMED" && request.status.uppercase() != "REJECTED") {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Status must be either CONFIRMED or REJECTED")
                        )
                        return@patch
                    }
                    
                    // Update order status
                    orderService.updateOrderStatusByAdmin(
                        orderId = orderId,
                        newStatus = request.status,
                        message = request.message,
                        adminId = userId
                    ).fold(
                        onSuccess = { order ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Order status updated successfully",
                                    data = order
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error.message ?: "Failed to update order status")
                            )
                        }
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Server error: ${e.message}")
                    )
                }
            }
            
            /**
             * PATCH /api/admin/orders/:id/cancel
             * Day 10: Admin order cancellation (any status)
             */
            patch("/{id}/cancel") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString()
                    val role = principal?.payload?.getClaim("role")?.asString()
                    
                    if (userId == null) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ErrorResponse("User ID not found in token")
                        )
                        return@patch
                    }
                    
                    if (role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Admin access required")
                        )
                        return@patch
                    }
                    
                    val orderId = call.parameters["id"] ?: run {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Order ID is required")
                        )
                        return@patch
                    }
                    
                    val request = call.receive<AdminCancelRequest>()
                    
                    orderService.cancelOrderByAdmin(orderId, userId, role, request.reason).fold(
                        onSuccess = { response ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Order cancelled successfully",
                                    data = response
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error.message ?: "Failed to cancel order")
                            )
                        }
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Server error: ${e.message}")
                    )
                }
            }
            
            /**
             * GET /api/admin/orders
             * Day 10: Admin order dashboard with filters
             */
            get {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString()
                    val role = principal?.payload?.getClaim("role")?.asString()
                    
                    if (userId == null) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ErrorResponse("User ID not found in token")
                        )
                        return@get
                    }
                    
                    if (role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Admin access required")
                        )
                        return@get
                    }
                    
                    val status = call.request.queryParameters["status"]
                    val retailerId = call.request.queryParameters["retailer_id"]
                    val dateFrom = call.request.queryParameters["date_from"]
                    val dateTo = call.request.queryParameters["date_to"]
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: 50
                    
                    orderService.getOrdersForAdminDashboard(
                        status = status,
                        retailerId = retailerId,
                        dateFrom = dateFrom,
                        dateTo = dateTo,
                        page = page,
                        pageSize = pageSize
                    ).fold(
                        onSuccess = { dashboardData ->
                            call.respond(
                                HttpStatusCode.OK,
                                AdminDashboardApiResponse(
                                    success = true,
                                    message = "Orders retrieved successfully",
                                    data = dashboardData
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error.message ?: "Failed to retrieve orders")
                            )
                        }
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Server error: ${e.message}")
                    )
                }
            }
            
            /**
             * GET /api/admin/orders/:id
             * Day 10: Get order with full details and status history
             */
            get("/{id}") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString()
                    val role = principal?.payload?.getClaim("role")?.asString()
                    
                    if (userId == null) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ErrorResponse("User ID not found in token")
                        )
                        return@get
                    }
                    
                    if (role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Admin access required")
                        )
                        return@get
                    }
                    
                    val orderId = call.parameters["id"] ?: run {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Order ID is required")
                        )
                        return@get
                    }
                    
                    orderService.getOrderWithFullHistory(orderId).fold(
                        onSuccess = { orderDetail ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Order details retrieved successfully",
                                    data = orderDetail
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.NotFound,
                                ErrorResponse(error.message ?: "Order not found")
                            )
                        }
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Server error: ${e.message}")
                    )
                }
            }
        }
    }
}
