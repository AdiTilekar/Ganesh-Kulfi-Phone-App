package com.ganeshkulfi.backend.data.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.Column
import org.postgresql.util.PGobject
import java.time.Instant
import java.util.*

/**
 * Order Status (Day 10: Added CANCELLED_ADMIN)
 */
enum class OrderStatus {
    PENDING,          // Order placed by retailer, awaiting factory confirmation
    CONFIRMED,        // Factory confirmed the order
    PACKED,           // Order has been packed
    OUT_FOR_DELIVERY, // Order is out for delivery
    DELIVERED,        // Order delivered
    REJECTED,         // Factory rejected the order
    COMPLETED,        // Order fulfilled and delivered
    CANCELLED,        // Order cancelled by retailer
    CANCELLED_ADMIN   // Order cancelled by admin (for operational reasons)
}

/**
 * Payment Status
 */
enum class PaymentStatus {
    UNPAID,       // No payment made
    PARTIAL,      // Partial payment received
    PAID          // Fully paid
}

/**
 * Orders Table - Exposed ORM
 */
object Orders : UUIDTable("orders") {
    val orderNumber = varchar("order_number", 50).uniqueIndex()
    val retailerId = reference("retailer_id", Users)
    val retailerEmail = varchar("retailer_email", 255)
    val retailerName = varchar("retailer_name", 255)
    val shopName = varchar("shop_name", 255).nullable()
    
    // Order details
    val totalItems = integer("total_items").default(0)
    val totalQuantity = integer("total_quantity").default(0)
    val subtotal = decimal("subtotal", 10, 2).default(0.toBigDecimal())
    val discount = decimal("discount", 10, 2).default(0.toBigDecimal())
    val tax = decimal("tax", 10, 2).default(0.toBigDecimal())
    val totalAmount = decimal("total_amount", 10, 2).default(0.toBigDecimal())
    
    // Status tracking
    val status = customEnumeration(
        "status",
        "order_status",
        { value -> OrderStatus.valueOf(value as String) },
        { PGobject().apply { type = "order_status"; value = it.name } }
    ).default(OrderStatus.PENDING)
    val paymentStatus = customEnumeration(
        "payment_status",
        "payment_status",
        { value -> PaymentStatus.valueOf(value as String) },
        { PGobject().apply { type = "payment_status"; value = it.name } }
    ).default(PaymentStatus.UNPAID)
    
    // Notes
    val retailerNotes = text("retailer_notes").nullable()
    val factoryNotes = text("factory_notes").nullable()
    val rejectionReason = text("rejection_reason").nullable()
    val confirmationMessage = text("confirmation_message").nullable()
    val cancellationReason = text("cancellation_reason").nullable()  // Day 10
    
    // Idempotency
    val idempotencyKey = varchar("idempotency_key", 255).nullable().uniqueIndex()
    
    // Day 9: Server-side only pricing breakdown (NEVER exposed to retailers)
    val basePrice = decimal("base_price", 10, 2).nullable()
    val overridePrice = decimal("override_price", 10, 2).nullable()
    val discountPercentage = decimal("discount_percentage", 5, 2).default(0.toBigDecimal())
    val gstPercentage = decimal("gst_percentage", 5, 2).default(18.toBigDecimal())
    
    // Timestamps
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())
    val confirmedAt = timestamp("confirmed_at").nullable()
    val rejectedAt = timestamp("rejected_at").nullable()
    val completedAt = timestamp("completed_at").nullable()
    val cancelledAt = timestamp("cancelled_at").nullable()  // Day 10
    
    // Who updated
    val confirmedBy = reference("confirmed_by", Users).nullable()
    val rejectedBy = reference("rejected_by", Users).nullable()
    val cancelledBy = reference("cancelled_by", Users).nullable()  // Day 10
}

/**
 * Order Items Table - Exposed ORM
 */
object OrderItems : UUIDTable("order_items") {
    val orderId = reference("order_id", Orders)
    val productId = varchar("product_id", 36)
    val productName = varchar("product_name", 100)
    
    // Quantity and pricing
    val quantity = integer("quantity")
    val unitPrice = decimal("unit_price", 10, 2)
    val discountPercent = decimal("discount_percent", 5, 2).default(0.toBigDecimal())
    val discountAmount = decimal("discount_amount", 10, 2).default(0.toBigDecimal())
    val lineTotal = decimal("line_total", 10, 2)
    
    // Metadata
    val createdAt = timestamp("created_at").default(Instant.now())
}

/**
 * Order Domain Model
 */
data class Order(
    val id: String = UUID.randomUUID().toString(),
    val orderNumber: String,
    val retailerId: String,
    val retailerEmail: String,
    val retailerName: String,
    val shopName: String? = null,
    
    // Order details
    val totalItems: Int = 0,
    val totalQuantity: Int = 0,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val totalAmount: Double = 0.0,
    
    // Status
    val status: OrderStatus = OrderStatus.PENDING,
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    
    // Notes
    val retailerNotes: String? = null,
    val factoryNotes: String? = null,
    val rejectionReason: String? = null,
    val confirmationMessage: String? = null,
    val cancellationReason: String? = null,  // Day 10
    
    // Idempotency
    val idempotencyKey: String? = null,
    
    // Day 9: Server-side only pricing breakdown (NEVER exposed to retailers)
    val basePrice: Double? = null,
    val overridePrice: Double? = null,
    val discountPercentage: Double = 0.0,
    val gstPercentage: Double = 18.0,
    
    // Timestamps
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val confirmedAt: Instant? = null,
    val rejectedAt: Instant? = null,
    val completedAt: Instant? = null,
    val cancelledAt: Instant? = null,  // Day 10
    
    // Who updated
    val confirmedBy: String? = null,
    val rejectedBy: String? = null,
    val cancelledBy: String? = null,  // Day 10
    
    // Items (populated separately)
    val items: List<OrderItem> = emptyList()
)

/**
 * Order Item Domain Model
 */
data class OrderItem(
    val id: String = UUID.randomUUID().toString(),
    val orderId: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val lineTotal: Double,
    val createdAt: Instant = Instant.now()
)
