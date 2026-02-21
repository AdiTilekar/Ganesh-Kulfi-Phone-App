package com.ganeshkulfi.app.data.repository

import android.util.Log
import com.ganeshkulfi.app.BuildConfig
import com.ganeshkulfi.app.data.model.Order
import com.ganeshkulfi.app.data.model.OrderItem
import com.ganeshkulfi.app.data.model.OrderStatus
import com.ganeshkulfi.app.data.remote.ApiService
import com.ganeshkulfi.app.data.remote.CreateOrderRequest
import com.ganeshkulfi.app.data.remote.OrderItemRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) {
    
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    private val ordersFlow: Flow<List<Order>> = _orders.asStateFlow()

    suspend fun fetchRetailerOrders(): Result<List<Order>> {
        return withContext(Dispatchers.IO) {
            try {
                if (BuildConfig.DEBUG) Log.d(TAG, "Starting fetchRetailerOrders()")
                
                val token = authRepository.getAuthToken()
                if (token == null) {
                    if (BuildConfig.DEBUG) Log.e(TAG, "No auth token found")
                    return@withContext Result.failure(Exception("Not authenticated"))
                }
                
                if (BuildConfig.DEBUG) Log.d(TAG, "Calling API: /api/orders/my")
                
                val response = apiService.getMyOrders("Bearer $token")
                
                if (BuildConfig.DEBUG) Log.d(TAG, "Response code: ${response.code()}, successful: ${response.isSuccessful}")
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val apiOrders = response.body()?.data?.orders ?: emptyList()
                    if (BuildConfig.DEBUG) Log.d(TAG, "Fetched ${apiOrders.size} orders from API")
                    
                    // Map API Order to model Order
                    val orders = apiOrders.map { apiOrder ->
                        Order(
                            id = apiOrder.id,
                            userId = apiOrder.retailerId ?: "",
                            items = apiOrder.items?.map { apiItem ->
                                OrderItem(
                                    flavorId = apiItem.productId,
                                    flavorName = apiItem.productName,
                                    quantity = apiItem.quantity,
                                    basePrice = apiItem.unitPrice,
                                    discountedPrice = apiItem.unitPrice * (1 - (apiItem.discountPercent / 100.0)),
                                    subtotal = apiItem.lineTotal
                                )
                            } ?: emptyList(),
                            subtotal = apiOrder.subtotal ?: apiOrder.totalAmount,
                            discountAmount = apiOrder.discount ?: 0.0,
                            totalAmount = apiOrder.totalAmount,
                            status = when (apiOrder.status.uppercase()) {
                                "PENDING" -> OrderStatus.PENDING
                                "CONFIRMED" -> OrderStatus.CONFIRMED
                                "PACKED" -> OrderStatus.PACKED
                                "OUT_FOR_DELIVERY" -> OrderStatus.OUT_FOR_DELIVERY
                                "DELIVERED" -> OrderStatus.DELIVERED
                                "COMPLETED" -> OrderStatus.COMPLETED
                                "REJECTED" -> OrderStatus.REJECTED
                                "CANCELLED" -> OrderStatus.CANCELLED
                                "CANCELLED_ADMIN" -> OrderStatus.CANCELLED_ADMIN
                                // Legacy status mappings
                                "PREPARING" -> OrderStatus.PACKED
                                "READY" -> OrderStatus.READY
                                "DISPATCHED" -> OrderStatus.OUT_FOR_DELIVERY
                                else -> OrderStatus.PENDING
                            },
                            customerName = apiOrder.retailerName ?: "",
                            customerPhone = apiOrder.retailerEmail ?: "",
                            customerAddress = "",
                            shopName = apiOrder.shopName ?: "",
                            notes = apiOrder.retailerNotes ?: "",
                            createdAt = parseTimestamp(apiOrder.createdAt),
                            updatedAt = parseTimestamp(apiOrder.updatedAt ?: apiOrder.createdAt)
                        )
                    }
                    
                    if (BuildConfig.DEBUG) Log.d(TAG, "Mapped ${orders.size} orders to model")
                    
                    // Update local state
                    _orders.value = orders
                    
                    Result.success(orders)
                } else {
                    val errorMsg = response.body()?.message ?: "Failed to fetch orders"
                    if (BuildConfig.DEBUG) Log.e(TAG, "Failed to fetch orders: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching orders: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    private fun parseTimestamp(timestamp: String): Long {
        return try {
            // Try parsing ISO 8601 format using SimpleDateFormat for API < 26 compatibility
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            sdf.parse(timestamp)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Failed to parse timestamp: $timestamp - ${e.message}")
            // Fallback to current time
            System.currentTimeMillis()
        }
    }
    
    suspend fun createOrder(order: Order): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Get auth token
                val token = authRepository.getAuthToken()
                    ?: return@withContext Result.failure(Exception("Not authenticated"))
                
                // Convert order items to API format
                val orderItems = order.items.map { item ->
                    OrderItemRequest(
                        productId = item.flavorId,
                        productName = item.flavorName,
                        quantity = item.quantity,
                        unitPrice = item.basePrice,
                        discountPercent = 0.0
                    )
                }
                
                // Create order request
                val request = CreateOrderRequest(
                    items = orderItems,
                    retailerNotes = order.notes
                )
                
                if (BuildConfig.DEBUG) Log.d(TAG, "Creating order with ${orderItems.size} item types")
                
                // Call backend API
                val response = apiService.createOrder("Bearer $token", request)
                
                if (BuildConfig.DEBUG) Log.d(TAG, "Create order response: ${response.code()}")
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val orderData = response.body()?.data
                    if (BuildConfig.DEBUG) Log.d(TAG, "Order created: ${orderData?.orderNumber}")
                    
                    // Refresh orders list
                    fetchRetailerOrders()
                    
                    Result.success(orderData?.id ?: "")
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = response.body()?.message ?: errorBody ?: "Failed to create order (code: ${response.code()})"
                    if (BuildConfig.DEBUG) Log.e(TAG, "Order creation failed: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception creating order: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    fun getUserOrdersFlow(userId: String): Flow<List<Order>> {
        return ordersFlow.map { orders ->
            orders.filter { it.userId == userId }
                .sortedByDescending { it.createdAt }
        }
    }

    fun getAllOrdersFlow(): Flow<List<Order>> {
        return ordersFlow.map { orders ->
            orders.sortedByDescending { it.createdAt }
        }
    }

    suspend fun getOrderById(orderId: String): Result<Order> {
        return try {
            val order = _orders.value.find { it.id == orderId }
                ?: throw Exception("Order not found")
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> {
        return try {
            _orders.value = _orders.value.map { order ->
                if (order.id == orderId) {
                    order.copy(
                        status = status,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    order
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelOrder(orderId: String): Result<Unit> {
        return updateOrderStatus(orderId, OrderStatus.CANCELLED)
    }

    fun getRetailerOrdersFlow(userId: String): Flow<List<Order>> {
        return ordersFlow.map { orders ->
            if (BuildConfig.DEBUG) Log.d(TAG, "Filtering ${orders.size} orders for userId: $userId")
            val filtered = orders.filter { it.userId == userId }
            if (BuildConfig.DEBUG) Log.d(TAG, "Found ${filtered.size} orders for this user")
            filtered.sortedByDescending { it.createdAt }
        }
    }

    fun getPendingOrdersFlow(): Flow<List<Order>> {
        return ordersFlow.map { orders ->
            orders.filter { it.status == OrderStatus.PENDING }
                .sortedByDescending { it.createdAt }
        }
    }

    fun getOrdersByStatus(status: OrderStatus): Flow<List<Order>> {
        return ordersFlow.map { orders ->
            orders.filter { it.status == status }
                .sortedByDescending { it.createdAt }
        }
    }

    suspend fun getTotalRevenue(): Double {
        return _orders.value.filter { 
            it.status == OrderStatus.DELIVERED || it.status == OrderStatus.READY 
        }.sumOf { it.totalAmount }
    }

    suspend fun getTotalOrderCount(): Int {
        return _orders.value.size
    }

    suspend fun getPendingOrderCount(): Int {
        return _orders.value.count { it.status == OrderStatus.PENDING }
    }

    companion object {
        private const val TAG = "OrderRepository"
    }
}
