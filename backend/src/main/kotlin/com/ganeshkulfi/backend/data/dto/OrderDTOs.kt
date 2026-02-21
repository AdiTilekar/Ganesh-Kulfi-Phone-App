package com.ganeshkulfi.backend.data.dto

import com.ganeshkulfi.backend.data.models.Order
import com.ganeshkulfi.backend.data.models.OrderItem
import com.ganeshkulfi.backend.data.models.OrderStatus
import com.ganeshkulfi.backend.data.models.PaymentStatus
import kotlinx.serialization.Serializable

/**
 * Create Order Request
 */
@Serializable
data class CreateOrderRequest(
    val items: List<OrderItemRequest>,
    val retailerNotes: String? = null
)

@Serializable
data class OrderItemRequest(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val discountPercent: Double = 0.0
)

/**
 * Update Order Status Request
 */
@Serializable
data class UpdateOrderStatusRequest(
    val status: String,  // CONFIRMED, REJECTED, COMPLETED, CANCELLED
    val factoryNotes: String? = null,
    val rejectionReason: String? = null
)

/**
 * Admin Status Update Request (Day 8)
 */
@Serializable
data class AdminStatusUpdateRequest(
    val status: String,  // CONFIRMED or REJECTED
    val message: String? = null  // Optional confirmation/rejection message to retailer
)

/**
 * Day 10: Retailer Cancel Order Request
 */
@Serializable
data class RetailerCancelRequest(
    val reason: String? = null  // Optional cancellation reason
)

/**
 * Day 10: Admin Cancel Order Request
 */
@Serializable
data class AdminCancelRequest(
    val reason: String  // Required cancellation reason for admin
)

/**
 * Day 10: Cancel Order Response
 */
@Serializable
data class CancelOrderResponse(
    val orderId: String,
    val orderNumber: String,
    val status: String,  // CANCELLED or CANCELLED_ADMIN
    val message: String,
    val cancelledAt: String,
    val cancelledBy: String
)

/**
 * Order Response
 */
@Serializable
data class OrderResponse(
    val id: String,
    val orderNumber: String,
    val retailerId: String,
    val retailerEmail: String,
    val retailerName: String,
    val shopName: String? = null,
    
    val totalItems: Int,
    val totalQuantity: Int,
    val subtotal: Double,
    val discount: Double,
    val tax: Double,
    val totalAmount: Double,
    
    val status: String,
    val paymentStatus: String,
    
    val retailerNotes: String? = null,
    val factoryNotes: String? = null,
    val rejectionReason: String? = null,
    val confirmationMessage: String? = null,
    
    val createdAt: String,
    val updatedAt: String,
    val confirmedAt: String? = null,
    val rejectedAt: String? = null,
    val completedAt: String? = null,
    val cancelledAt: String? = null,
    
    val confirmedBy: String? = null,
    val rejectedBy: String? = null,
    val cancelledBy: String? = null,
    
    val items: List<OrderItemResponse>
) {
    companion object {
        fun fromOrder(order: Order): OrderResponse {
            return OrderResponse(
                id = order.id,
                orderNumber = order.orderNumber,
                retailerId = order.retailerId,
                retailerEmail = order.retailerEmail,
                retailerName = order.retailerName,
                shopName = order.shopName,
                totalItems = order.totalItems,
                totalQuantity = order.totalQuantity,
                subtotal = order.subtotal,
                discount = order.discount,
                tax = order.tax,
                totalAmount = order.totalAmount,
                status = order.status.name,
                paymentStatus = order.paymentStatus.name,
                retailerNotes = order.retailerNotes,
                factoryNotes = order.factoryNotes,
                rejectionReason = order.rejectionReason,
                confirmationMessage = order.confirmationMessage,
                createdAt = order.createdAt.toString(),
                updatedAt = order.updatedAt.toString(),
                confirmedAt = order.confirmedAt?.toString(),
                rejectedAt = order.rejectedAt?.toString(),
                completedAt = order.completedAt?.toString(),
                cancelledAt = order.cancelledAt?.toString(),
                confirmedBy = order.confirmedBy,
                rejectedBy = order.rejectedBy,
                cancelledBy = order.cancelledBy,
                items = order.items.map { OrderItemResponse.fromOrderItem(it) }
            )
        }
    }
}

@Serializable
data class OrderItemResponse(
    val id: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val discountPercent: Double,
    val discountAmount: Double,
    val lineTotal: Double
) {
    companion object {
        fun fromOrderItem(item: OrderItem): OrderItemResponse {
            return OrderItemResponse(
                id = item.id,
                productId = item.productId,
                productName = item.productName,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                discountPercent = item.discountPercent,
                discountAmount = item.discountAmount,
                lineTotal = item.lineTotal
            )
        }
    }
}

@Serializable
data class OrdersListResponse(
    val orders: List<OrderResponse>,
    val total: Int
)

/**
 * Day 10: Admin Dashboard Order with Items
 */
@Serializable
data class AdminOrderWithItems(
    val order: OrderResponse,
    val items: List<OrderItemResponse>,
    val itemCount: Int
)

/**
 * Day 10: Admin Dashboard Response
 */
@Serializable
data class AdminDashboardResponse(
    val orders: List<AdminOrderWithItems>,
    val totalCount: Int,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int
)

/**
 * Day 10: Admin Dashboard API Response
 */
@Serializable
data class AdminDashboardApiResponse(
    val success: Boolean,
    val message: String,
    val data: AdminDashboardResponse
)
