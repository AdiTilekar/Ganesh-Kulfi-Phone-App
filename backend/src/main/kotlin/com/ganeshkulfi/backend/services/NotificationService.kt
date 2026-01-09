package com.ganeshkulfi.backend.services

import org.slf4j.LoggerFactory

/**
 * Simple Notification Service (No Firebase)
 * Logs notification events for order status changes
 * Android app will poll for updates instead of push notifications
 */
class NotificationService {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)
    
    /**
     * Log notification event (Android app will poll for updates)
     */
    fun logNotification(
        userId: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        logger.info("📱 Notification logged for user $userId: $title - $body")
        logger.debug("Notification data: $data")
    }
    
    /**
     * Log order status update notification
     */
    fun logOrderStatusNotification(
        userId: String,
        orderId: String,
        orderNumber: String,
        newStatus: String
    ) {
        val (title, body) = when (newStatus.uppercase()) {
            "CONFIRMED" -> "Order Confirmed" to "Your order #$orderNumber has been confirmed by factory"
            "PACKED" -> "Order Packed" to "Your order #$orderNumber has been packed and is ready for dispatch"
            "OUT_FOR_DELIVERY" -> "Out for Delivery" to "Your order #$orderNumber is out for delivery"
            "DELIVERED" -> "Order Delivered" to "Your order #$orderNumber has been delivered successfully"
            "CANCELLED" -> "Order Cancelled" to "Your order #$orderNumber has been cancelled"
            else -> "Order Update" to "Your order #$orderNumber status: $newStatus"
        }
        
        val data = mapOf(
            "type" to "order_status_update",
            "orderId" to orderId,
            "orderNumber" to orderNumber,
            "status" to newStatus
        )
        
        logNotification(userId, title, body, data)
    }
}
