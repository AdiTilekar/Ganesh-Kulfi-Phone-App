package com.ganeshkulfi.backend.data.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.UUID

/**
 * Day 11: Order Timeline Table
 * Tracks all status changes for orders with detailed history
 *
 * TODO(tech-debt): This table overlaps with order_status_history (V9).
 *  Both have active DB triggers on orders.status changes, so every update
 *  writes to BOTH tables. Consider merging:
 *    - Add `old_status`, `changed_by`, `reason` columns here, OR
 *    - Add `message`, `notification_sent` to order_status_history,
 *  then drop the redundant table + trigger.
 */
object OrderTimelines : UUIDTable("order_timeline") {
    val orderId = uuid("order_id").references(Orders.id)
    val status = varchar("status", 50)
    val message = text("message").nullable()
    val createdBy = uuid("created_by").nullable()
    val createdByRole = varchar("created_by_role", 20).nullable()
    val notificationSent = bool("notification_sent").default(false)
    val createdAt = timestamp("created_at").default(Instant.now())
}

/**
 * Order Timeline Domain Model
 */
data class OrderTimeline(
    val id: String,
    val orderId: String,
    val status: String,
    val message: String?,
    val createdBy: String?,
    val createdByRole: String?,
    val notificationSent: Boolean = false,
    val createdAt: Instant = Instant.now()
)
