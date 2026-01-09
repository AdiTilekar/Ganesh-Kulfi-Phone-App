package com.ganeshkulfi.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganeshkulfi.app.data.model.InventoryItem
import com.ganeshkulfi.app.data.model.Retailer
import com.ganeshkulfi.app.data.repository.InventoryRepository
import com.ganeshkulfi.app.data.repository.RetailerRepository
import com.ganeshkulfi.app.data.repository.StockTransactionRepository
import com.ganeshkulfi.app.data.repository.PricingRepository
import com.ganeshkulfi.app.data.repository.AuthRepository
import com.ganeshkulfi.app.data.remote.ApiService
import com.ganeshkulfi.app.data.remote.UpdateUserRequest
import com.ganeshkulfi.app.data.remote.CancelOrderRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val retailerRepository: RetailerRepository,
    private val stockTransactionRepository: StockTransactionRepository,
    private val pricingRepository: PricingRepository,
    private val authRepository: AuthRepository,
    private val apiService: ApiService
) : ViewModel() {

    // Dashboard Stats
    private val _dashboardStats = MutableStateFlow(DashboardStats())
    val dashboardStats: StateFlow<DashboardStats> = _dashboardStats.asStateFlow()

    // Inventory
    val inventory: StateFlow<List<InventoryItem>> = inventoryRepository.inventoryFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Retailers
    val retailers: StateFlow<List<Retailer>> = retailerRepository.retailersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Low Stock Items
    val lowStockItems: StateFlow<List<InventoryItem>> = inventoryRepository.inventoryFlow
        .map { items ->
            inventoryRepository.getLowStockItems(20) // threshold = 20
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Orders
    private val _orders = MutableStateFlow<List<com.ganeshkulfi.app.data.remote.Order>>(emptyList())
    val orders: StateFlow<List<com.ganeshkulfi.app.data.remote.Order>> = _orders.asStateFlow()

    private val _ordersLoading = MutableStateFlow(false)
    val ordersLoading: StateFlow<Boolean> = _ordersLoading.asStateFlow()

    private val _ordersError = MutableStateFlow<String?>(null)
    val ordersError: StateFlow<String?> = _ordersError.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadDashboardStats()
        startAutoRefresh()
        // Initial data refresh
        refreshAllData()
    }
    
    private fun startAutoRefresh() {
        viewModelScope.launch {
            // Auto-refresh all data every 15 seconds for faster updates
            while (true) {
                kotlinx.coroutines.delay(15_000) // 15 seconds
                refreshAllData()
            }
        }
    }
    
    /**
     * Refresh all dashboard data immediately
     */
    fun refreshAllData() {
        viewModelScope.launch {
            val token = authRepository.getAuthToken()
            if (!token.isNullOrEmpty()) {
                // Refresh all data sources in parallel
                launch { inventoryRepository.refreshInventory() }
                launch { retailerRepository.refreshRetailers() }
                launch { fetchOrders(token) }
            }
        }
    }

    private fun loadDashboardStats() {
        viewModelScope.launch {
            _isLoading.value = true
            
            combine(
                inventoryRepository.inventoryFlow,
                retailerRepository.retailersFlow,
                stockTransactionRepository.transactionsFlow,
                _orders
            ) { inventory, retailers, transactions, orders ->
                
                val totalStock = inventory.sumOf { it.totalStock }
                val totalValue = inventory.sumOf { it.totalStock * it.costPrice }
                // Calculate total revenue from actual confirmed/delivered orders
                val totalRevenue = orders
                    .filter { it.status == "CONFIRMED" || it.status == "DELIVERED" }
                    .sumOf { it.totalAmount }
                val todaySales = calculateTodaySalesFromOrders(orders)
                val lowStockCount = inventory.count { it.availableStock < 20 }
                val activeRetailers = retailers.count { it.isActive }
                val totalOutstanding = retailers.sumOf { it.totalOutstanding }
                val pendingPayments = transactions.count { 
                    it.paymentStatus == com.ganeshkulfi.app.data.model.PaymentStatus.PENDING 
                }
                val pendingOrders = orders.count { 
                    it.status == "PENDING" 
                }
                
                DashboardStats(
                    todaySales = todaySales,
                    totalRevenue = totalRevenue,
                    totalStock = totalStock,
                    totalValue = totalValue,
                    lowStockItems = lowStockCount,
                    activeRetailers = activeRetailers,
                    totalOutstanding = totalOutstanding,
                    pendingPayments = pendingPayments,
                    pendingOrders = pendingOrders
                )
            }.collect { stats ->
                _dashboardStats.value = stats
                _isLoading.value = false
            }
        }
    }

    private fun calculateTodaySales(transactions: List<com.ganeshkulfi.app.data.model.StockTransaction>): Double {
        val todayStart = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        return transactions
            .filter { it.createdAt >= todayStart }
            .filter { it.transactionType == com.ganeshkulfi.app.data.model.TransactionType.GIVEN }
            .sumOf { it.totalAmount }
    }
    
    private fun calculateTodaySalesFromOrders(orders: List<com.ganeshkulfi.app.data.remote.Order>): Double {
        val todayStart = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        return orders
            .filter { order ->
                // Parse timestamp from order (format: 2026-01-02T...)
                try {
                    val timestamp = java.time.Instant.parse(order.createdAt).toEpochMilli()
                    timestamp >= todayStart
                } catch (e: Exception) {
                    false
                }
            }
            .filter { it.status == "CONFIRMED" || it.status == "DELIVERED" }
            .sumOf { it.totalAmount }
    }

    // Inventory Operations
    fun updateStock(flavorId: String, quantity: Int) {
        viewModelScope.launch {
            inventoryRepository.updateStock(flavorId, quantity)
        }
    }

    fun recordSale(flavorId: String, quantity: Int) {
        viewModelScope.launch {
            inventoryRepository.recordSale(flavorId, quantity)
        }
    }

    fun updateItemPrice(flavorId: String, costPrice: Double, sellingPrice: Double) {
        viewModelScope.launch {
            inventoryRepository.updatePrice(flavorId, costPrice, sellingPrice)
        }
    }
    
    // Manual refresh methods
    fun refreshInventory() {
        viewModelScope.launch {
            inventoryRepository.refreshInventory()
        }
    }
    
    fun refreshRetailers() {
        viewModelScope.launch {
            retailerRepository.refreshRetailers()
        }
    }
    
    fun refreshDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            // Refresh both in parallel
            launch { inventoryRepository.refreshInventory() }
            launch { retailerRepository.refreshRetailers() }
            // Wait a moment for data to update
            kotlinx.coroutines.delay(500)
            _isLoading.value = false
        }
    }

    // Retailer Operations
    fun addRetailer(retailer: Retailer) {
        viewModelScope.launch {
            retailerRepository.addRetailer(retailer)
        }
    }

    fun addRetailerWithCredentials(retailer: Retailer, email: String, password: String) {
        viewModelScope.launch {
            try {
                // First create the retailer to obtain a retailer id
                val createdResult = retailerRepository.createRetailerAccount(retailer, email, password)
                if (createdResult.isSuccess) {
                    val createdRetailer = createdResult.getOrThrow()

                    // Register retailer credentials via backend API
                    // This ensures admin stays logged in after creating a retailer
                    val registerResult = authRepository.registerRetailerCredentials(
                        email = email,
                        password = password,
                        name = createdRetailer.name,
                        phone = createdRetailer.phone,
                        retailerId = createdRetailer.id,
                        shopName = createdRetailer.shopName,
                        pricingTier = createdRetailer.pricingTier
                    )

                    if (registerResult.isFailure) {
                        // If registration failed, log the error
                        val error = registerResult.exceptionOrNull()
                        android.util.Log.e("AdminViewModel", "Retailer registration failed", error)
                    } else {
                        // Refresh retailers from backend to show the new retailer
                        retailerRepository.refreshRetailers()
                    }
                } else {
                    // Failed to create retailer
                    val error = createdResult.exceptionOrNull()
                    android.util.Log.e("AdminViewModel", "Retailer creation failed", error)
                }
            } catch (e: Exception) {
                // Handle error
                android.util.Log.e("AdminViewModel", "Error creating retailer account", e)
            }
        }
    }

    fun updateRetailer(retailer: Retailer) {
        viewModelScope.launch {
            retailerRepository.updateRetailer(retailer)
        }
    }

    fun deleteRetailer(retailerId: String) {
        viewModelScope.launch {
            retailerRepository.deleteRetailer(retailerId)
        }
    }

    // Stock Transaction Operations
    fun giveStockToRetailer(
        retailerId: String,
        flavorId: String,
        quantity: Int,
        basePrice: Double? = null  // Optional: if null, will use pricing from repository
    ) {
        viewModelScope.launch {
            // Get retailer
            val retailer = retailers.value.find { it.id == retailerId }
                ?: return@launch
            
            // Get flavor details for base price
            val flavor = inventory.value.find { it.flavorId == flavorId }
                ?: return@launch
            
            // Calculate retailer-specific price
            val actualBasePrice = basePrice ?: flavor.sellingPrice
            val (totalAmount, priceInfo) = pricingRepository.calculateTransactionAmount(
                retailer = retailer,
                flavorId = flavorId,
                basePrice = actualBasePrice,
                quantity = quantity
            )
            
            stockTransactionRepository.createTransaction(
                retailerId = retailerId,
                flavorId = flavorId,
                quantity = quantity,
                pricePerUnit = priceInfo.retailerPrice,  // Use retailer-specific price
                totalAmount = totalAmount,
                type = com.ganeshkulfi.app.data.model.TransactionType.GIVEN
            )
            
            // Update inventory
            inventoryRepository.giveStockToRetailer(flavorId, quantity)
        }
    }

    /**
     * Get price breakdown for a retailer and flavor
     */
    fun getPriceBreakdown(
        retailer: Retailer,
        flavorId: String,
        flavorName: String,
        basePrice: Double,
        quantity: Int
    ) = pricingRepository.getPriceBreakdown(retailer, flavorId, flavorName, basePrice, quantity)

    /**
     * Update retailer pricing tier
     */
    fun updateRetailerPricingTier(retailerId: String, newTier: com.ganeshkulfi.app.data.model.PricingTier) {
        viewModelScope.launch {
            val retailer = retailers.value.find { it.id == retailerId }
            retailer?.let {
                retailerRepository.updateRetailer(it.copy(pricingTier = newTier))
            }
        }
    }

    /**
     * Set custom price for a retailer-flavor combination
     */
    fun setCustomPrice(
        retailerId: String,
        flavorId: String,
        customPrice: Double,
        discount: Double = 0.0,
        minimumQuantity: Int = 0
    ) {
        pricingRepository.setCustomPrice(retailerId, flavorId, customPrice, discount, minimumQuantity)
    }

    /**
     * Remove custom price
     */
    fun removeCustomPrice(retailerId: String, flavorId: String) {
        pricingRepository.removeCustomPrice(retailerId, flavorId)
    }

    fun recordPayment(retailerId: String, amount: Double) {
        viewModelScope.launch {
            // Get retailer's pending transactions
            val pendingTransactions = stockTransactionRepository.getPendingPayments()
                .filter { it.retailerId == retailerId }
                .sortedBy { it.createdAt }
            
            var remainingAmount = amount
            
            // Apply payment to oldest transactions first
            for (transaction in pendingTransactions) {
                if (remainingAmount <= 0) break
                
                val transactionAmount = transaction.totalAmount
                if (remainingAmount >= transactionAmount) {
                    // Full payment for this transaction
                    stockTransactionRepository.updatePaymentStatus(
                        transaction.id,
                        com.ganeshkulfi.app.data.model.PaymentStatus.PAID
                    )
                    remainingAmount -= transactionAmount
                } else {
                    // Partial payment
                    stockTransactionRepository.updatePaymentStatus(
                        transaction.id,
                        com.ganeshkulfi.app.data.model.PaymentStatus.PARTIAL
                    )
                    remainingAmount = 0.0
                }
            }
        }
    }
    
    // API Integration for Retailer & Order Management
    
    /**
     * Update retailer via API
     */
    fun updateRetailerViaApi(retailer: Retailer, token: String) {
        viewModelScope.launch {
            try {
                // Use userId (backend user ID) for update, not retailerId
                val userIdToUpdate = if (retailer.userId.isNotEmpty()) retailer.userId else retailer.id
                
                val request = UpdateUserRequest(
                    name = retailer.name,
                    phone = retailer.phone,
                    role = "RETAILER",
                    retailerId = retailer.id,
                    shopName = retailer.shopName,
                    tier = retailer.pricingTier.name
                )
                
                val response = apiService.updateUser(userIdToUpdate, "Bearer $token", request)
                if (response.isSuccessful && response.body()?.success == true) {
                    // Refresh retailers from backend
                    retailerRepository.refreshRetailers()
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Failed to update user pricing tier (updateUserPricingTier)", e)
            }
        }
    }
    
    /**
     * Delete retailer via API
     */
    fun deleteRetailerViaApi(retailer: Retailer, token: String) {
        viewModelScope.launch {
            try {
                // Use userId (backend user ID) for deletion, not retailerId
                val userIdToDelete = if (retailer.userId.isNotEmpty()) retailer.userId else retailer.id
                val response = apiService.deleteUser(userIdToDelete, "Bearer $token")
                if (response.isSuccessful && response.body()?.success == true) {
                    // Refresh retailers from backend
                    retailerRepository.refreshRetailers()
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Failed to delete retailer via API", e)
            }
        }
    }
    
    /**
     * Fetch all orders from backend
     */
    fun fetchOrders(token: String) {
        viewModelScope.launch {
            _ordersLoading.value = true
            _ordersError.value = null
            
            try {
                
                val response = apiService.getOrders("Bearer $token")
                
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val adminResponse = response.body()?.data
                    
                    if (adminResponse != null) {
                        // Parse the orders from the admin dashboard response
                        val parsedOrders = adminResponse.orders.map { orderWithItems ->
                            // The order already has items nested, use the main order object
                            orderWithItems.order.copy(
                                items = orderWithItems.items
                            )
                        }
                        
                        _orders.value = parsedOrders
                        
                        parsedOrders.forEach { order ->
                        }
                    } else {
                        _ordersError.value = "No data in response"
                    }
                } else {
                    val errorBody = try { response.errorBody()?.string() } catch (e: Exception) { null }
                    val errorMsg = response.body()?.message ?: errorBody ?: "Failed to fetch orders (${response.code()})"
                    _ordersError.value = errorMsg
                    if (errorBody != null) {
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Failed to fetch admin orders", e)
                _ordersError.value = e.message ?: "Unknown error occurred"
            } finally {
                _ordersLoading.value = false
            }
        }
    }
    
    /**
     * Update order status via API
     */
    fun updateOrderStatus(orderId: String, newStatus: String, token: String) {
        viewModelScope.launch {
            _ordersLoading.value = true
            try {
                
                val response = when (newStatus.lowercase()) {
                    "confirmed" -> apiService.confirmOrder(orderId, "Bearer $token")
                    "packed" -> apiService.packOrder(orderId, "Bearer $token")
                    "out_for_delivery" -> apiService.outForDeliveryOrder(orderId, "Bearer $token")
                    "delivered" -> apiService.deliverOrder(orderId, "Bearer $token")
                    else -> {
                        null
                    }
                }
                
                
                if (response?.isSuccessful == true && response.body()?.success == true) {
                    // Refresh orders to show updated status
                    fetchOrders(token)
                } else {
                    val errorMsg = response?.body()?.message ?: "Failed to update order"
                    _ordersError.value = errorMsg
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Failed to update order status", e)
                _ordersError.value = e.message ?: "Failed to update order"
            } finally {
                _ordersLoading.value = false
            }
        }
    }
    
    /**
     * Cancel order via API
     */
    fun cancelOrder(orderId: String, reason: String, token: String) {
        viewModelScope.launch {
            _ordersLoading.value = true
            try {
                
                val request = CancelOrderRequest(reason)
                val response = apiService.cancelOrder(orderId, "Bearer $token", request)
                
                
                if (response.isSuccessful && response.body()?.success == true) {
                    // Refresh orders to show updated status
                    fetchOrders(token)
                } else {
                    val errorMsg = response.body()?.message ?: "Failed to cancel order"
                    _ordersError.value = errorMsg
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Failed to cancel order", e)
                _ordersError.value = e.message ?: "Failed to cancel order"
            } finally {
                _ordersLoading.value = false
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Cancel repository background tasks to prevent memory leaks
        try {
            inventoryRepository.close()
            retailerRepository.close()
        } catch (e: Exception) {
            android.util.Log.e("AdminViewModel", "Error closing repositories", e)
        }
    }
}

data class DashboardStats(
    val todaySales: Double = 0.0,
    val totalRevenue: Double = 0.0,
    val totalStock: Int = 0,
    val totalValue: Double = 0.0,
    val lowStockItems: Int = 0,
    val activeRetailers: Int = 0,
    val totalOutstanding: Double = 0.0,
    val pendingPayments: Int = 0,
    val pendingOrders: Int = 0
)
