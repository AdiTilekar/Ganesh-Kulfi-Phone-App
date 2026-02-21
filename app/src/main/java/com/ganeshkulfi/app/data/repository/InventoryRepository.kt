package com.ganeshkulfi.app.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.ganeshkulfi.app.BuildConfig
import com.ganeshkulfi.app.data.model.Flavor
import com.ganeshkulfi.app.data.model.InventoryItem
import com.ganeshkulfi.app.data.remote.ApiService
import com.ganeshkulfi.app.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepository @Inject constructor(
    private val apiService: ApiService,
    private val sharedPreferences: SharedPreferences,
    @ApplicationScope private val repositoryScope: CoroutineScope
) {
    
    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventoryFlow: Flow<List<InventoryItem>> = _inventory.asStateFlow()

    init {
        // Initialize inventory from flavors
        initializeInventory()
        
        // Start auto-refresh every 30 seconds
        repositoryScope.launch {
            var failureCount = 0
            while (isActive) {
                try {
                    fetchInventoryFromBackend()
                    failureCount = 0 // Reset on success
                    delay(30_000) // Refresh every 30 seconds
                } catch (e: Exception) {
                    failureCount++
                    val backoffDelay = minOf(60_000L * failureCount, 300_000L) // Max 5 min
                    Log.e(TAG, "Auto-refresh failed (attempt $failureCount), retrying in ${backoffDelay/1000}s", e)
                    delay(backoffDelay)
                }
            }
        }
    }

    private fun initializeInventory() {
        val flavors = Flavor.getDefaultFlavors()
        _inventory.value = flavors.map { flavor ->
            InventoryItem(
                flavorId = flavor.key,
                flavorName = flavor.nameEn,
                totalStock = flavor.stock,
                availableStock = flavor.stock,
                stockGivenToRetailers = 0,
                soldToday = 0,
                soldThisWeek = 0,
                soldThisMonth = 0,
                soldQuantity = 0,
                costPrice = flavor.price * 0.6, // 40% profit margin
                sellingPrice = flavor.price.toDouble(),
                reorderLevel = 20
            )
        }
    }
    
    private suspend fun fetchInventoryFromBackend() {
        try {
            val token = sharedPreferences.getString("auth_token", null)
            if (token.isNullOrEmpty()) {
                if (BuildConfig.DEBUG) Log.w(TAG, "No auth token, skipping inventory sync")
                return
            }
            
            // Use admin endpoint to get products WITH stock info
            val response = apiService.getAdminProducts("Bearer $token")
            
            if (response.isSuccessful && response.body() != null) {
                val adminProducts = response.body()!!
                val currentInventory = _inventory.value
                val updatedInventory = adminProducts.map { product ->
                    val existingItem = currentInventory.find { it.flavorId == product.id }
                    
                    InventoryItem(
                        flavorId = product.id,
                        flavorName = product.name,
                        totalStock = product.stockQuantity,
                        availableStock = product.availableQuantity,
                        stockGivenToRetailers = product.reservedQuantity,
                        soldToday = existingItem?.soldToday ?: 0,
                        soldThisWeek = existingItem?.soldThisWeek ?: 0,
                        soldThisMonth = existingItem?.soldThisMonth ?: 0,
                        soldQuantity = existingItem?.soldQuantity ?: 0,
                        costPrice = existingItem?.costPrice ?: (product.basePrice * 0.6),
                        sellingPrice = product.basePrice,
                        reorderLevel = existingItem?.reorderLevel ?: 20,
                        lastRestockedAt = existingItem?.lastRestockedAt ?: 0L,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                
                if (updatedInventory.isNotEmpty()) {
                    _inventory.value = updatedInventory
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Updated ${adminProducts.size} inventory items from backend")
                        Log.d(TAG, "Stock: ${updatedInventory.take(3).map { "${it.flavorName}: ${it.totalStock}" }}")
                    }
                }
            } else {
                if (BuildConfig.DEBUG) Log.w(TAG, "Failed to fetch admin products: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching inventory: ${e.message}", e)
        }
    }
    
    suspend fun refreshInventory() {
        fetchInventoryFromBackend()
    }

    suspend fun getAllInventory(): Result<List<InventoryItem>> {
        return try {
            Result.success(_inventory.value)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInventoryByFlavor(flavorId: String): Result<InventoryItem> {
        return try {
            val item = _inventory.value.find { it.flavorId == flavorId }
            if (item != null) {
                Result.success(item)
            } else {
                Result.failure(Exception("Inventory item not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStock(flavorId: String, quantity: Int): Result<Unit> {
        return try {
            val token = sharedPreferences.getString("auth_token", null)
            if (token.isNullOrEmpty()) {
                if (BuildConfig.DEBUG) Log.e(TAG, "No auth token for stock update")
                return Result.failure(Exception("Not authenticated"))
            }
            
            // Call backend API to update stock
            val updateStockDto = mapOf("quantity" to quantity)
            val response = apiService.updateProductStock("Bearer $token", flavorId, updateStockDto)
            
            if (response.isSuccessful) {
                // Update local state after successful backend update
                _inventory.value = _inventory.value.map { item ->
                    if (item.flavorId == flavorId) {
                        item.copy(
                            totalStock = item.totalStock + quantity,
                            availableStock = item.availableStock + quantity,
                            lastRestockedAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        item
                    }
                }
                if (BuildConfig.DEBUG) Log.d(TAG, "Stock updated successfully for $flavorId: +$quantity units")
                Result.success(Unit)
            } else {
                val errorMsg = response.body()?.get("message")?.toString() ?: "Failed to update stock"
                if (BuildConfig.DEBUG) Log.e(TAG, "Stock update failed: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating stock: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun giveStockToRetailer(flavorId: String, quantity: Int): Result<Unit> {
        return try {
            _inventory.value = _inventory.value.map { item ->
                if (item.flavorId == flavorId) {
                    if (item.availableStock >= quantity) {
                        item.copy(
                            availableStock = item.availableStock - quantity,
                            stockGivenToRetailers = item.stockGivenToRetailers + quantity,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        throw Exception("Insufficient stock available")
                    }
                } else {
                    item
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordSale(flavorId: String, quantity: Int): Result<Unit> {
        return try {
            _inventory.value = _inventory.value.map { item ->
                if (item.flavorId == flavorId) {
                    item.copy(
                        availableStock = item.availableStock - quantity,
                        soldToday = item.soldToday + quantity,
                        soldThisWeek = item.soldThisWeek + quantity,
                        soldThisMonth = item.soldThisMonth + quantity,
                        soldQuantity = item.soldQuantity + quantity,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    item
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLowStockItems(threshold: Int = 20): List<InventoryItem> {
        return _inventory.value.filter { it.availableStock <= threshold }
    }

    suspend fun getLowStockItemsAsync(): Result<List<InventoryItem>> {
        return try {
            Result.success(_inventory.value.filter { it.needsRestock })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTotalInventoryValue(): Double {
        return _inventory.value.sumOf { it.totalStock * it.costPrice }
    }

    suspend fun getTotalPotentialRevenue(): Double {
        return _inventory.value.sumOf { it.availableStock * it.sellingPrice }
    }

    suspend fun updatePrice(flavorId: String, costPrice: Double, sellingPrice: Double): Result<Unit> {
        return try {
            val currentInventory = _inventory.value.toMutableList()
            val index = currentInventory.indexOfFirst { it.flavorId == flavorId }
            
            if (index != -1) {
                currentInventory[index] = currentInventory[index].copy(
                    costPrice = costPrice,
                    sellingPrice = sellingPrice,
                    updatedAt = System.currentTimeMillis()
                )
                _inventory.value = currentInventory
                Result.success(Unit)
            } else {
                Result.failure(Exception("Flavor not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Cancel background coroutines when repository is no longer needed
     * Call this to prevent memory leaks
     */
    fun close() {
        repositoryScope.cancel()
    }

    companion object {
        private const val TAG = "InventoryRepo"
    }
}
