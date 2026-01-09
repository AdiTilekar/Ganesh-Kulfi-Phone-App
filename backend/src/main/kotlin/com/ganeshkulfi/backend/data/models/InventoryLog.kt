package com.ganeshkulfi.backend.data.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

/**
 * Inventory Change Types
 */
enum class InventoryChangeType {
    ADDED,          // New product added
    INCREASED,      // Stock manually increased
    DECREASED,      // Stock manually decreased
    RESERVED,       // Stock reserved for pending order
    RELEASED,       // Reserved stock released (order rejected/cancelled)
    DEDUCTED,       // Stock deducted (order confirmed)
    ADJUSTMENT      // Manual adjustment/correction
}

/**
 * Inventory Logs Table (Exposed ORM)
 */
object InventoryLogs : Table("inventory_log") {
    val id = char("id", 36)
    val productId = char("product_id", 36)
    val changeType = varchar("change_type", 50)
    val quantityBefore = integer("quantity_before")
    val quantityAfter = integer("quantity_after")
    val quantityChange = integer("quantity_change")
    val reason = text("reason").nullable()
    val orderId = char("order_id", 36).nullable()
    val performedBy = char("performed_by", 36)
    val createdAt = timestamp("created_at").default(Instant.now())
    
    override val primaryKey = PrimaryKey(id)
}

/**
 * Inventory Log Data Class
 */
data class InventoryLog(
    val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val changeType: InventoryChangeType,
    val quantityBefore: Int,
    val quantityAfter: Int,
    val quantityChange: Int,
    val reason: String? = null,
    val orderId: String? = null,
    val performedBy: String,
    val createdAt: Instant = Instant.now()
)
