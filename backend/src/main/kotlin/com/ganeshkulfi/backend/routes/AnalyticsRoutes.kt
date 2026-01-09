package com.ganeshkulfi.backend.routes

import com.ganeshkulfi.backend.data.dto.ApiResponse
import com.ganeshkulfi.backend.data.dto.ErrorResponse
import com.ganeshkulfi.backend.data.models.UserRole
import com.ganeshkulfi.backend.services.AnalyticsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Analytics Routes
 * Admin-only analytics endpoints for dashboard
 */
fun Route.analyticsRoutes(analyticsService: AnalyticsService) {
    
    authenticate("auth-jwt") {
        route("/api/admin/analytics") {
            
            /**
             * GET /api/admin/analytics/dashboard
             * Get complete dashboard statistics
             */
            get("/dashboard") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userRole = principal?.getClaim("role", String::class)
                        ?.let { UserRole.valueOf(it) }
                    
                    if (userRole != UserRole.ADMIN) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Only ADMIN can access analytics")
                        )
                        return@get
                    }
                    
                    // Get all dashboard stats
                    val dailyOrders = analyticsService.getDailyOrderCount(7)
                    val dailySales = analyticsService.getDailySales(7)
                    val topProductsResult = analyticsService.getTopProducts(5)
                    val topRetailersResult = analyticsService.getTopRetailers(5)
                    
                    val dashboard = com.ganeshkulfi.backend.data.dto.analytics.DashboardResponse(
                        totalOrders = dailyOrders.days.sumOf { it.count },
                        totalRevenue = dailySales.days.sumOf { it.totalSales },
                        dailyOrders = dailyOrders,
                        dailySales = dailySales,
                        topProducts = topProductsResult.getOrNull(),
                        topRetailers = topRetailersResult.getOrNull()
                    )
                    
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            message = "Dashboard data retrieved successfully",
                            data = dashboard
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(e.message ?: "Failed to retrieve dashboard data")
                    )
                }
            }
            
            /**
             * Get daily order count for last 30 days
             * GET /api/admin/analytics/daily-orders
             */
            get("/daily-orders") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userRole = principal?.getClaim("role", String::class)
                        ?.let { UserRole.valueOf(it) }
                    
                    if (userRole != UserRole.ADMIN) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Only ADMIN can access analytics")
                        )
                        return@get
                    }
                    
                    val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 30
                    val response = analyticsService.getDailyOrderCount(days)
                    
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            message = "Daily order count retrieved successfully",
                            data = response
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(e.message ?: "Failed to retrieve daily order count")
                    )
                }
            }
            
            /**
             * Get daily sales for last 30 days
             * GET /api/admin/analytics/daily-sales
             */
            get("/daily-sales") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userRole = principal?.getClaim("role", String::class)
                        ?.let { UserRole.valueOf(it) }
                    
                    if (userRole != UserRole.ADMIN) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Only ADMIN can access analytics")
                        )
                        return@get
                    }
                    
                    val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 30
                    val response = analyticsService.getDailySales(days)
                    
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            message = "Daily sales retrieved successfully",
                            data = response
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(e.message ?: "Failed to retrieve daily sales")
                    )
                }
            }
            
            /**
             * Get pending orders count and list
             * GET /api/admin/analytics/pending-orders
             */
            get("/pending-orders") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userRole = principal?.getClaim("role", String::class)
                        ?.let { UserRole.valueOf(it) }
                    
                    if (userRole != UserRole.ADMIN) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Only ADMIN can access analytics")
                        )
                        return@get
                    }
                    
                    val response = analyticsService.getPendingOrders()
                    
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            message = "Pending orders retrieved successfully",
                            data = response
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(e.message ?: "Failed to retrieve pending orders")
                    )
                }
            }
            
            /**
             * Get low stock products
             * GET /api/admin/analytics/low-stock
             */
            get("/low-stock") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userRole = principal?.getClaim("role", String::class)
                        ?.let { UserRole.valueOf(it) }
                    
                    if (userRole != UserRole.ADMIN) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Only ADMIN can access analytics")
                        )
                        return@get
                    }
                    
                    val response = analyticsService.getLowStockProducts()
                    
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            message = "Low stock products retrieved successfully",
                            data = response
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(e.message ?: "Failed to retrieve low stock products")
                    )
                }
            }
        }
    }
}
