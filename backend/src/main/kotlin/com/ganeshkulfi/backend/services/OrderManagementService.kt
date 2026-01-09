package com.ganeshkulfi.backend.services

import com.ganeshkulfi.backend.data.models.Order
import com.ganeshkulfi.backend.data.models.OrderItem
import com.ganeshkulfi.backend.data.models.OrderStatus
import com.ganeshkulfi.backend.data.models.StatusHistory
import com.ganeshkulfi.backend.data.repository.OrderRepository
import com.ganeshkulfi.backend.data.repository.StatusHistoryRepository
import java.time.Instant

/**
 * Day 10: Order Management Service
 * Admin-specific service with full order visibility and management capabilities
 */
class OrderManagementService(
    private val orderRepository: OrderRepository,
    private val statusHistoryRepository: StatusHistoryRepository
) {
    
    /**
     * Get order with full details and complete status history (admin view)
     */
    suspend fun getOrderWithFullHistory(orderId: String): AdminOrderDetail? {
        val order = orderRepository.findById(orderId) ?: return null
        val items = orderRepository.getOrderItems(orderId)
        val history = statusHistoryRepository.getOrderHistory(orderId)
        
        return AdminOrderDetail(
            order = order,
            items = items,
            statusHistory = history
        )
    }
    
    /**
     * Get all orders with optional filters for admin dashboard
     */
    suspend fun getOrdersForDashboard(
        status: OrderStatus? = null,
        retailerId: String? = null,
        dateFrom: Instant? = null,
        dateTo: Instant? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<AdminOrderSummary> {
        val orders = if (retailerId != null) {
            orderRepository.findByRetailerId(retailerId)
        } else {
            orderRepository.findAll(status)
        }
        
        // Apply date filters if provided
        val filtered = orders
            .filter { order ->
                val matchesStatus = status == null || order.status == status
                val matchesDateFrom = dateFrom == null || order.createdAt >= dateFrom
                val matchesDateTo = dateTo == null || order.createdAt <= dateTo
                matchesStatus && matchesDateFrom && matchesDateTo
            }
            .drop(offset)
            .take(limit)
        
        return filtered.map { order ->
            val itemCount = orderRepository.getOrderItems(order.id).size
            val changeCount = statusHistoryRepository.countStatusChanges(order.id)
            
            AdminOrderSummary(
                order = order,
                itemCount = itemCount,
                statusChangeCount = changeCount.toInt()
            )
        }
    }
    
    /**
     * Cancel order by admin with reason
     * Records cancellation in status history
     */
    suspend fun cancelOrder(orderId: String, adminId: String, adminRole: String, reason: String): Boolean {
        val order = orderRepository.findById(orderId) ?: return false
        
        // Record old status before cancellation
        val oldStatus = order.status.name
        
        // Cancel the order
        val cancelled = orderRepository.cancelByAdmin(orderId, adminId, reason)
        
        if (cancelled) {
            // Record status change in history
            statusHistoryRepository.recordStatusChange(
                orderId = orderId,
                oldStatus = oldStatus,
                newStatus = OrderStatus.CANCELLED_ADMIN.name,
                changedBy = adminId,
                changedByRole = adminRole,
                reason = reason
            )
        }
        
        return cancelled
    }
    
    /**
     * Get dashboard statistics
     */
    suspend fun getDashboardStats(): DashboardStats {
        val allOrders = orderRepository.findAll()
        
        return DashboardStats(
            totalOrders = allOrders.size,
            pendingOrders = allOrders.count { it.status == OrderStatus.PENDING },
            confirmedOrders = allOrders.count { it.status == OrderStatus.CONFIRMED },
            rejectedOrders = allOrders.count { it.status == OrderStatus.REJECTED },
            cancelledOrders = allOrders.count { 
                it.status == OrderStatus.CANCELLED || it.status == OrderStatus.CANCELLED_ADMIN 
            },
            totalRevenue = allOrders
                .filter { it.status == OrderStatus.CONFIRMED }
                .sumOf { it.totalAmount }
        )
    }
}

/**
 * Admin order detail with full history
 */
data class AdminOrderDetail(
    val order: Order,
    val items: List<OrderItem>,
    val statusHistory: List<StatusHistory>
)

/**
 * Admin order summary for dashboard list view
 */
data class AdminOrderSummary(
    val order: Order,
    val itemCount: Int,
    val statusChangeCount: Int
)

/**
 * Dashboard statistics
 */
data class DashboardStats(
    val totalOrders: Int,
    val pendingOrders: Int,
    val confirmedOrders: Int,
    val rejectedOrders: Int,
    val cancelledOrders: Int,
    val totalRevenue: Double
)
