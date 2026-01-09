package com.ganeshkulfi.backend.data.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * Inventory Transaction Reasons
 * 
 * INITIAL_STOCK: Seeding initial stock into ledger
 * STOCK_ADJUSTMENT: Manual admin adjustment (add/remove stock)
 * ORDER: Stock deduction when admin confirms order
 * ORDER_CANCEL: Stock refund when order is cancelled
 * WASTAGE: Stock write-off due to wastage
 * DAMAGED: Stock write-off due to damage
 */
enum class InventoryReason {
    INITIAL_STOCK,
    STOCK_ADJUSTMENT,
    ORDER,
    ORDER_CANCEL,
    WASTAGE,
    DAMAGED
}

/**
 * Immutable Inventory Transaction Ledger Table
 * 
 * This table tracks ALL stock movements as immutable records.
 * Current stock = SUM(delta) for a product
 * 
 * Design decisions:
 * - id: BIGINT for scalability (millions of transactions)
 * - delta: Can be positive (add) or negative (deduct)
 * - reason: Enum for audit trail
 * - actor_id: Who performed this action (admin/system)
 * - order_id: Optional FK for order-related transactions
 * - ts: Timestamp for chronological ordering
 */
object InventoryTransactions : Table("inventory_tx") {
    val id = long("id").autoIncrement()
    val productId = varchar("product_id", 36)
    val delta = integer("delta")
    val reason = varchar("reason", 50)
    val actorId = varchar("actor_id", 36)
    val orderId = varchar("order_id", 36).nullable()
    val ts = timestamp("ts")
    
    override val primaryKey = PrimaryKey(id)
}

/**
 * Inventory Transaction Data Class
 */
data class InventoryTransaction(
    val id: Long,
    val productId: String,
    val delta: Int,
    val reason: InventoryReason,
    val actorId: String,
    val orderId: String?,
    val ts: Instant
)

/**
 * Current Stock Summary
 */
data class StockSummary(
    val productId: String,
    val productName: String,
    val currentStock: Int,
    val lastUpdated: Instant?
)

/**
 * Stock History Entry
 */
data class StockHistoryEntry(
    val id: Long,
    val delta: Int,
    val reason: InventoryReason,
    val actorEmail: String,
    val orderId: String?,
    val timestamp: Instant,
    val runningStock: Int
)
