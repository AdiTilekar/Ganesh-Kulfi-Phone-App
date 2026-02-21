package com.ganeshkulfi.backend.data.repository

import com.ganeshkulfi.backend.data.dto.StockValidationResult
import com.ganeshkulfi.backend.data.dto.InsufficientStockItem
import com.ganeshkulfi.backend.data.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class InventoryRepository {
    
    /**
     * Log an inventory change
     */
    fun logInventoryChange(
        productId: String,
        changeType: InventoryChangeType,
        quantityBefore: Int,
        quantityAfter: Int,
        quantityChange: Int,
        reason: String?,
        orderId: String? = null,
        performedBy: String
    ): InventoryLog = transaction {
        val log = InventoryLog(
            productId = productId,
            changeType = changeType,
            quantityBefore = quantityBefore,
            quantityAfter = quantityAfter,
            quantityChange = quantityChange,
            reason = reason,
            orderId = orderId,
            performedBy = performedBy
        )
        
        InventoryLogs.insert {
            it[InventoryLogs.id] = log.id
            it[InventoryLogs.productId] = log.productId
            it[InventoryLogs.changeType] = log.changeType.name
            it[InventoryLogs.quantityBefore] = log.quantityBefore
            it[InventoryLogs.quantityAfter] = log.quantityAfter
            it[InventoryLogs.quantityChange] = log.quantityChange
            it[InventoryLogs.reason] = log.reason
            it[InventoryLogs.orderId] = log.orderId
            it[InventoryLogs.performedBy] = log.performedBy
            it[InventoryLogs.createdAt] = log.createdAt
        }
        
        log
    }
    
    /**
     * Get inventory logs for a product
     */
    fun getProductInventoryLogs(productId: String, limit: Int = 50): List<InventoryLog> = transaction {
        InventoryLogs.select { InventoryLogs.productId eq productId }
            .orderBy(InventoryLogs.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { rowToInventoryLog(it) }
    }
    
    /**
     * Get all inventory logs (for factory owner)
     */
    fun getAllInventoryLogs(limit: Int = 100): List<InventoryLog> = transaction {
        InventoryLogs.selectAll()
            .orderBy(InventoryLogs.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { rowToInventoryLog(it) }
    }
    
    /**
     * Validate stock availability for order items
     */
    fun validateStockAvailability(items: List<Pair<String, Int>>): StockValidationResult = transaction {
        val insufficientItems = mutableListOf<InsufficientStockItem>()
        
        items.forEach { (productId, requestedQty) ->
            val product = Products.select { Products.id eq productId }
                .singleOrNull()
                ?.let { rowToProduct(it) }
            
            if (product == null) {
                insufficientItems.add(
                    InsufficientStockItem(
                        productId = productId,
                        productName = "Unknown Product",
                        requestedQuantity = requestedQty,
                        availableQuantity = 0
                    )
                )
            } else if (!product.hasSufficientStock(requestedQty)) {
                insufficientItems.add(
                    InsufficientStockItem(
                        productId = productId,
                        productName = product.name,
                        requestedQuantity = requestedQty,
                        availableQuantity = product.availableQuantity
                    )
                )
            }
        }
        
        StockValidationResult(
            isValid = insufficientItems.isEmpty(),
            insufficientProducts = insufficientItems
        )
    }
    
    /**
     * Reserve stock for an order (using database function)
     */
    fun reserveStockForOrder(orderId: String, productId: String, quantity: Int, userId: String): Boolean = transaction {
        var success = false
        val conn = this.connection.connection as java.sql.Connection
        conn.prepareStatement("SELECT reserve_stock_for_order(?::uuid, ?, ?, ?::uuid)").use { stmt ->
            stmt.setString(1, orderId)
            stmt.setString(2, productId)
            stmt.setInt(3, quantity)
            stmt.setString(4, userId)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                success = rs.getBoolean(1)
            }
        }
        success
    }
    
    /**
     * Release reserved stock (using database function)
     */
    fun releaseReservedStock(orderId: String, userId: String) = transaction {
        val conn = this.connection.connection as java.sql.Connection
        conn.prepareStatement("SELECT release_reserved_stock(?::uuid, ?::uuid)").use { stmt ->
            stmt.setString(1, orderId)
            stmt.setString(2, userId)
            stmt.executeQuery()
        }
    }
    
    /**
     * Deduct confirmed stock (using database function)
     */
    fun deductConfirmedStock(orderId: String, userId: String) = transaction {
        val conn = this.connection.connection as java.sql.Connection
        conn.prepareStatement("SELECT deduct_confirmed_stock(?::uuid, ?::uuid)").use { stmt ->
            stmt.setString(1, orderId)
            stmt.setString(2, userId)
            stmt.executeQuery()
        }
    }
    
    /**
     * Adjust stock manually (using database function)
     */
    fun adjustStock(productId: String, quantityChange: Int, reason: String, userId: String) = transaction {
        val conn = this.connection.connection as java.sql.Connection
        conn.prepareStatement("SELECT adjust_stock(?, ?, ?, ?::uuid)").use { stmt ->
            stmt.setString(1, productId)
            stmt.setInt(2, quantityChange)
            stmt.setString(3, reason.take(500))
            stmt.setString(4, userId)
            stmt.executeQuery()
        }
    }
    
    /**
     * Get available quantity for a product
     */
    fun getAvailableQuantity(productId: String): Int = transaction {
        var available = 0
        val conn = this.connection.connection as java.sql.Connection
        conn.prepareStatement("SELECT get_available_quantity(?)").use { stmt ->
            stmt.setString(1, productId)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                available = rs.getInt(1)
            }
        }
        available
    }
    
    // Helper functions
    
    private fun rowToInventoryLog(row: ResultRow): InventoryLog {
        return InventoryLog(
            id = row[InventoryLogs.id],
            productId = row[InventoryLogs.productId],
            changeType = InventoryChangeType.valueOf(row[InventoryLogs.changeType]),
            quantityBefore = row[InventoryLogs.quantityBefore],
            quantityAfter = row[InventoryLogs.quantityAfter],
            quantityChange = row[InventoryLogs.quantityChange],
            reason = row[InventoryLogs.reason],
            orderId = row[InventoryLogs.orderId],
            performedBy = row[InventoryLogs.performedBy],
            createdAt = row[InventoryLogs.createdAt]
        )
    }
    
    private fun rowToProduct(row: ResultRow): Product {
        return Product(
            id = row[Products.id],
            name = row[Products.name],
            description = row[Products.description],
            basePrice = row[Products.basePrice].toDouble(),
            category = ProductCategory.valueOf(row[Products.category]),
            imageUrl = row[Products.imageUrl],
            isAvailable = row[Products.isAvailable],
            isSeasonal = row[Products.isSeasonal],
            stockQuantity = row[Products.stockQuantity],
            reservedQuantity = row[Products.reservedQuantity],
            status = ProductStatus.valueOf(row[Products.status]),
            minOrderQuantity = row[Products.minOrderQuantity],
            createdAt = row[Products.createdAt],
            updatedAt = row[Products.updatedAt]
        )
    }
}
