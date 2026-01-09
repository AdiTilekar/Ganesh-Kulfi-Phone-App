package com.ganeshkulfi.backend.routes

import com.ganeshkulfi.backend.data.dto.*
import com.ganeshkulfi.backend.data.models.UserRole
import com.ganeshkulfi.backend.services.InventoryService
import com.ganeshkulfi.backend.services.ProductService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Factory Owner Routes
 * Endpoints for inventory management and product CRUD
 */
fun Route.factoryRoutes(
    productService: ProductService,
    inventoryService: InventoryService
) {
    route("/factory") {
        authenticate("auth-jwt") {
            
            // ========== Product Management ==========
            
            /**
             * GET /factory/products
             * Get all products with inventory details
             */
            get("/products") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val role = UserRole.valueOf(principal!!.payload.getClaim("role").asString())
                    
                    productService.getAllProductsWithInventory(role).fold(
                        onSuccess = { products ->
                            call.respond(HttpStatusCode.OK, products)
                        },
                        onFailure = { error ->
                            when (error) {
                                is SecurityException -> call.respond(HttpStatusCode.Forbidden, mapOf("error" to error.message))
                                else -> call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to fetch products"))
                            }
                        }
                    )
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error"))
                }
            }
            
            /**
             * POST /factory/products
             * Create a new product
             */
            post("/products") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val role = UserRole.valueOf(principal!!.payload.getClaim("role").asString())
                    val request = call.receive<CreateProductRequest>()
                    
                    productService.createProductFactory(request, role).fold(
                        onSuccess = { product ->
                            call.respond(HttpStatusCode.Created, product)
                        },
                        onFailure = { error ->
                            when (error) {
                                is SecurityException -> call.respond(HttpStatusCode.Forbidden, mapOf("error" to error.message))
                                is IllegalArgumentException -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to error.message))
                                else -> call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create product"))
                            }
                        }
                    )
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body"))
                }
            }
            
            /**
             * PATCH /factory/products/:id
             * Update product details
             */
            patch("/products/{id}") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val role = UserRole.valueOf(principal!!.payload.getClaim("role").asString())
                    val productId = call.parameters["id"] ?: return@patch call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Product ID is required")
                    )
                    val request = call.receive<UpdateProductRequest>()
                    
                    productService.updateProductFactory(productId, request, role).fold(
                        onSuccess = { product ->
                            call.respond(HttpStatusCode.OK, product)
                        },
                        onFailure = { error ->
                            when (error) {
                                is SecurityException -> call.respond(HttpStatusCode.Forbidden, mapOf("error" to error.message))
                                is IllegalArgumentException -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to error.message))
                                else -> call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to update product"))
                            }
                        }
                    )
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body"))
                }
            }
            
            // ========== Stock Management ==========
            
            /**
             * PATCH /factory/products/:id/stock
             * Adjust stock (increase/decrease)
             */
            patch("/products/{id}/stock") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.getClaim("userId").asString()
                    val role = UserRole.valueOf(principal.payload.getClaim("role").asString())
                    val productId = call.parameters["id"] ?: return@patch call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Product ID is required")
                    )
                    val request = call.receive<StockAdjustmentRequest>()
                    
                    inventoryService.adjustStock(productId, request.quantityChange, request.reason, userId, role)
                    
                    call.respond(HttpStatusCode.OK, mapOf(
                        "message" to "Stock adjusted successfully",
                        "productId" to productId,
                        "change" to request.quantityChange
                    ))
                } catch (e: SecurityException) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to adjust stock"))
                }
            }
            
            /**
             * GET /factory/products/:id/inventory-logs
             * Get inventory logs for a product
             */
            get("/products/{id}/inventory-logs") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val role = UserRole.valueOf(principal!!.payload.getClaim("role").asString())
                    
                    if (role != UserRole.ADMIN) {
                        return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                    }
                    
                    val productId = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Product ID is required")
                    )
                    
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                    val logs = inventoryService.getProductInventoryLogs(productId, limit)
                    
                    val response = logs.map { log ->
                        InventoryLogResponse.fromInventoryLog(log)
                    }
                    
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to fetch inventory logs"))
                }
            }
            
            /**
             * GET /factory/inventory-logs
             * Get all inventory logs
             */
            get("/inventory-logs") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val role = UserRole.valueOf(principal!!.payload.getClaim("role").asString())
                    
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                    
                    val logs = inventoryService.getAllInventoryLogs(role, limit)
                    val response = logs.map { log ->
                        InventoryLogResponse.fromInventoryLog(log)
                    }
                    
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: SecurityException) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to fetch inventory logs"))
                }
            }
        }
    }
}
