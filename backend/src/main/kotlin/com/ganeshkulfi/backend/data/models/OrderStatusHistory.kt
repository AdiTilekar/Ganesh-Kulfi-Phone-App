package com.ganeshkulfi.backend.data.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * Day 10: Order Status History Model
 * Audit trail for all order status changes
 * Used for admin dashboard and compliance tracking
 */

object OrderStatusHistory : Table("order_status_history") {
    val id = uuid("id").autoGenerate()
    val orderId = uuid("order_id").references(Orders.id)
    val oldStatus = varchar("old_status", 50).nullable()
    val newStatus = varchar("new_status", 50)
    val changedBy = uuid("changed_by").nullable()
    val changedByRole = varchar("changed_by_role", 20)  // ADMIN, RETAILER
    val reason = text("reason").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}

@Serializable
data class StatusHistory(
    val id: String,
    val orderId: String,
    val oldStatus: String?,
    val newStatus: String,
    val changedBy: String?,
    val changedByRole: String,
    val reason: String?,
    val createdAt: String
)

fun ResultRow.toStatusHistory() = StatusHistory(
    id = this[OrderStatusHistory.id].toString(),
    orderId = this[OrderStatusHistory.orderId].toString(),
    oldStatus = this[OrderStatusHistory.oldStatus],
    newStatus = this[OrderStatusHistory.newStatus],
    changedBy = this[OrderStatusHistory.changedBy]?.toString(),
    changedByRole = this[OrderStatusHistory.changedByRole],
    reason = this[OrderStatusHistory.reason],
    createdAt = this[OrderStatusHistory.createdAt].toString()
)

/**
 * Retailer-safe timeline event (only shows key milestones)
 */
@Serializable
data class OrderTimelineEvent(
    val event: String,  // ORDER_PLACED, ORDER_CONFIRMED, ORDER_REJECTED, ORDER_CANCELLED
    val timestamp: String,
    val message: String?  // User-friendly message
)
