package com.ganeshkulfi.backend.services

import com.ganeshkulfi.backend.data.dto.*
import com.ganeshkulfi.backend.data.models.OrderStatus
import com.ganeshkulfi.backend.data.models.Products
import com.ganeshkulfi.backend.data.repository.OrderRepository
import com.ganeshkulfi.backend.data.repository.ProductRepository
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Order Service
 * Business logic for order management with inventory integration
 * Day 9: Enhanced with PricingService for tier-based pricing
 * Auto stock reduction when orders are confirmed/delivered
 */
class OrderService(
    private val orderRepository: OrderRepository,
    private val inventoryService: InventoryService,
    private val pricingService: PricingService,
    private val productRepository: ProductRepository
) {
    
    /**
     * Create new order with idempotency support and validation (Retailer)
     * Day 8: Enhanced with server-side validation, stock checks, and idempotency
     */
    fun createOrder(
        retailerId: String,
        retailerEmail: String,
        retailerName: String,
        shopName: String?,
        request: CreateOrderRequest,
        idempotencyKey: String?
    ): Result<OrderResponse> {
        return try {
            // Check idempotency first
            if (idempotencyKey != null) {
                val existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey)
                if (existingOrder != null) {
                    // Return existing order - prevents duplicate creation
                    return Result.success(OrderResponse.fromOrder(existingOrder))
                }
            }
            
            // Validate items
            if (request.items.isEmpty()) {
                return Result.failure(Exception("Order must contain at least one item"))
            }
            
            // Day 9: Validate and calculate prices using PricingService
            val validatedItems = mutableListOf<Triple<String, Int, Double>>()
            
            for (item in request.items) {
                // Validate quantity
                if (item.quantity <= 0) {
                    return Result.failure(Exception("Quantity must be positive for ${item.productName}"))
                }
                
                // Get product from database for server-side validation
                val productValidation = transaction {
                    Products.select { Products.id eq item.productId }
                        .singleOrNull()
                        ?.let {
                            Triple(
                                it[Products.name],
                                it[Products.isAvailable],
                                item.productId
                            )
                        }
                }
                
                if (productValidation == null) {
                    return Result.failure(Exception("Product not found: ${item.productName}"))
                }
                
                val (productName, isAvailable, productId) = productValidation
                
                // Validate product is available
                if (!isAvailable) {
                    return Result.failure(Exception("Product '$productName' is currently unavailable"))
                }
                
                // Day 9: Calculate tier-based price using PricingService
                val priceCalculation = try {
                    pricingService.calculatePrice(
                        productId = productId,  // String (UUID)
                        retailerId = retailerId,
                        quantity = item.quantity
                    )
                } catch (e: Exception) {
                    return Result.failure(Exception("Failed to calculate price for ${item.productName}: ${e.message}"))
                }
                
                // Use final price from PricingService (includes tier pricing, quantity discounts, GST)
                validatedItems.add(Triple(item.productId, item.quantity, priceCalculation.unitPriceFinal))
            }
            
            // Create order with transaction safety
            val order = orderRepository.createWithStockDeduction(
                retailerId = retailerId,
                retailerEmail = retailerEmail,
                retailerName = retailerName,
                shopName = shopName,
                items = validatedItems,
                retailerNotes = request.retailerNotes,
                idempotencyKey = idempotencyKey
            )
            
            Result.success(OrderResponse.fromOrder(order))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get order by ID
     */
    fun getOrderById(orderId: String, requestingUserId: String, isAdmin: Boolean): Result<OrderResponse> {
        return try {
            val order = orderRepository.findById(orderId)
                ?: return Result.failure(Exception("Order not found"))
            
            // Check access: admin can view all, retailer can only view their own
            if (!isAdmin && order.retailerId != requestingUserId) {
                return Result.failure(Exception("Access denied"))
            }
            
            Result.success(OrderResponse.fromOrder(order))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get order by order number
     */
    fun getOrderByNumber(orderNumber: String, requestingUserId: String, isAdmin: Boolean): Result<OrderResponse> {
        return try {
            val order = orderRepository.findByOrderNumber(orderNumber)
                ?: return Result.failure(Exception("Order not found"))
            
            // Check access
            if (!isAdmin && order.retailerId != requestingUserId) {
                return Result.failure(Exception("Access denied"))
            }
            
            Result.success(OrderResponse.fromOrder(order))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get retailer's orders
     */
    fun getRetailerOrders(retailerId: String): Result<List<OrderResponse>> {
        return try {
            val orders = orderRepository.findByRetailerId(retailerId)
            Result.success(orders.map { OrderResponse.fromOrder(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all orders (Factory Owner)
     */
    fun getAllOrders(statusFilter: String? = null): Result<List<OrderResponse>> {
        return try {
            val status = statusFilter?.let { 
                try {
                    OrderStatus.valueOf(it.uppercase())
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            
            val orders = orderRepository.findAll(status)
            Result.success(orders.map { OrderResponse.fromOrder(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update order status (Factory Owner) - With automatic stock reduction
     * When order is confirmed or delivered, stock is automatically reduced
     */
    fun updateOrderStatus(
        orderId: String,
        request: UpdateOrderStatusRequest,
        updatedBy: String
    ): Result<OrderResponse> {
        return try {
            // Get the order with its items
            val order = orderRepository.findById(orderId)
                ?: return Result.failure(Exception("Order not found"))
            
            val currentStatus = order.status
            
            // Validate status
            val newStatus = try {
                OrderStatus.valueOf(request.status.uppercase())
            } catch (e: IllegalArgumentException) {
                return Result.failure(Exception("Invalid status: ${request.status}"))
            }
            
            // Validate rejection reason for rejected orders
            if (newStatus == OrderStatus.REJECTED && request.rejectionReason.isNullOrBlank()) {
                return Result.failure(Exception("Rejection reason is required"))
            }
            
            // Automatic stock reduction when confirming or delivering order
            // Only reduce stock when transitioning TO confirmed/delivered (not if already in that state)
            if ((newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.DELIVERED) && 
                currentStatus != OrderStatus.CONFIRMED && currentStatus != OrderStatus.DELIVERED) {
                
                
                // Get order items
                val orderItems = orderRepository.getOrderItems(orderId)
                
                // Reduce stock for each item
                orderItems.forEach { item ->
                    try {
                        val product = productRepository.findById(item.productId)
                        if (product != null) {
                            val newStock = (product.stockQuantity - item.quantity).coerceAtLeast(0)
                            productRepository.updateProductStock(item.productId, newStock)
                        } else {
                        }
                    } catch (e: Exception) {
                    }
                }
            }
            
            // Update order status in database
            val updatedOrder = orderRepository.updateStatus(
                orderId = orderId,
                newStatus = newStatus,
                updatedBy = updatedBy,
                factoryNotes = request.factoryNotes,
                rejectionReason = request.rejectionReason
            ) ?: return Result.failure(Exception("Failed to update order"))
            
            Result.success(OrderResponse.fromOrder(updatedOrder))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get pending orders count (for dashboard)
     */
    fun getPendingOrdersCount(): Result<Int> {
        return try {
            val orders = orderRepository.findAll(OrderStatus.PENDING)
            Result.success(orders.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update order status by admin with confirmation message (Day 8)
     */
    fun updateOrderStatusByAdmin(
        orderId: String,
        newStatus: String,
        message: String?,
        adminId: String
    ): Result<OrderResponse> {
        return try {
            // Check if order exists
            val existingOrder = orderRepository.findById(orderId)
                ?: return Result.failure(Exception("Order not found"))
            
            // Validate status
            val orderStatus = try {
                OrderStatus.valueOf(newStatus.uppercase())
            } catch (e: IllegalArgumentException) {
                return Result.failure(Exception("Invalid status. Must be CONFIRMED or REJECTED"))
            }
            
            // Only allow PENDING -> CONFIRMED or PENDING -> REJECTED
            if (existingOrder.status != OrderStatus.PENDING) {
                return Result.failure(Exception("Can only update PENDING orders. Current status: ${existingOrder.status}"))
            }
            
            if (orderStatus != OrderStatus.CONFIRMED && orderStatus != OrderStatus.REJECTED) {
                return Result.failure(Exception("Can only set status to CONFIRMED or REJECTED"))
            }
            
            // Update with confirmation message
            val updatedOrder = orderRepository.updateStatusWithMessage(
                orderId = orderId,
                newStatus = orderStatus,
                updatedBy = adminId,
                confirmationMessage = message
            ) ?: return Result.failure(Exception("Failed to update order"))
            
            Result.success(OrderResponse.fromOrder(updatedOrder))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get retailer order history with pagination and filters
     */
    fun getRetailerOrderHistory(
        retailerId: String,
        filters: OrderFilters
    ): Result<PaginatedResponse<OrderHistoryResponse>> {
        return try {
            // Validate filters
            if (filters.page < 1) {
                return Result.failure(Exception("Page must be greater than 0"))
            }
            
            if (filters.pageSize < 1 || filters.pageSize > 100) {
                return Result.failure(Exception("Page size must be between 1 and 100"))
            }
            
            // Parse date filters if provided
            val dateFrom = filters.dateFrom?.let { 
                try {
                    java.time.Instant.parse(it)
                } catch (e: Exception) {
                    return Result.failure(Exception("Invalid dateFrom format. Use ISO-8601 format"))
                }
            }
            
            val dateTo = filters.dateTo?.let { 
                try {
                    java.time.Instant.parse(it)
                } catch (e: Exception) {
                    return Result.failure(Exception("Invalid dateTo format. Use ISO-8601 format"))
                }
            }
            
            // Validate status if provided
            if (filters.status != null) {
                try {
                    OrderStatus.valueOf(filters.status.uppercase())
                } catch (e: IllegalArgumentException) {
                    return Result.failure(Exception("Invalid status. Must be one of: PENDING, CONFIRMED, REJECTED, COMPLETED, CANCELLED"))
                }
            }
            
            // Get orders with pagination
            val (orders, totalCount) = orderRepository.getOrdersByRetailer(
                retailerId = retailerId,
                status = filters.status,
                dateFrom = dateFrom,
                dateTo = dateTo,
                minTotal = filters.minTotal,
                maxTotal = filters.maxTotal,
                sortBy = filters.sortBy,
                order = filters.order,
                page = filters.page,
                pageSize = filters.pageSize
            )
            
            // Convert to history response (simplified view for retailers)
            val historyResponses = orders.map { order ->
                OrderHistoryResponse(
                    id = order.id,
                    orderNumber = order.orderNumber,
                    status = order.status.name,
                    totalItems = order.totalItems,
                    totalQuantity = order.totalQuantity,
                    totalAmount = order.totalAmount,
                    createdAt = order.createdAt.toString(),
                    updatedAt = order.updatedAt.toString(),
                    confirmedAt = order.confirmedAt?.toString(),
                    rejectedAt = order.rejectedAt?.toString(),
                    factoryNotes = order.factoryNotes,
                    rejectionReason = order.rejectionReason
                )
            }
            
            val totalPages = (totalCount + filters.pageSize - 1) / filters.pageSize
            
            val paginatedResponse = PaginatedResponse(
                data = historyResponses,
                totalCount = totalCount,
                totalPages = totalPages,
                currentPage = filters.page,
                pageSize = filters.pageSize
            )
            
            Result.success(paginatedResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Day 10: Cancel order by retailer (PENDING only)
     */
    fun cancelOrderByRetailer(orderId: String, retailerId: String, reason: String?): Result<CancelOrderResponse> {
        return try {
            val order = orderRepository.findById(orderId)
                ?: return Result.failure(Exception("Order not found"))
            
            if (order.retailerId != retailerId) {
                return Result.failure(Exception("Unauthorized: Order belongs to different retailer"))
            }
            
            if (order.status != OrderStatus.PENDING) {
                return Result.failure(Exception("Only PENDING orders can be cancelled. Current status: ${order.status}"))
            }
            
            val cancelled = orderRepository.cancelByRetailer(orderId, retailerId, reason)
            if (!cancelled) {
                return Result.failure(Exception("Failed to cancel order"))
            }
            
            val updatedOrder = orderRepository.findById(orderId)!!
            
            Result.success(
                CancelOrderResponse(
                    orderId = updatedOrder.id,
                    orderNumber = updatedOrder.orderNumber,
                    status = updatedOrder.status.name,
                    message = "Order cancelled successfully",
                    cancelledAt = updatedOrder.cancelledAt.toString(),
                    cancelledBy = updatedOrder.cancelledBy ?: "N/A"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Day 10: Cancel order by admin (any status)
     */
    fun cancelOrderByAdmin(orderId: String, adminId: String, adminRole: String, reason: String): Result<CancelOrderResponse> {
        return try {
            val order = orderRepository.findById(orderId)
                ?: return Result.failure(Exception("Order not found"))
            
            val cancelled = orderRepository.cancelByAdmin(orderId, adminId, reason)
            if (!cancelled) {
                return Result.failure(Exception("Failed to cancel order"))
            }
            
            val updatedOrder = orderRepository.findById(orderId)!!
            
            Result.success(
                CancelOrderResponse(
                    orderId = updatedOrder.id,
                    orderNumber = updatedOrder.orderNumber,
                    status = updatedOrder.status.name,
                    message = "Order cancelled by admin",
                    cancelledAt = updatedOrder.cancelledAt.toString(),
                    cancelledBy = updatedOrder.cancelledBy ?: "N/A"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Day 10: Get orders for admin dashboard with filters
     */
    fun getOrdersForAdminDashboard(
        status: String?,
        retailerId: String?,
        dateFrom: String?,
        dateTo: String?,
        page: Int,
        pageSize: Int
    ): Result<AdminDashboardResponse> {
        return try {
            // Parse status if provided
            val orderStatus = status?.let { 
                try {
                    OrderStatus.valueOf(it.uppercase())
                } catch (e: Exception) {
                    return Result.failure(Exception("Invalid status: $status"))
                }
            }
            
            // Get orders
            val orders = if (retailerId != null) {
                orderRepository.findByRetailerId(retailerId)
            } else {
                orderRepository.findAll(orderStatus)
            }
            
            val filtered = orders
                .filter { orderStatus == null || it.status == orderStatus }
                .drop((page - 1) * pageSize)
                .take(pageSize)
            
            val orderResponses = filtered.map { order ->
                val items = orderRepository.getOrderItems(order.id)
                AdminOrderWithItems(
                    order = OrderResponse.fromOrder(order),
                    items = items.map { OrderItemResponse.fromOrderItem(it) },
                    itemCount = items.size
                )
            }
            
            val totalCount = orders.size
            val totalPages = (totalCount + pageSize - 1) / pageSize
            
            Result.success(
                AdminDashboardResponse(
                    orders = orderResponses,
                    totalCount = totalCount,
                    totalPages = totalPages,
                    currentPage = page,
                    pageSize = pageSize
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Day 10: Get order with full history for admin
     */
    fun getOrderWithFullHistory(orderId: String): Result<Map<String, Any>> {
        return try {
            val order = orderRepository.findById(orderId)
                ?: return Result.failure(Exception("Order not found"))
            
            val items = orderRepository.getOrderItems(orderId)
            
            Result.success(
                mapOf(
                    "order" to OrderResponse.fromOrder(order),
                    "items" to items,
                    "statusHistory" to emptyList<Any>() // Will be populated by OrderManagementService
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
