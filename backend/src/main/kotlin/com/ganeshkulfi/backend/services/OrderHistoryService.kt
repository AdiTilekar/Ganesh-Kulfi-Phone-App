package com.ganeshkulfi.backend.services

import com.ganeshkulfi.backend.data.models.Order
import com.ganeshkulfi.backend.data.models.OrderItem
import com.ganeshkulfi.backend.data.models.OrderStatus
import com.ganeshkulfi.backend.data.models.OrderTimelineEvent
import com.ganeshkulfi.backend.data.repository.OrderRepository
import com.ganeshkulfi.backend.data.repository.StatusHistoryRepository

/**
 * Day 10: Order History Service
 * Provides retailer-safe views of order history with timeline events
 * Hides internal pricing details and admin-only information
 */
class OrderHistoryService(
    private val orderRepository: OrderRepository,
    private val statusHistoryRepository: StatusHistoryRepository
) {
    
    /**
     * Get order with timeline for retailer view
     * Returns only retailer-visible events (ORDER_PLACED, ORDER_CONFIRMED, ORDER_REJECTED, ORDER_CANCELLED)
     */
    suspend fun getOrderWithTimeline(orderId: String, retailerId: String): OrderWithTimeline? {
        val order = orderRepository.findById(orderId) ?: return null
        
        // Verify order belongs to retailer
        if (order.retailerId != retailerId) {
            return null
        }
        
        val items = orderRepository.getOrderItems(orderId)
        val timeline = statusHistoryRepository.getOrderHistory(orderId)
            .map { history ->
                // Convert to retailer-safe timeline
                OrderTimelineEvent(
                    event = history.newStatus,
                    timestamp = history.createdAt,
                    message = when (history.newStatus) {
                        "PENDING" -> "Order placed"
                        "CONFIRMED" -> "Order confirmed"
                        "REJECTED" -> "Order rejected"
                        "CANCELLED" -> "Order cancelled"
                        "CANCELLED_ADMIN" -> "Order cancelled"
                        else -> null
                    }
                )
            }
        
        return OrderWithTimeline(
            order = order,
            items = items,
            timeline = timeline
        )
    }
    
    /**
     * Get order history for retailer with limited visibility
     */
    suspend fun getRetailerOrders(
        retailerId: String,
        status: OrderStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<OrderWithTimeline> {
        val orders = orderRepository.findByRetailerId(retailerId)
            .filter { status == null || it.status == status }
            .drop(offset)
            .take(limit)
        
        return orders.map { order ->
            val items = orderRepository.getOrderItems(order.id)
            val timeline = statusHistoryRepository.getOrderHistory(order.id)
                .map { history ->
                    OrderTimelineEvent(
                        event = history.newStatus,
                        timestamp = history.createdAt,
                        message = when (history.newStatus) {
                            "PENDING" -> "Order placed"
                            "CONFIRMED" -> "Order confirmed"
                            "REJECTED" -> "Order rejected"
                            "CANCELLED" -> "Order cancelled"
                            "CANCELLED_ADMIN" -> "Order cancelled"
                            else -> null
                        }
                    )
                }
            
            OrderWithTimeline(
                order = order,
                items = items,
                timeline = timeline
            )
        }
    }
}

/**
 * Order with timeline for retailer view
 */
data class OrderWithTimeline(
    val order: Order,
    val items: List<OrderItem>,
    val timeline: List<OrderTimelineEvent>
)
