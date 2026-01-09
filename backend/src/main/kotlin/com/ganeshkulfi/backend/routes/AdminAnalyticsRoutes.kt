package com.ganeshkulfi.backend.routes

import com.ganeshkulfi.backend.data.dto.*
import com.ganeshkulfi.backend.data.models.UserRole
import com.ganeshkulfi.backend.services.AnalyticsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Admin Analytics Routes
 * 
 * Admin-only routes for business analytics:
 * - GET /api/admin/analytics/orders/summary - Today's order summary
 * - GET /api/admin/analytics/top-products - Top selling products
 * - GET /api/admin/analytics/top-retailers - Top retailers by orders
 * - GET /api/admin/analytics/orders/weekly - Last 7 days order stats
 */
fun Route.adminAnalyticsRoutes(analyticsService: AnalyticsService) {
    
    authenticate("auth-jwt") {
        route("/api/admin/analytics") {
            
            /**
             * GET /api/admin/analytics/orders/summary
             * Get today's order summary statistics
             */
            get("/orders/summary") {
                try {
                    // Check admin role
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.payload?.getClaim("role")?.asString()
                    
                    if (role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Admin access required")
                        )
                        return@get
                    }
                    
                    // Get analytics summary
                    analyticsService.getOrdersSummary().fold(
                        onSuccess = { summary ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Analytics summary retrieved successfully",
                                    data = summary
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(error.message ?: "Failed to retrieve analytics")
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
             * GET /api/admin/analytics/top-products
             * Get top selling products by quantity
             */
            get("/top-products") {
                try {
                    // Check admin role
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.payload?.getClaim("role")?.asString()
                    
                    if (role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Admin access required")
                        )
                        return@get
                    }
                    
                    // Get limit parameter
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                    
                    // Get top products
                    analyticsService.getTopProducts(limit).fold(
                        onSuccess = { topProducts ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Top products retrieved successfully",
                                    data = mapOf(
                                        "products" to topProducts,
                                        "count" to topProducts.size
                                    )
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error.message ?: "Failed to retrieve top products")
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
             * GET /api/admin/analytics/top-retailers
             * Get top retailers by order count
             */
            get("/top-retailers") {
                try {
                    // Check admin role
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.payload?.getClaim("role")?.asString()
                    
                    if (role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Admin access required")
                        )
                        return@get
                    }
                    
                    // Get limit parameter
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                    
                    // Get top retailers
                    analyticsService.getTopRetailers(limit).fold(
                        onSuccess = { topRetailers ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Top retailers retrieved successfully",
                                    data = mapOf(
                                        "retailers" to topRetailers,
                                        "count" to topRetailers.size
                                    )
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error.message ?: "Failed to retrieve top retailers")
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
             * GET /api/admin/analytics/orders/weekly
             * Get order statistics for the last 7 days
             */
            get("/orders/weekly") {
                try {
                    // Check admin role
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.payload?.getClaim("role")?.asString()
                    
                    if (role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Admin access required")
                        )
                        return@get
                    }
                    
                    // Get weekly stats
                    analyticsService.getWeeklyOrderStats().fold(
                        onSuccess = { weeklyStats ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Weekly order statistics retrieved successfully",
                                    data = mapOf(
                                        "stats" to weeklyStats,
                                        "period" to "Last 7 days"
                                    )
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(error.message ?: "Failed to retrieve weekly statistics")
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
