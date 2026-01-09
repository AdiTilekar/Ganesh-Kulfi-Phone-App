package com.ganeshkulfi.backend.data.dto

import com.ganeshkulfi.backend.data.models.InventoryChangeType
import com.ganeshkulfi.backend.data.models.InventoryLog
import com.ganeshkulfi.backend.data.models.Product
import kotlinx.serialization.Serializable

/**
 * Request to adjust stock quantity (Factory only)
 */
@Serializable
data class StockAdjustmentRequest(
    val quantityChange: Int, // Positive for increase, negative for decrease
    val reason: String
)

/**
 * Response for product with inventory details
 */
@Serializable
data class ProductInventoryResponse(
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
    val isActive: Boolean, // Day 13
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun fromProduct(product: Product): ProductInventoryResponse {
            return ProductInventoryResponse(
                id = product.id,
                name = product.name,
                description = product.description,
                basePrice = product.basePrice,
                category = product.category.name,
                imageUrl = product.imageUrl,
                isAvailable = product.isAvailable,
                isSeasonal = product.isSeasonal,
                stockQuantity = product.stockQuantity,
                reservedQuantity = product.reservedQuantity,
                availableQuantity = product.availableQuantity,
                status = product.status.name,
                minOrderQuantity = product.minOrderQuantity,
                isActive = product.isActive, // Day 13
                createdAt = product.createdAt.toString(),
                updatedAt = product.updatedAt.toString()
            )
        }
    }
}

/**
 * Response for inventory log entry
 */
@Serializable
data class InventoryLogResponse(
    val id: String,
    val productId: String,
    val productName: String?,
    val changeType: String,
    val quantityBefore: Int,
    val quantityAfter: Int,
    val quantityChange: Int,
    val reason: String?,
    val orderId: String?,
    val performedBy: String,
    val performedByEmail: String?,
    val createdAt: String
) {
    companion object {
        fun fromInventoryLog(
            log: InventoryLog,
            productName: String? = null,
            performedByEmail: String? = null
        ): InventoryLogResponse {
            return InventoryLogResponse(
                id = log.id,
                productId = log.productId,
                productName = productName,
                changeType = log.changeType.name,
                quantityBefore = log.quantityBefore,
                quantityAfter = log.quantityAfter,
                quantityChange = log.quantityChange,
                reason = log.reason,
                orderId = log.orderId,
                performedBy = log.performedBy,
                performedByEmail = performedByEmail,
                createdAt = log.createdAt.toString()
            )
        }
    }
}

/**
 * Stock validation result
 */
data class StockValidationResult(
    val isValid: Boolean,
    val insufficientProducts: List<InsufficientStockItem> = emptyList()
)

/**
 * Item with insufficient stock
 */
@Serializable
data class InsufficientStockItem(
    val productId: String,
    val productName: String,
    val requestedQuantity: Int,
    val availableQuantity: Int
)
