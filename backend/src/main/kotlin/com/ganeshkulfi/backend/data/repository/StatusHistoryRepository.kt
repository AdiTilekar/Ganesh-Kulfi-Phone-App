package com.ganeshkulfi.backend.data.repository

import com.ganeshkulfi.backend.data.models.OrderStatusHistory
import com.ganeshkulfi.backend.data.models.StatusHistory
import com.ganeshkulfi.backend.data.models.toStatusHistory
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

/**
 * Day 10: Status History Repository
 * Handles audit trail for order status changes
 */
class StatusHistoryRepository {

    /**
     * Record a status change
     * This is called whenever an order status changes
     */
    fun recordStatusChange(
        orderId: String,
        oldStatus: String?,
        newStatus: String,
        changedBy: String?,
        changedByRole: String,  // ADMIN or RETAILER
        reason: String? = null
    ): StatusHistory = transaction {
        val id = OrderStatusHistory.insert {
            it[OrderStatusHistory.orderId] = UUID.fromString(orderId)
            it[OrderStatusHistory.oldStatus] = oldStatus
            it[OrderStatusHistory.newStatus] = newStatus
            it[OrderStatusHistory.changedBy] = changedBy?.let { UUID.fromString(it) }
            it[OrderStatusHistory.changedByRole] = changedByRole
            it[OrderStatusHistory.reason] = reason
            it[createdAt] = Instant.now()
        } get OrderStatusHistory.id

        OrderStatusHistory.select { OrderStatusHistory.id eq id }
            .single()
            .toStatusHistory()
    }

    /**
     * Get full status history for an order (Admin view)
     */
    fun getOrderHistory(orderId: String): List<StatusHistory> = transaction {
        OrderStatusHistory.select { OrderStatusHistory.orderId eq UUID.fromString(orderId) }
            .orderBy(OrderStatusHistory.createdAt to SortOrder.ASC)
            .map { it.toStatusHistory() }
    }

    /**
     * Get latest status change for an order
     */
    fun getLatestStatusChange(orderId: String): StatusHistory? = transaction {
        OrderStatusHistory.select { OrderStatusHistory.orderId eq UUID.fromString(orderId) }
            .orderBy(OrderStatusHistory.createdAt to SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.toStatusHistory()
    }

    /**
     * Get status history for multiple orders
     */
    fun getHistoryForOrders(orderIds: List<String>): Map<String, List<StatusHistory>> = transaction {
        val uuidList = orderIds.map { UUID.fromString(it) }
        OrderStatusHistory.select { OrderStatusHistory.orderId inList uuidList }
            .orderBy(OrderStatusHistory.createdAt to SortOrder.ASC)
            .map { it.toStatusHistory() }
            .groupBy { it.orderId }
    }

    /**
     * Count status changes for an order
     */
    fun countStatusChanges(orderId: String): Long = transaction {
        OrderStatusHistory.select { OrderStatusHistory.orderId eq UUID.fromString(orderId) }
            .count()
    }

    /**
     * Get all status changes by a specific user
     */
    fun getChangesByUser(userId: String): List<StatusHistory> = transaction {
        OrderStatusHistory.select { OrderStatusHistory.changedBy eq UUID.fromString(userId) }
            .orderBy(OrderStatusHistory.createdAt to SortOrder.DESC)
            .map { it.toStatusHistory() }
    }
}
