package com.ganeshkulfi.backend.data.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

/**
 * Product Categories
 */
enum class ProductCategory {
    CLASSIC,    // Traditional flavors
    PREMIUM,    // Premium/luxury flavors
    FRUIT,      // Fruit-based kulfi
    FUSION,     // Modern/fusion flavors
    SPECIAL     // Special occasion flavors
}

/**
 * Product Status
 */
enum class ProductStatus {
    AVAILABLE,
    OUT_OF_STOCK,
    DISCONTINUED
}

/**
 * Products Table (Exposed ORM)
 */
object Products : Table("product") {
    val id = char("id", 36)
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val basePrice = decimal("base_price", 10, 2)
    val category = varchar("category", 50)
    val imageUrl = varchar("image_url", 500).nullable()
    val isAvailable = bool("is_available").default(true)
    val isSeasonal = bool("is_seasonal").default(false)
    val stockQuantity = integer("stock_quantity").default(0)
    val reservedQuantity = integer("reserved_quantity").default(0)
    val status = varchar("status", 20).default("AVAILABLE")
    val minOrderQuantity = integer("min_order_quantity").default(1)
    val isActive = bool("is_active").default(true) // Day 13: Product activation/deactivation
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())
    
    override val primaryKey = PrimaryKey(id)
}

/**
 * Product Data Class
 */
data class Product(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String?,
    val basePrice: Double,
    val category: ProductCategory,
    val imageUrl: String?,
    val isAvailable: Boolean = true,
    val isSeasonal: Boolean = false,
    val stockQuantity: Int = 0,
    val reservedQuantity: Int = 0,
    val status: ProductStatus = ProductStatus.AVAILABLE,
    val minOrderQuantity: Int = 1,
    val isActive: Boolean = true, // Day 13: Product activation/deactivation
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    /**
     * Calculate available quantity (stock - reserved)
     */
    val availableQuantity: Int
        get() = (stockQuantity - reservedQuantity).coerceAtLeast(0)
    
    /**
     * Check if product has sufficient stock
     */
    fun hasSufficientStock(requestedQuantity: Int): Boolean {
        return availableQuantity >= requestedQuantity
    }
}
