package com.ganeshkulfi.backend.data.repository

import com.ganeshkulfi.backend.data.models.OrderTimeline
import com.ganeshkulfi.backend.data.models.OrderTimelines
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

/**
 * Day 11: Order Timeline Repository
 * Manages order status history and timeline entries
 */
class OrderTimelineRepository {
    
    /**
     * Create timeline entry
     */
    fun create(
        orderId: String,
        status: String,
        message: String?,
        createdBy: String? = null,
        createdByRole: String? = null
    ): OrderTimeline? = transaction {
        val id = OrderTimelines.insert {
            it[OrderTimelines.orderId] = UUID.fromString(orderId)
            it[OrderTimelines.status] = status
            it[OrderTimelines.message] = message
            it[OrderTimelines.createdBy] = createdBy?.let { UUID.fromString(it) }
            it[OrderTimelines.createdByRole] = createdByRole
            it[notificationSent] = false
            it[createdAt] = Instant.now()
        } get OrderTimelines.id
        
        findById(id.toString())
    }
    
    /**
     * Find timeline entry by ID
     */
    fun findById(id: String): OrderTimeline? = transaction {
        OrderTimelines.select { OrderTimelines.id eq UUID.fromString(id) }
            .mapNotNull { toOrderTimeline(it) }
            .singleOrNull()
    }
    
    /**
     * Get timeline for an order
     */
    fun getTimelineForOrder(orderId: String): List<OrderTimeline> = transaction {
        OrderTimelines
            .select { OrderTimelines.orderId eq UUID.fromString(orderId) }
            .orderBy(OrderTimelines.createdAt to SortOrder.DESC)
            .mapNotNull { toOrderTimeline(it) }
    }
    
    /**
     * Mark notification as sent
     */
    fun markNotificationSent(id: String): Boolean = transaction {
        val updated = OrderTimelines.update({ OrderTimelines.id eq UUID.fromString(id) }) {
            it[notificationSent] = true
        }
        updated > 0
    }
    
    /**
     * Get all timeline entries (for admin)
     */
    fun getAll(): List<OrderTimeline> = transaction {
        OrderTimelines
            .selectAll()
            .orderBy(OrderTimelines.createdAt to SortOrder.DESC)
            .mapNotNull { toOrderTimeline(it) }
    }
    
    /**
     * Delete timeline entries for an order
     */
    fun deleteForOrder(orderId: String): Int = transaction {
        OrderTimelines.deleteWhere { 
            OrderTimelines.orderId eq UUID.fromString(orderId) 
        }
    }
    
    /**
     * Convert ResultRow to OrderTimeline
     */
    private fun toOrderTimeline(row: ResultRow): OrderTimeline {
        return OrderTimeline(
            id = row[OrderTimelines.id].toString(),
            orderId = row[OrderTimelines.orderId].toString(),
            status = row[OrderTimelines.status],
            message = row[OrderTimelines.message],
            createdBy = row[OrderTimelines.createdBy]?.toString(),
            createdByRole = row[OrderTimelines.createdByRole],
            notificationSent = row[OrderTimelines.notificationSent],
            createdAt = row[OrderTimelines.createdAt]
        )
    }
}
