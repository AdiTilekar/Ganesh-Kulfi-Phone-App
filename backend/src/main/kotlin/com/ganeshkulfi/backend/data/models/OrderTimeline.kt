package com.ganeshkulfi.backend.data.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.UUID

/**
 * Day 11: Order Timeline Table
 * Tracks all status changes for orders with detailed history
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
