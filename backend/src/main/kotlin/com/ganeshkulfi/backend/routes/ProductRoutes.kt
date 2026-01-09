package com.ganeshkulfi.backend.routes

import com.ganeshkulfi.backend.data.dto.*
import com.ganeshkulfi.backend.data.models.UserRole
import com.ganeshkulfi.backend.services.ProductService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Product Routes
 * 
 * Public routes:
 * - GET /api/products - Get all available products
 * - GET /api/products/{id} - Get product by ID
 * - GET /api/products/category/{category} - Get products by category
 * - GET /api/products/seasonal - Get seasonal products
 * 
 * Admin-only routes:
 * - POST /api/products - Create product
 * - PATCH /api/products/{id} - Update product
 * - DELETE /api/products/{id} - Delete product
 * - PATCH /api/products/{id}/stock - Update stock
 */
fun Route.productRoutes(productService: ProductService) {
    
    route("/api/products") {
        
        // PUBLIC ROUTES - No authentication required
        
        /**
         * GET /api/products
         * Get all available products (for Android app)
         */
        get {
            productService.getAvailableProducts().fold(
                onSuccess = { products ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            message = "Products retrieved successfully",
                            data = ProductsListResponse(
                                products = products,
                                total = products.size
                            )
                        )
                    )
                },
                onFailure = { error ->
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(error.message ?: "Failed to retrieve products")
                    )
                }
            )
        }
        
        /**
         * GET /api/products/seasonal
         * Get seasonal products
         */
        get("/seasonal") {
            productService.getSeasonalProducts().fold(
                onSuccess = { products ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            message = "Seasonal products retrieved successfully",
                            data = ProductsListResponse(
                                products = products,
                                total = products.size
                            )
                        )
                    )
                },
                onFailure = { error ->
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(error.message ?: "Failed to retrieve seasonal products")
                    )
                }
            )
        }
        
        /**
         * GET /api/products/category/{category}
         * Get products by category
         */
        get("/category/{category}") {
            val category = call.parameters["category"] ?: run {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Category is required")
                )
                return@get
            }
            
            productService.getProductsByCategory(category).fold(
                onSuccess = { products ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            message = "Products retrieved successfully",
                            data = ProductsListResponse(
                                products = products,
                                total = products.size,
                                category = category.uppercase()
                            )
                        )
                    )
                },
                onFailure = { error ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(error.message ?: "Failed to retrieve products")
                    )
                }
            )
        }
        
        /**
         * GET /api/products/{id}
         * Get product by ID
         */
        get("/{id}") {
            val id = call.parameters["id"] ?: run {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Product ID is required")
                )
                return@get
            }
            
            productService.getProductById(id).fold(
                onSuccess = { product ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            message = "Product retrieved successfully",
                            data = product
                        )
                    )
                },
                onFailure = { error ->
                    call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse(error.message ?: "Product not found")
                    )
                }
            )
        }
    }
    
    // ADMIN-ONLY ROUTES - Require JWT authentication + ADMIN role
    authenticate("auth-jwt") {
        route("/api/products") {
            /**
             * GET /api/products/all
             * Get ALL products including unavailable (Admin view)
             */
            get("/all") {
                // Check if user is admin
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()
                
                if (role != UserRole.ADMIN.name) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ErrorResponse("Admin access required")
                    )
                    return@get
                }
                
                productService.getAllProducts().fold(
                    onSuccess = { products ->
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                message = "All products retrieved successfully",
                                data = ProductsListResponse(
                                    products = products,
                                    total = products.size
                                )
                            )
                        )
                    },
                    onFailure = { error ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(error.message ?: "Failed to retrieve products")
                        )
                    }
                )
            }
            
            /**
             * POST /api/products
             * Create new product (Admin only)
             */
            post {
                try {
                    // Check admin role
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.payload?.getClaim("role")?.asString()
                    
                    if (role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Admin access required")
                        )
                        return@post
                    }
                    
                    // Parse request
                    val request = call.receive<CreateProductRequest>()
                    
                    // Create product
                    productService.createProduct(request).fold(
                        onSuccess = { product ->
                            call.respond(
                                HttpStatusCode.Created,
                                ApiResponse(
                                    success = true,
                                    message = "Product created successfully",
                                    data = product
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error.message ?: "Failed to create product")
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
             * PATCH /api/products/{id}
             * Update product (Admin only)
             */
            patch("/{id}") {
                try {
                    // Check admin role
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.payload?.getClaim("role")?.asString()
                    
                    if (role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Admin access required")
                        )
                        return@patch
                    }
                    
                    // Get product ID
                    val id = call.parameters["id"] ?: run {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Product ID is required")
                        )
                        return@patch
                    }
                    
                    // Parse request
                    val request = call.receive<UpdateProductRequest>()
                    
                    // Update product
                    productService.updateProduct(id, request).fold(
                        onSuccess = { product ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Product updated successfully",
                                    data = product
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error.message ?: "Failed to update product")
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
             * DELETE /api/products/{id}
             * Delete product (Admin only)
             */
            delete("/{id}") {
                try {
                    // Check admin role
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.payload?.getClaim("role")?.asString()
                    
                    if (role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Admin access required")
                        )
                        return@delete
                    }
                    
                    // Get product ID
                    val id = call.parameters["id"] ?: run {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Product ID is required")
                        )
                        return@delete
                    }
                    
                    // Delete product
                    productService.deleteProduct(id).fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Product deleted successfully",
                                    data = null
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.NotFound,
                                ErrorResponse(error.message ?: "Product not found")
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
             * PATCH /api/products/{id}/stock
             * Update product stock (Admin only)
             */
            patch("/{id}/stock") {
                try {
                    // Check admin role
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.payload?.getClaim("role")?.asString()
                    
                    if (role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Admin access required")
                        )
                        return@patch
                    }
                    
                    // Get product ID
                    val id = call.parameters["id"] ?: run {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Product ID is required")
                        )
                        return@patch
                    }
                    
                    // Parse request
                    @kotlinx.serialization.Serializable
                    data class StockUpdateRequest(val quantity: Int)
                    
                    val request = call.receive<StockUpdateRequest>()
                    
                    // Update stock
                    productService.updateStock(id, request.quantity).fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Stock updated successfully",
                                    data = mapOf("newQuantity" to request.quantity)
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error.message ?: "Failed to update stock")
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
             * Day 13: PATCH /api/products/{id}/image
             * Update product image (Admin only)
             */
            patch("/{id}/image") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userRole = principal?.getClaim("role", String::class)
                        ?.let { UserRole.valueOf(it) }
                    
                    if (userRole != UserRole.ADMIN) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Only admins can update product images")
                        )
                        return@patch
                    }
                    
                    val id = call.parameters["id"] ?: run {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Product ID is required")
                        )
                        return@patch
                    }
                    
                    @kotlinx.serialization.Serializable
                    data class ImageUpdateRequest(val imageUrl: String)
                    
                    val request = call.receive<ImageUpdateRequest>()
                    
                    if (request.imageUrl.isBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Image URL is required")
                        )
                        return@patch
                    }
                    
                    productService.updateProductImage(id, request.imageUrl).fold(
                        onSuccess = { product ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Product image updated successfully",
                                    data = product
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error.message ?: "Failed to update product image")
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
             * Day 13: PATCH /api/products/{id}/stock
             * Update product stock (Admin only)
             */
            patch("/{id}/stock") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userRole = principal?.getClaim("role", String::class)
                        ?.let { UserRole.valueOf(it) }
                    
                    if (userRole != UserRole.ADMIN) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Only admins can update product stock")
                        )
                        return@patch
                    }
                    
                    val id = call.parameters["id"] ?: run {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Product ID is required")
                        )
                        return@patch
                    }
                    
                    val request = call.receive<UpdateStockDTO>()
                    
                    productService.updateProductStock(id, request.stockQuantity).fold(
                        onSuccess = { product ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Product stock updated successfully",
                                    data = product
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error.message ?: "Failed to update product stock")
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
             * Day 13: PATCH /api/products/{id}/activate
             * Activate product (Admin only)
             */
            patch("/{id}/activate") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userRole = principal?.getClaim("role", String::class)
                        ?.let { UserRole.valueOf(it) }
                    
                    if (userRole != UserRole.ADMIN) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Only admins can activate products")
                        )
                        return@patch
                    }
                    
                    val id = call.parameters["id"] ?: run {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Product ID is required")
                        )
                        return@patch
                    }
                    
                    productService.activateProduct(id).fold(
                        onSuccess = { product ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Product activated successfully",
                                    data = product
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error.message ?: "Failed to activate product")
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
             * Day 13: PATCH /api/products/{id}/deactivate
             * Deactivate product (Admin only)
             */
            patch("/{id}/deactivate") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userRole = principal?.getClaim("role", String::class)
                        ?.let { UserRole.valueOf(it) }
                    
                    if (userRole != UserRole.ADMIN) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Only admins can deactivate products")
                        )
                        return@patch
                    }
                    
                    val id = call.parameters["id"] ?: run {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Product ID is required")
                        )
                        return@patch
                    }
                    
                    productService.deactivateProduct(id).fold(
                        onSuccess = { product ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Product deactivated successfully",
                                    data = product
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error.message ?: "Failed to deactivate product")
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
