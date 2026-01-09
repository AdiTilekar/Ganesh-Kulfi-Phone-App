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
 * Retailer Order History Routes
 * 
 * Authenticated retailer routes:
 * - GET /api/retailer/orders/history - Get order history with filters and pagination
 * - GET /api/retailer/orders/history/:id - Get specific order details
 */
fun Route.retailerOrderRoutes(orderService: OrderService) {
    
    authenticate("auth-jwt") {
        route("/api/retailer/orders") {
            
            /**
             * GET /api/retailer/orders/history
             * Get retailer's order history with pagination and filtering
             */
            get("/history") {
                try {
                    // Get authenticated user
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
                    
                    // Verify retailer role
                    if (role != UserRole.RETAILER.name && role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Only retailers can access order history")
                        )
                        return@get
                    }
                    
                    // Extract query parameters
                    val status = call.request.queryParameters["status"]
                    val dateFrom = call.request.queryParameters["date_from"]
                    val dateTo = call.request.queryParameters["date_to"]
                    val productId = call.request.queryParameters["product_id"]
                    val minTotal = call.request.queryParameters["min_total"]?.toDoubleOrNull()
                    val maxTotal = call.request.queryParameters["max_total"]?.toDoubleOrNull()
                    val sortBy = call.request.queryParameters["sort_by"] ?: "date"
                    val order = call.request.queryParameters["order"] ?: "desc"
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: 20
                    
                    // Build filters
                    val filters = OrderFilters(
                        status = status,
                        dateFrom = dateFrom,
                        dateTo = dateTo,
                        productId = productId,
                        minTotal = minTotal,
                        maxTotal = maxTotal,
                        sortBy = sortBy,
                        order = order,
                        page = page,
                        pageSize = pageSize
                    )
                    
                    // Get order history
                    orderService.getRetailerOrderHistory(userId, filters).fold(
                        onSuccess = { paginatedResponse ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Order history retrieved successfully",
                                    data = paginatedResponse
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error.message ?: "Failed to retrieve order history")
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
             * GET /api/retailer/orders/history/:id
             * Get specific order details from history
             */
            get("/history/{id}") {
                try {
                    // Get authenticated user
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
                    
                    // Verify retailer role
                    if (role != UserRole.RETAILER.name && role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Only retailers can access order details")
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
                    
                    // Get order details
                    val isAdmin = role == UserRole.ADMIN.name
                    orderService.getOrderById(orderId, userId, isAdmin).fold(
                        onSuccess = { order ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Order retrieved successfully",
                                    data = order
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
            
            /**
             * PATCH /api/retailer/orders/:id/cancel
             * Day 10: Cancel order (PENDING only)
             */
            patch("/{orderId}/cancel") {
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
                    
                    if (role != UserRole.RETAILER.name && role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Only retailers can cancel their orders")
                        )
                        return@patch
                    }
                    
                    val orderId = call.parameters["orderId"]
                    if (orderId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Order ID is required")
                        )
                        return@patch
                    }
                    
                    val request = try {
                        call.receive<RetailerCancelRequest>()
                    } catch (e: Exception) {
                        RetailerCancelRequest(reason = null)
                    }
                    
                    orderService.cancelOrderByRetailer(orderId, userId, request.reason).fold(
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
        }
    }
}
