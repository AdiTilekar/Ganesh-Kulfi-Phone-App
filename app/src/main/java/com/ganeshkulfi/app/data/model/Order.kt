package com.ganeshkulfi.app.data.model

data class Order(
    val id: String = "",
    val userId: String = "",
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: OrderStatus = OrderStatus.PENDING,
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val shopName: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class OrderItem(
    val flavorId: String = "",
    val flavorName: String = "",
    val quantity: Int = 0,
    val basePrice: Double = 0.0,
    val discountedPrice: Double = 0.0,
    val subtotal: Double = 0.0
)

enum class OrderStatus {
    PENDING,          // Order placed by retailer, awaiting factory confirmation
    CONFIRMED,        // Factory confirmed the order
    PACKED,           // Order has been packed
    OUT_FOR_DELIVERY, // Order is out for delivery
    DELIVERED,        // Order delivered
    REJECTED,         // Factory rejected the order
    COMPLETED,        // Order fulfilled and delivered (alias for DELIVERED)
    CANCELLED,        // Order cancelled by retailer
    CANCELLED_ADMIN,  // Order cancelled by admin
    
    // Legacy statuses for backward compatibility — backend no longer produces these
    @Deprecated("Backend never sends PREPARING; kept only for old cached data")
    PREPARING,
    @Deprecated("Backend never sends READY; kept only for old cached data")
    READY,
    @Deprecated("Backend never sends DISPATCHED; kept only for old cached data")
    DISPATCHED
}
