package com.ganeshkulfi.backend.services

import com.ganeshkulfi.backend.data.dto.*
import com.ganeshkulfi.backend.data.models.*
import com.ganeshkulfi.backend.data.repository.InventoryRepository
import com.ganeshkulfi.backend.data.repository.ProductRepository

class InventoryService(
    private val inventoryRepository: InventoryRepository,
    private val productRepository: ProductRepository
) {
    
    /**
     * Validate if order items have sufficient stock
     */
    fun validateOrderStock(items: List<Pair<String, Int>>): StockValidationResult {
        return inventoryRepository.validateStockAvailability(items)
    }
    
    /**
     * Reserve stock for a pending order
     */
    fun reserveStockForOrder(orderId: String, items: List<Pair<String, Int>>, userId: String): Boolean {
        // First validate stock
        val validation = validateOrderStock(items)
        if (!validation.isValid) {
            return false
        }
        
        // Reserve each item
        items.forEach { (productId, quantity) ->
            val success = inventoryRepository.reserveStockForOrder(orderId, productId, quantity, userId)
            if (!success) {
                // If any reservation fails, release all and return false
                inventoryRepository.releaseReservedStock(orderId, userId)
                return false
            }
        }
        
        return true
    }
    
    /**
     * Release reserved stock (on order rejection/cancellation)
     */
    fun releaseReservedStock(orderId: String, userId: String) {
        inventoryRepository.releaseReservedStock(orderId, userId)
    }
    
    /**
     * Deduct stock permanently (on order confirmation)
     */
    fun deductConfirmedStock(orderId: String, userId: String) {
        inventoryRepository.deductConfirmedStock(orderId, userId)
    }
    
    /**
     * Manually adjust stock (factory owner only)
     */
    fun adjustStock(productId: String, quantityChange: Int, reason: String, userId: String, userRole: UserRole) {
        if (userRole != UserRole.ADMIN) {
            throw SecurityException("Only factory owners can adjust stock")
        }
        
        inventoryRepository.adjustStock(productId, quantityChange, reason, userId)
    }
    
    /**
     * Get inventory logs for a product
     */
    fun getProductInventoryLogs(productId: String, limit: Int = 50): List<InventoryLog> {
        return inventoryRepository.getProductInventoryLogs(productId, limit)
    }
    
    /**
     * Get all inventory logs (factory owner only)
     */
    fun getAllInventoryLogs(userRole: UserRole, limit: Int = 100): List<InventoryLog> {
        if (userRole != UserRole.ADMIN) {
            throw SecurityException("Only factory owners can view all inventory logs")
        }
        
        return inventoryRepository.getAllInventoryLogs(limit)
    }
    
    /**
     * Get available quantity for a product
     */
    fun getAvailableQuantity(productId: String): Int {
        return inventoryRepository.getAvailableQuantity(productId)
    }
}
