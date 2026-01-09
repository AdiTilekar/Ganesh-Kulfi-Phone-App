package com.ganeshkulfi.backend.data.repository

import com.ganeshkulfi.backend.data.models.Product
import com.ganeshkulfi.backend.data.models.ProductCategory
import com.ganeshkulfi.backend.data.models.ProductStatus
import com.ganeshkulfi.backend.data.models.Products
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant

/**
 * Product Repository
 * Handles all database operations for products
 */
class ProductRepository {
    
    /**
     * Map ResultRow to Product
     */
    private fun resultRowToProduct(row: ResultRow): Product {
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
            status = com.ganeshkulfi.backend.data.models.ProductStatus.valueOf(row[Products.status]),
            minOrderQuantity = row[Products.minOrderQuantity],
            isActive = row[Products.isActive], // Day 13
            createdAt = row[Products.createdAt],
            updatedAt = row[Products.updatedAt]
        )
    }
    
    /**
     * Get all products
     */
    fun findAll(): List<Product> = transaction {
        Products.selectAll()
            .orderBy(Products.category to SortOrder.ASC, Products.name to SortOrder.ASC)
            .mapNotNull { resultRowToProduct(it) }
    }
    
    /**
     * Get product by ID
     */
    fun findById(id: String): Product? = transaction {
        Products.select { Products.id eq id }
            .mapNotNull { resultRowToProduct(it) }
            .singleOrNull()
    }
    
    /**
     * Get products by category
     */
    fun findByCategory(category: ProductCategory): List<Product> = transaction {
        Products.select { Products.category eq category.name }
            .orderBy(Products.name to SortOrder.ASC)
            .mapNotNull { resultRowToProduct(it) }
    }
    
    /**
     * Get only available products (Day 13: Filter by isActive for retailers)
     */
    fun findAvailable(includeInactive: Boolean = false): List<Product> = transaction {
        val condition = if (includeInactive) {
            Products.isAvailable eq true
        } else {
            (Products.isAvailable eq true) and (Products.isActive eq true)
        }
        
        Products.select { condition }
            .orderBy(Products.category to SortOrder.ASC, Products.name to SortOrder.ASC)
            .mapNotNull { resultRowToProduct(it) }
    }
    
    /**
     * Get seasonal products
     */
    fun findSeasonal(): List<Product> = transaction {
        Products.select { (Products.isSeasonal eq true) and (Products.isAvailable eq true) }
            .orderBy(Products.name to SortOrder.ASC)
            .mapNotNull { resultRowToProduct(it) }
    }
    
    /**
     * Create new product
     */
    fun create(
        name: String,
        description: String?,
        basePrice: Double,
        category: ProductCategory,
        imageUrl: String? = null,
        isAvailable: Boolean = true,
        isSeasonal: Boolean = false,
        stockQuantity: Int = 0,
        minOrderQuantity: Int = 1
    ): Product? = transaction {
        val productId = java.util.UUID.randomUUID().toString()
        
        Products.insert {
            it[id] = productId
            it[Products.name] = name
            it[Products.description] = description
            it[Products.basePrice] = BigDecimal.valueOf(basePrice)
            it[Products.category] = category.name
            it[Products.imageUrl] = imageUrl
            it[Products.isAvailable] = isAvailable
            it[Products.isSeasonal] = isSeasonal
            it[Products.stockQuantity] = stockQuantity
            it[Products.minOrderQuantity] = minOrderQuantity
            it[createdAt] = Instant.now()
            it[updatedAt] = Instant.now()
        }
        
        findById(productId)
    }
    
    /**
     * Update product
     */
    fun update(id: String, updates: Map<String, Any?>): Product? = transaction {
        Products.update({ Products.id eq id }) {
            updates.forEach { (key, value) ->
                when (key) {
                    "name" -> it[name] = value as String
                    "description" -> it[description] = value as? String
                    "basePrice" -> it[basePrice] = BigDecimal.valueOf(value as Double)
                    "category" -> it[category] = (value as ProductCategory).name
                    "imageUrl" -> it[imageUrl] = value as? String
                    "isAvailable" -> it[isAvailable] = value as Boolean
                    "isSeasonal" -> it[isSeasonal] = value as Boolean
                    "stockQuantity" -> it[stockQuantity] = value as Int
                    "minOrderQuantity" -> it[minOrderQuantity] = value as Int
                }
            }
            it[updatedAt] = Instant.now()
        }
        
        findById(id)
    }
    
    /**
     * Delete product
     */
    fun delete(id: String): Boolean = transaction {
        Products.deleteWhere { Products.id eq id } > 0
    }
    
    /**
     * Check if product name exists
     */
    fun nameExists(name: String): Boolean = transaction {
        Products.select { Products.name eq name }
            .count() > 0
    }
    
    /**
     * Update stock quantity
     */
    fun updateStock(id: String, quantity: Int): Boolean = transaction {
        Products.update({ Products.id eq id }) {
            it[stockQuantity] = quantity
            it[updatedAt] = Instant.now()
        } > 0
    }
    
    /**
     * Create new product (Factory owner)
     */
    fun createProduct(
        name: String,
        description: String?,
        basePrice: Double,
        category: ProductCategory,
        imageUrl: String?,
        isAvailable: Boolean,
        isSeasonal: Boolean,
        stockQuantity: Int,
        minOrderQuantity: Int
    ): Product = transaction {
        val productId = java.util.UUID.randomUUID().toString()
        
        Products.insert {
            it[id] = productId
            it[Products.name] = name
            it[Products.description] = description
            it[Products.basePrice] = BigDecimal.valueOf(basePrice)
            it[Products.category] = category.name
            it[Products.imageUrl] = imageUrl
            it[Products.isAvailable] = isAvailable
            it[Products.isSeasonal] = isSeasonal
            it[Products.stockQuantity] = stockQuantity
            it[Products.reservedQuantity] = 0
            it[Products.status] = if (stockQuantity > 0) "AVAILABLE" else "OUT_OF_STOCK"
            it[Products.minOrderQuantity] = minOrderQuantity
            it[createdAt] = Instant.now()
            it[updatedAt] = Instant.now()
        }
        
        findById(productId)!!
    }
    
    /**
     * Update product details (Factory owner)
     */
    fun updateProduct(id: String, updates: Map<String, Any?>): Product? = transaction {
        val updateCount = Products.update({ Products.id eq id }) {
            updates.forEach { (key, value) ->
                when (key) {
                    "name" -> it[name] = value as String
                    "description" -> it[description] = value as? String
                    "basePrice" -> it[basePrice] = BigDecimal.valueOf(value as Double)
                    "category" -> it[category] = value as String
                    "imageUrl" -> it[imageUrl] = value as? String
                    "isAvailable" -> it[isAvailable] = value as Boolean
                    "isSeasonal" -> it[isSeasonal] = value as Boolean
                    "minOrderQuantity" -> it[minOrderQuantity] = value as Int
                    "status" -> it[status] = value as String
                }
            }
            it[updatedAt] = Instant.now()
        }
        
        if (updateCount > 0) findById(id) else null
    }
    
    /**
     * Get inventory for all products (Factory owner)
     */
    fun getAllWithInventory(): List<Product> = transaction {
        Products.selectAll()
            .orderBy(Products.status to SortOrder.ASC, Products.name to SortOrder.ASC)
            .map { resultRowToProduct(it) }
    }
    
    /**
     * Day 13: Update product image URL
     */
    fun updateProductImage(id: String, imageUrl: String): Product? = transaction {
        val updateCount = Products.update({ Products.id eq id }) {
            it[Products.imageUrl] = imageUrl
            it[updatedAt] = Instant.now()
        }
        
        if (updateCount > 0) findById(id) else null
    }
    
    /**
     * Day 13: Update product stock quantity
     */
    fun updateProductStock(id: String, stockQuantity: Int): Product? = transaction {
        val updateCount = Products.update({ Products.id eq id }) {
            it[Products.stockQuantity] = stockQuantity
            it[status] = if (stockQuantity > 0) "AVAILABLE" else "OUT_OF_STOCK"
            it[updatedAt] = Instant.now()
        }
        
        if (updateCount > 0) findById(id) else null
    }
    
    /**
     * Day 13: Activate product
     */
    fun activateProduct(id: String): Product? = transaction {
        val updateCount = Products.update({ Products.id eq id }) {
            it[isActive] = true
            it[updatedAt] = Instant.now()
        }
        
        if (updateCount > 0) findById(id) else null
    }
    
    /**
     * Day 13: Deactivate product
     */
    fun deactivateProduct(id: String): Product? = transaction {
        val updateCount = Products.update({ Products.id eq id }) {
            it[isActive] = false
            it[updatedAt] = Instant.now()
        }
        
        if (updateCount > 0) findById(id) else null
    }
}
