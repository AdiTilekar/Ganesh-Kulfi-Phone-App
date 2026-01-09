package com.ganeshkulfi.app.data.remote

import com.ganeshkulfi.app.data.model.User
import retrofit2.Response
import retrofit2.http.*

/**
 * API Service for Backend Communication
 */
interface ApiService {
    
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthData>>
    
    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthData>>
    
    @POST("/api/users")
    suspend fun createUser(
        @Header("Authorization") token: String,
        @Body request: CreateUserRequest
    ): Response<ApiResponse<UserDto>>
    
    @GET("/api/products")
    suspend fun getProducts(): Response<ApiResponse<ProductsData>>
    
    @GET("/factory/products")
    suspend fun getAdminProducts(@Header("Authorization") token: String): Response<List<AdminProduct>>
    
    @GET("/api/admin/orders")
    suspend fun getOrders(@Header("Authorization") token: String): Response<ApiResponse<AdminOrdersResponse>>
    
    @GET("/api/orders/my")
    suspend fun getMyOrders(@Header("Authorization") token: String): Response<ApiResponse<OrdersListResponse>>
    
    @POST("/api/orders")
    suspend fun createOrder(
        @Header("Authorization") token: String,
        @Body request: CreateOrderRequest
    ): Response<ApiResponse<OrderResponse>>
    
    @GET("/api/users")
    suspend fun getUsers(@Header("Authorization") token: String): Response<ApiResponse<UsersListResponse>>
    
    // Retailer Management
    @PUT("/api/users/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: String,
        @Header("Authorization") token: String,
        @Body request: UpdateUserRequest
    ): Response<ApiResponse<UserDto>>
    
    @DELETE("/api/users/{userId}")
    suspend fun deleteUser(
        @Path("userId") userId: String,
        @Header("Authorization") token: String
    ): Response<ApiResponse<Unit>>
    
    // Order Status Management
    @POST("/api/orders/{orderId}/confirm")
    suspend fun confirmOrder(
        @Path("orderId") orderId: String,
        @Header("Authorization") token: String
    ): Response<ApiResponse<Order>>
    
    @POST("/api/orders/{orderId}/pack")
    suspend fun packOrder(
        @Path("orderId") orderId: String,
        @Header("Authorization") token: String
    ): Response<ApiResponse<Order>>
    
    @POST("/api/orders/{orderId}/out-for-delivery")
    suspend fun outForDeliveryOrder(
        @Path("orderId") orderId: String,
        @Header("Authorization") token: String
    ): Response<ApiResponse<Order>>
    
    @POST("/api/orders/{orderId}/deliver")
    suspend fun deliverOrder(
        @Path("orderId") orderId: String,
        @Header("Authorization") token: String
    ): Response<ApiResponse<Order>>
    
    @PATCH("/api/admin/orders/{orderId}/cancel")
    suspend fun cancelOrder(
        @Path("orderId") orderId: String,
        @Header("Authorization") token: String,
        @Body request: CancelOrderRequest
    ): Response<ApiResponse<Order>>
    
    // Inventory Management
    @PATCH("/api/products/{id}/stock")
    suspend fun updateProductStock(
        @Header("Authorization") token: String,
        @Path("id") productId: String,
        @Body request: Map<String, Int>
    ): Response<Map<String, Any>>
}

// DTOs
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val phone: String,
    val role: String
)

data class CreateUserRequest(
    val email: String,
    val password: String,
    val name: String,
    val phone: String?,
    val role: String,
    val retailerId: String? = null,
    val shopName: String? = null,
    val tier: String? = null
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)

data class AuthData(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    val phone: String?,
    val role: String,
    val retailerId: String?,
    val shopName: String?,
    val tier: String?,
    val isActive: Boolean? = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class ProductsData(
    val products: List<Product>
)

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val basePrice: Double,
    val imageUrl: String?,
    val isActive: Boolean,
    val stockQuantity: Int? = null,  // Backend stock quantity (admin only)
    val minOrderQuantity: Int? = null
)

// Admin product with full stock details from /api/factory/products
data class AdminProduct(
    val id: String,
    val name: String,
    val description: String?,
    val basePrice: Double,
    val category: String,
    val imageUrl: String?,
    val isAvailable: Boolean,
    val isSeasonal: Boolean,
    val stockQuantity: Int,
    val reservedQuantity: Int,
    val availableQuantity: Int,
    val status: String,
    val minOrderQuantity: Int,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class Order(
    val id: String,
    val orderNumber: String,
    val status: String,
    val totalAmount: Double,
    val createdAt: String,
    val retailerId: String? = null,
    val retailerName: String? = null,
    val retailerEmail: String? = null,
    val shopName: String? = null,
    val totalItems: Int? = null,
    val totalQuantity: Int? = null,
    val subtotal: Double? = null,
    val discount: Double? = null,
    val tax: Double? = null,
    val paymentStatus: String? = null,
    val retailerNotes: String? = null,
    val factoryNotes: String? = null,
    val updatedAt: String? = null,
    val items: List<OrderItem>? = null
)

data class OrderItem(
    val id: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val discountPercent: Double,
    val discountAmount: Double? = 0.0,
    val lineTotal: Double
)

data class AdminOrdersResponse(
    val orders: List<AdminOrderWithItems>,
    val totalCount: Int,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int
)

data class OrdersListResponse(
    val orders: List<Order>,
    val total: Int
)

data class AdminOrderWithItems(
    val order: Order,
    val items: List<OrderItem>,
    val itemCount: Int
)

data class UsersListResponse(
    val users: List<UserDto>,
    val total: Int
)

// Update/Delete DTOs
data class UpdateUserRequest(
    val name: String?,
    val phone: String?,
    val role: String?,
    val retailerId: String?,
    val shopName: String?,
    val tier: String?
)

data class CancelOrderRequest(
    val reason: String
)

data class CreateOrderRequest(
    val items: List<OrderItemRequest>,
    val retailerNotes: String? = null
)

data class OrderItemRequest(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val discountPercent: Double = 0.0
)

data class OrderResponse(
    val id: String,
    val orderNumber: String,
    val retailerId: String,
    val retailerEmail: String,
    val retailerName: String,
    val shopName: String?,
    val totalItems: Int,
    val totalQuantity: Int,
    val subtotal: Double,
    val discount: Double,
    val tax: Double,
    val totalAmount: Double,
    val status: String,
    val paymentStatus: String,
    val createdAt: String,
    val updatedAt: String
)
