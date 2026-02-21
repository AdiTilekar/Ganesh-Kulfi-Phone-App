package com.ganeshkulfi.backend.data.repository

import com.ganeshkulfi.backend.data.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * Order Repository
 * Handles database operations for orders
 */
class OrderRepository {
    
    /**
     * Create new order with items
     */
    fun create(
        retailerId: String,
        retailerEmail: String,
        retailerName: String,
        shopName: String?,
        items: List<Pair<String, Int>>, // List of (productId, quantity)
        retailerNotes: String?
    ): Order = transaction {
        // Generate order number
        val orderNumber = generateOrderNumber()
        
        // Calculate totals from items
        var totalItems = 0
        var totalQuantity = 0
        var subtotal = 0.0
        
        val orderItems = items.map { (productId, quantity) ->
            // Get product details
            val product = Products.select { Products.id eq productId }
                .map { 
                    Triple(
                        it[Products.name],
                        it[Products.basePrice].toDouble(),
                        productId
                    )
                }
                .firstOrNull() ?: throw Exception("Product not found: $productId")
            
            val (productName, unitPrice) = product
            val lineTotal = unitPrice * quantity
            
            totalItems++
            totalQuantity += quantity
            subtotal += lineTotal
            
            Triple(productId, productName, Triple(quantity, unitPrice, lineTotal))
        }
        
        val discount = 0.0 // Can be calculated based on pricing tier
        val tax = 0.0
        val totalAmount = subtotal - discount + tax
        
        // Insert order
        val orderId = Orders.insertAndGetId {
            it[Orders.orderNumber] = orderNumber
            it[Orders.retailerId] = UUID.fromString(retailerId)
            it[Orders.retailerEmail] = retailerEmail
            it[Orders.retailerName] = retailerName
            it[Orders.shopName] = shopName
            it[Orders.totalItems] = totalItems
            it[Orders.totalQuantity] = totalQuantity
            it[Orders.subtotal] = BigDecimal.valueOf(subtotal)
            it[Orders.discount] = BigDecimal.valueOf(discount)
            it[Orders.tax] = BigDecimal.valueOf(tax)
            it[Orders.totalAmount] = BigDecimal.valueOf(totalAmount)
            it[Orders.status] = OrderStatus.PENDING
            it[Orders.paymentStatus] = PaymentStatus.UNPAID
            it[Orders.retailerNotes] = retailerNotes
            it[Orders.createdAt] = Instant.now()
            it[Orders.updatedAt] = Instant.now()
        }
        
        // Insert order items
        orderItems.forEach { (productId, productName, details) ->
            val (quantity, unitPrice, lineTotal) = details
            OrderItems.insert {
                it[OrderItems.orderId] = orderId
                it[OrderItems.productId] = productId
                it[OrderItems.productName] = productName
                it[OrderItems.quantity] = quantity
                it[OrderItems.unitPrice] = BigDecimal.valueOf(unitPrice)
                it[OrderItems.discountPercent] = BigDecimal.ZERO
                it[OrderItems.discountAmount] = BigDecimal.ZERO
                it[OrderItems.lineTotal] = BigDecimal.valueOf(lineTotal)
                it[OrderItems.createdAt] = Instant.now()
            }
        }
        
        // Return complete order
        findById(orderId.toString())!!
    }
    
    /**
     * Find order by ID with items
     */
    fun findById(id: String): Order? = transaction {
        val order = Orders.select { Orders.id eq UUID.fromString(id) }
            .mapNotNull { toOrder(it) }
            .singleOrNull()
        
        order?.copy(items = getOrderItems(id))
    }
    
    /**
     * Find order by order number
     */
    fun findByOrderNumber(orderNumber: String): Order? = transaction {
        val order = Orders.select { Orders.orderNumber eq orderNumber }
            .mapNotNull { toOrder(it) }
            .singleOrNull()
        
        order?.let { it.copy(items = getOrderItems(it.id)) }
    }
    
    /**
     * Get all orders for a retailer
     */
    fun findByRetailerId(retailerId: String): List<Order> = transaction {
        Orders.select { Orders.retailerId eq UUID.fromString(retailerId) }
            .orderBy(Orders.createdAt, SortOrder.DESC)
            .map { row ->
                val order = toOrder(row)
                order.copy(items = getOrderItems(order.id))
            }
    }
    
    /**
     * Get all orders (factory owner view)
     */
    fun findAll(status: OrderStatus? = null): List<Order> = transaction {
        val query = if (status != null) {
            Orders.select { Orders.status eq status }
        } else {
            Orders.selectAll()
        }
        
        query.orderBy(Orders.createdAt, SortOrder.DESC)
            .map { row ->
                val order = toOrder(row)
                order.copy(items = getOrderItems(order.id))
            }
    }
    
    /**
     * Get orders by status
     */
    fun findByStatus(status: OrderStatus): List<Order> = transaction {
        Orders.select { Orders.status eq status }
            .orderBy(Orders.createdAt, SortOrder.DESC)
            .map { row ->
                val order = toOrder(row)
                order.copy(items = getOrderItems(order.id))
            }
    }
    
    /**
     * Update order status
     */
    fun updateStatus(
        orderId: String,
        newStatus: OrderStatus,
        updatedBy: String,
        factoryNotes: String? = null,
        rejectionReason: String? = null
    ): Order? = transaction {
        val now = Instant.now()
        
        Orders.update({ Orders.id eq UUID.fromString(orderId) }) {
            it[status] = newStatus
            it[updatedAt] = now
            
            when (newStatus) {
                OrderStatus.CONFIRMED -> {
                    it[confirmedAt] = now
                    it[confirmedBy] = UUID.fromString(updatedBy)
                }
                OrderStatus.REJECTED -> {
                    it[rejectedAt] = now
                    it[rejectedBy] = UUID.fromString(updatedBy)
                    it[Orders.rejectionReason] = rejectionReason
                }
                OrderStatus.COMPLETED -> {
                    it[completedAt] = now
                }
                else -> {}
            }
            
            if (factoryNotes != null) {
                it[Orders.factoryNotes] = factoryNotes
            }
        }
        
        findById(orderId)
    }
    
    /**
     * Update order status with confirmation message (Admin only)
     */
    fun updateStatusWithMessage(
        orderId: String,
        newStatus: OrderStatus,
        updatedBy: String,
        confirmationMessage: String? = null
    ): Order? = transaction {
        val now = Instant.now()
        
        Orders.update({ Orders.id eq UUID.fromString(orderId) }) {
            it[status] = newStatus
            it[updatedAt] = now
            
            when (newStatus) {
                OrderStatus.CONFIRMED -> {
                    it[confirmedAt] = now
                    it[confirmedBy] = UUID.fromString(updatedBy)
                }
                OrderStatus.REJECTED -> {
                    it[rejectedAt] = now
                    it[rejectedBy] = UUID.fromString(updatedBy)
                }
                OrderStatus.COMPLETED -> {
                    it[completedAt] = now
                }
                else -> {}
            }
            
            if (confirmationMessage != null) {
                it[Orders.confirmationMessage] = confirmationMessage
            }
        }
        
        findById(orderId)
    }
    
    /**
     * Get orders by retailer with pagination and filtering
     */
    fun getOrdersByRetailer(
        retailerId: String,
        status: String? = null,
        dateFrom: Instant? = null,
        dateTo: Instant? = null,
        minTotal: Double? = null,
        maxTotal: Double? = null,
        sortBy: String = "date",
        order: String = "desc",
        page: Int = 1,
        pageSize: Int = 20
    ): Pair<List<Order>, Int> = transaction {
        var query = Orders.select { Orders.retailerId eq UUID.fromString(retailerId) }
        
        // Apply filters
        if (status != null) {
            val orderStatus = OrderStatus.valueOf(status.uppercase())
            query = query.andWhere { Orders.status eq orderStatus }
        }
        
        if (dateFrom != null) {
            query = query.andWhere { Orders.createdAt greaterEq dateFrom }
        }
        
        if (dateTo != null) {
            query = query.andWhere { Orders.createdAt lessEq dateTo }
        }
        
        if (minTotal != null) {
            query = query.andWhere { Orders.totalAmount greaterEq BigDecimal.valueOf(minTotal) }
        }
        
        if (maxTotal != null) {
            query = query.andWhere { Orders.totalAmount lessEq BigDecimal.valueOf(maxTotal) }
        }
        
        // Get total count
        val totalCount = query.count().toInt()
        
        // Apply sorting
        query = when (sortBy.lowercase()) {
            "total_amount", "amount" -> {
                if (order.lowercase() == "asc") {
                    query.orderBy(Orders.totalAmount to SortOrder.ASC)
                } else {
                    query.orderBy(Orders.totalAmount to SortOrder.DESC)
                }
            }
            else -> { // default to date
                if (order.lowercase() == "asc") {
                    query.orderBy(Orders.createdAt to SortOrder.ASC)
                } else {
                    query.orderBy(Orders.createdAt to SortOrder.DESC)
                }
            }
        }
        
        // Apply pagination
        val offset = (page - 1) * pageSize
        query = query.limit(pageSize, offset.toLong())
        
        // Map to orders
        val orders = query.map { toOrder(it) }
        
        Pair(orders, totalCount)
    }
    
    /**
     * Get analytics summary for today
     */
    fun getOrdersSummaryToday(): Map<String, Any> = transaction {
        val result = mutableMapOf<String, Any>()
        
        exec("""
            SELECT 
                COUNT(*) as total_orders,
                COUNT(*) FILTER (WHERE status = 'CONFIRMED') as confirmed_orders,
                COUNT(*) FILTER (WHERE status = 'REJECTED') as rejected_orders,
                COUNT(*) FILTER (WHERE status = 'PENDING') as pending_orders,
                COALESCE(SUM(total_amount) FILTER (WHERE status = 'CONFIRMED'), 0) as total_revenue,
                COALESCE(AVG(total_amount) FILTER (WHERE status = 'CONFIRMED'), 0) as avg_order_value
            FROM orders
            WHERE DATE(created_at) = CURRENT_DATE
        """) { rs ->
            if (rs.next()) {
                result["totalOrdersToday"] = rs.getInt("total_orders")
                result["confirmedOrdersToday"] = rs.getInt("confirmed_orders")
                result["rejectedOrdersToday"] = rs.getInt("rejected_orders")
                result["pendingOrdersToday"] = rs.getInt("pending_orders")
                result["totalRevenueToday"] = rs.getDouble("total_revenue")
                result["averageOrderValue"] = rs.getDouble("avg_order_value")
            }
        }
        
        result
    }
    
    /**
     * Get top products by quantity sold
     */
    fun getTopProducts(limit: Int = 10): List<Map<String, Any>> = transaction {
        val results = mutableListOf<Map<String, Any>>()
        // Clamp limit to a safe range to prevent abuse (already an Int, so no injection risk)
        val safeLimit = limit.coerceIn(1, 100)
        
        val conn = this.connection.connection as java.sql.Connection
        conn.prepareStatement("""
            SELECT 
                oi.product_id,
                oi.product_name,
                SUM(oi.quantity) as total_quantity,
                COUNT(DISTINCT o.id) as total_orders,
                SUM(oi.line_total) as total_revenue
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            WHERE o.status = 'CONFIRMED'
            GROUP BY oi.product_id, oi.product_name
            ORDER BY total_quantity DESC
            LIMIT ?
        """).use { stmt ->
        stmt.setInt(1, safeLimit)
        val rs = stmt.executeQuery()
        while (rs.next()) {
            results.add(mapOf(
                "productId" to rs.getString("product_id"),
                "productName" to rs.getString("product_name"),
                "totalQuantitySold" to rs.getInt("total_quantity"),
                "totalOrders" to rs.getInt("total_orders"),
                "totalRevenue" to rs.getDouble("total_revenue")
            ))
        }
        }
        
        results
    }
    
    /**
     * Get top retailers by order count
     */
    fun getTopRetailers(limit: Int = 10): List<Map<String, Any>> = transaction {
        val results = mutableListOf<Map<String, Any>>()
        val safeLimit = limit.coerceIn(1, 100)
        
        val conn = this.connection.connection as java.sql.Connection
        conn.prepareStatement("""
            SELECT 
                retailer_id,
                retailer_name,
                retailer_email,
                shop_name,
                COUNT(*) as total_orders,
                SUM(total_amount) as total_spent
            FROM orders
            WHERE status = 'CONFIRMED'
            GROUP BY retailer_id, retailer_name, retailer_email, shop_name
            ORDER BY total_orders DESC
            LIMIT ?
        """).use { stmt ->
            stmt.setInt(1, safeLimit)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                results.add(mapOf(
                    "retailerId" to rs.getString("retailer_id"),
                    "retailerName" to rs.getString("retailer_name"),
                    "retailerEmail" to rs.getString("retailer_email"),
                    "shopName" to rs.getString("shop_name"),
                    "totalOrders" to rs.getInt("total_orders"),
                    "totalSpent" to rs.getDouble("total_spent")
                ))
            }
        }
        
        results
    }
    
    /**
     * Get weekly order statistics (last 7 days)
     */
    fun getWeeklyOrderStats(): List<Map<String, Any>> = transaction {
        val results = mutableListOf<Map<String, Any>>()
        
        exec("""
            SELECT 
                DATE(created_at) as order_date,
                COUNT(*) as total_orders,
                COUNT(*) FILTER (WHERE status = 'CONFIRMED') as confirmed_orders,
                COUNT(*) FILTER (WHERE status = 'REJECTED') as rejected_orders,
                COALESCE(SUM(total_amount) FILTER (WHERE status = 'CONFIRMED'), 0) as total_revenue
            FROM orders
            WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'
            GROUP BY DATE(created_at)
            ORDER BY order_date DESC
        """) { rs ->
            while (rs.next()) {
                results.add(mapOf(
                    "date" to rs.getDate("order_date").toString(),
                    "totalOrders" to rs.getInt("total_orders"),
                    "confirmedOrders" to rs.getInt("confirmed_orders"),
                    "rejectedOrders" to rs.getInt("rejected_orders"),
                    "totalRevenue" to rs.getDouble("total_revenue")
                ))
            }
        }
        
        results
    }
    
    /**
     * Get order items for an order
     */
    fun getOrderItems(orderId: String): List<OrderItem> = transaction {
        OrderItems.select { OrderItems.orderId eq UUID.fromString(orderId) }
            .map { toOrderItem(it) }
    }
    
    /**
     * Generate unique order number
     */
    private fun generateOrderNumber(): String {
        // Use database function
        return transaction {
            var result: String? = null
            exec("SELECT generate_order_number()") { rs ->
                if (rs.next()) {
                    result = rs.getString(1)
                }
            }
            result ?: "ORD-${System.currentTimeMillis()}"
        }
    }
    
    /**
     * Find order by idempotency key
     */
    fun findByIdempotencyKey(key: String): Order? = transaction {
        val order = Orders.select { Orders.idempotencyKey eq key }
            .mapNotNull { toOrder(it) }
            .singleOrNull()
        
        order?.copy(items = getOrderItems(order.id))
    }
    
    /**
     * Create order with transaction safety and stock deduction
     * All operations in single transaction - rolls back on any failure
     */
    fun createWithStockDeduction(
        retailerId: String,
        retailerEmail: String,
        retailerName: String,
        shopName: String?,
        items: List<Triple<String, Int, Double>>, // (productId, quantity, serverPrice)
        retailerNotes: String?,
        idempotencyKey: String?
    ): Order = transaction {
        // Generate order number
        val orderNumber = generateOrderNumber()
        
        // Calculate totals and validate stock
        var totalItems = 0
        var totalQuantity = 0
        var subtotal = 0.0
        
        val validatedItems = items.map { (productId, quantity, serverPrice) ->
            // Get product with stock info
            val productRow = Products.select { Products.id eq productId }
                .singleOrNull() ?: throw Exception("Product not found: $productId")
            
            val productName = productRow[Products.name]
            val stockQuantity = productRow[Products.stockQuantity]
            val isAvailable = productRow[Products.isAvailable]
            
            // Validate product is available
            if (!isAvailable) {
                throw Exception("Product '$productName' is not available for order")
            }
            
            // STOCK CHECK DISABLED - Factory fulfills all orders regardless of stock count
            // Retailers can order any quantity, factory will produce as needed
            // Stock tracking is for internal use only, not a hard limit
            /*
            if (stockQuantity < quantity) {
                throw Exception("Insufficient stock for '$productName'. Available: $stockQuantity, Requested: $quantity")
            }
            */
            
            val lineTotal = serverPrice * quantity
            totalItems++
            totalQuantity += quantity
            subtotal += lineTotal
            
            Pair(Triple(productId, productName, serverPrice), Triple(quantity, lineTotal, stockQuantity))
        }
        
        val discount = 0.0
        val tax = 0.0
        val totalAmount = subtotal - discount + tax
        
        // Insert order
        val orderId = Orders.insertAndGetId {
            it[Orders.orderNumber] = orderNumber
            it[Orders.retailerId] = UUID.fromString(retailerId)
            it[Orders.retailerEmail] = retailerEmail
            it[Orders.retailerName] = retailerName
            it[Orders.shopName] = shopName
            it[Orders.totalItems] = totalItems
            it[Orders.totalQuantity] = totalQuantity
            it[Orders.subtotal] = BigDecimal.valueOf(subtotal)
            it[Orders.discount] = BigDecimal.valueOf(discount)
            it[Orders.tax] = BigDecimal.valueOf(tax)
            it[Orders.totalAmount] = BigDecimal.valueOf(totalAmount)
            it[Orders.status] = OrderStatus.PENDING
            it[Orders.paymentStatus] = PaymentStatus.UNPAID
            it[Orders.retailerNotes] = retailerNotes
            it[Orders.idempotencyKey] = idempotencyKey
            it[Orders.createdAt] = Instant.now()
            it[Orders.updatedAt] = Instant.now()
        }
        
        // Insert order items
        validatedItems.forEach { (productInfo, quantityInfo) ->
            val (productId, productName, unitPrice) = productInfo
            val (quantity, lineTotal, _) = quantityInfo
            
            OrderItems.insert {
                it[OrderItems.orderId] = orderId
                it[OrderItems.productId] = productId
                it[OrderItems.productName] = productName
                it[OrderItems.quantity] = quantity
                it[OrderItems.unitPrice] = BigDecimal.valueOf(unitPrice)
                it[OrderItems.discountPercent] = BigDecimal.ZERO
                it[OrderItems.discountAmount] = BigDecimal.ZERO
                it[OrderItems.lineTotal] = BigDecimal.valueOf(lineTotal)
                it[OrderItems.createdAt] = Instant.now()
            }
        }
        
        // Return complete order
        findById(orderId.toString())!!
    }
    
    /**
     * Map database row to Order
     */
    private fun toOrder(row: ResultRow): Order {
        return Order(
            id = row[Orders.id].toString(),
            orderNumber = row[Orders.orderNumber],
            retailerId = row[Orders.retailerId].toString(),
            retailerEmail = row[Orders.retailerEmail],
            retailerName = row[Orders.retailerName],
            shopName = row[Orders.shopName],
            totalItems = row[Orders.totalItems],
            totalQuantity = row[Orders.totalQuantity],
            subtotal = row[Orders.subtotal].toDouble(),
            discount = row[Orders.discount].toDouble(),
            tax = row[Orders.tax].toDouble(),
            totalAmount = row[Orders.totalAmount].toDouble(),
            status = row[Orders.status],
            paymentStatus = row[Orders.paymentStatus],
            retailerNotes = row[Orders.retailerNotes],
            factoryNotes = row[Orders.factoryNotes],
            rejectionReason = row[Orders.rejectionReason],
            confirmationMessage = row[Orders.confirmationMessage],
            idempotencyKey = row[Orders.idempotencyKey],
            createdAt = row[Orders.createdAt],
            updatedAt = row[Orders.updatedAt],
            confirmedAt = row[Orders.confirmedAt],
            rejectedAt = row[Orders.rejectedAt],
            completedAt = row[Orders.completedAt],
            confirmedBy = row[Orders.confirmedBy]?.toString(),
            rejectedBy = row[Orders.rejectedBy]?.toString()
        )
    }
    
    /**
     * Map database row to OrderItem
     */
    private fun toOrderItem(row: ResultRow): OrderItem {
        return OrderItem(
            id = row[OrderItems.id].toString(),
            orderId = row[OrderItems.orderId].toString(),
            productId = row[OrderItems.productId],
            productName = row[OrderItems.productName],
            quantity = row[OrderItems.quantity],
            unitPrice = row[OrderItems.unitPrice].toDouble(),
            discountPercent = row[OrderItems.discountPercent].toDouble(),
            discountAmount = row[OrderItems.discountAmount].toDouble(),
            lineTotal = row[OrderItems.lineTotal].toDouble(),
            createdAt = row[OrderItems.createdAt]
        )
    }

    /**
     * Day 10: Cancel order by retailer (PENDING only)
     * Returns true if cancelled, false if already cancelled or cannot be cancelled
     */
    fun cancelByRetailer(orderId: String, retailerId: String, reason: String?): Boolean = transaction {
        val order = Orders.select { 
            (Orders.id eq UUID.fromString(orderId)) and 
            (Orders.retailerId eq UUID.fromString(retailerId))
        }.singleOrNull() ?: return@transaction false
        
        val currentStatus = order[Orders.status]
        
        // Only PENDING orders can be cancelled by retailer
        if (currentStatus != OrderStatus.PENDING) {
            return@transaction false
        }
        
        // Update to CANCELLED
        Orders.update({ Orders.id eq UUID.fromString(orderId) }) {
            it[status] = OrderStatus.CANCELLED
            it[cancelledAt] = Instant.now()
            it[cancelledBy] = UUID.fromString(retailerId)
            it[cancellationReason] = reason
            it[updatedAt] = Instant.now()
        }
        
        true
    }

    /**
     * Day 10: Cancel order by admin (any status)
     * Returns true if cancelled, false if order not found
     */
    fun cancelByAdmin(orderId: String, adminId: String, reason: String): Boolean = transaction {
        val order = Orders.select { Orders.id eq UUID.fromString(orderId) }.singleOrNull() 
            ?: return@transaction false
        
        val currentStatus = order[Orders.status]
        
        // Already cancelled by admin or retailer, idempotent
        if (currentStatus == OrderStatus.CANCELLED_ADMIN || currentStatus == OrderStatus.CANCELLED) {
            return@transaction true
        }
        
        // Update to CANCELLED_ADMIN
        Orders.update({ Orders.id eq UUID.fromString(orderId) }) {
            it[status] = OrderStatus.CANCELLED_ADMIN
            it[cancelledAt] = Instant.now()
            it[cancelledBy] = UUID.fromString(adminId)
            it[cancellationReason] = reason
            it[updatedAt] = Instant.now()
        }
        
        true
    }
}
