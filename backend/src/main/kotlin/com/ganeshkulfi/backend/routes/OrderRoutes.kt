package com.ganeshkulfi.backend.routes

import com.ganeshkulfi.backend.data.dto.*
import com.ganeshkulfi.backend.data.models.UserRole
import com.ganeshkulfi.backend.services.OrderService
import com.ganeshkulfi.backend.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Order Routes
 * 
 * Retailer endpoints:
 * - POST /api/orders - Create new order
 * - GET /api/orders/my - Get my orders
 * - GET /api/orders/{id} - Get specific order
 * 
 * Factory Owner endpoints:
 * - GET /api/factory/orders - Get all orders (with optional status filter)
 * - GET /api/factory/orders/{id} - Get specific order
 * - PATCH /api/factory/orders/{id}/status - Update order status
 */
fun Route.orderRoutes(orderService: OrderService, userService: UserService) {
    
    // All order routes require authentication
    authenticate("auth-jwt") {
        
        route("/api/orders") {
            
            /**
             * POST /api/orders
             * Create new order (Retailer only)
             */
            post {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class)
                    val email = principal?.getClaim("email", String::class)
                    val role = principal?.getClaim("role", String::class)
                    
                    if (userId == null || email == null) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ErrorResponse("Invalid token")
                        )
                        return@post
                    }
                    
                    // Only retailers can create orders
                    if (role != UserRole.RETAILER.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Only retailers can create orders")
                        )
                        return@post
                    }
                    
                    val request = call.receive<CreateOrderRequest>()
                    
                    // Get idempotency key from header (Day 8)
                    val idempotencyKey = call.request.headers["Idempotency-Key"]
                    
                    // Fetch retailer details from user table
                    val retailerUser = userService.getCurrentUser(userId).getOrNull()
                    val retailerName = retailerUser?.name ?: "Unknown Retailer"
                    val shopName = retailerUser?.shopName
                    
                    orderService.createOrder(
                        retailerId = userId,
                        retailerEmail = email,
                        retailerName = retailerName,
                        shopName = shopName,
                        request = request,
                        idempotencyKey = idempotencyKey
                    ).fold(
                        onSuccess = { order ->
                            call.respond(
                                HttpStatusCode.Created,
                                ApiResponse(
                                    success = true,
                                    message = "Order created successfully",
                                    data = order
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error.message ?: "Failed to create order")
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
             * GET /api/orders/my
             * Get my orders (Retailer)
             * Optional query params: ?limit=100 (default 200, max 500)
             */
            get("/my") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class)
                    val role = principal?.getClaim("role", String::class)
                    
                    if (userId == null) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ErrorResponse("Invalid token")
                        )
                        return@get
                    }
                    
                    // Only retailers can view their orders
                    if (role != UserRole.RETAILER.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Only retailers can view orders")
                        )
                        return@get
                    }
                    
                    orderService.getRetailerOrders(userId).fold(
                        onSuccess = { allOrders ->
                            // Apply basic limit to prevent unbounded responses
                            val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 200).coerceIn(1, 500)
                            val orders = allOrders.take(limit)
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Orders retrieved successfully",
                                    data = OrdersListResponse(
                                        orders = orders,
                                        total = orders.size
                                    )
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.InternalServerError,
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
             * GET /api/orders/{id}
             * Get specific order (Retailer can only see their own)
             */
            get("/{id}") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class)
                    val role = principal?.getClaim("role", String::class)
                    val orderId = call.parameters["id"]
                    
                    if (userId == null || orderId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Invalid request")
                        )
                        return@get
                    }
                    
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
        }
        
        // Factory Owner Routes
        route("/api/factory/orders") {
            
            /**
             * GET /api/factory/orders
             * Get all orders (Factory Owner only)
             * Query params: ?status=PENDING|CONFIRMED|REJECTED
             */
            get {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.getClaim("role", String::class)
                    
                    // Only admin/factory owner can view all orders
                    if (role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Admin access required")
                        )
                        return@get
                    }
                    
                    val statusFilter = call.request.queryParameters["status"]
                    
                    orderService.getAllOrders(statusFilter).fold(
                        onSuccess = { orders ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Orders retrieved successfully",
                                    data = OrdersListResponse(
                                        orders = orders,
                                        total = orders.size
                                    )
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.InternalServerError,
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
             * GET /api/factory/orders/{id}
             * Get specific order (Factory Owner)
             */
            get("/{id}") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class)
                    val role = principal?.getClaim("role", String::class)
                    val orderId = call.parameters["id"]
                    
                    if (userId == null || orderId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Invalid request")
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
                    
                    orderService.getOrderById(orderId, userId, true).fold(
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
             * PATCH /api/factory/orders/{id}/status
             * Update order status (Factory Owner only)
             */
            patch("/{id}/status") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class)
                    val role = principal?.getClaim("role", String::class)
                    val orderId = call.parameters["id"]
                    
                    if (userId == null || orderId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Invalid request")
                        )
                        return@patch
                    }
                    
                    // Only admin can update order status
                    if (role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Admin access required")
                        )
                        return@patch
                    }
                    
                    val request = call.receive<UpdateOrderStatusRequest>()
                    
                    orderService.updateOrderStatus(orderId, request, userId).fold(
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
        }
    }
}
