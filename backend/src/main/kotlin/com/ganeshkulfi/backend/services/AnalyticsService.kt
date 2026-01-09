package com.ganeshkulfi.backend.services

import com.ganeshkulfi.backend.data.dto.*
import com.ganeshkulfi.backend.data.dto.analytics.*
import com.ganeshkulfi.backend.data.models.OrderStatus
import com.ganeshkulfi.backend.data.repository.AnalyticsRepository
import com.ganeshkulfi.backend.data.repository.OrderRepository
import com.ganeshkulfi.backend.data.repository.ProductRepository
import java.time.format.DateTimeFormatter

/**
 * Analytics Service
 * Business logic for admin analytics and reporting
 */
class AnalyticsService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val analyticsRepository: AnalyticsRepository
) {
    
    companion object {
        private const val LOW_STOCK_THRESHOLD = 10
        private const val DEFAULT_DAYS = 30
    }
    
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    /**
     * Get orders summary for today (admin dashboard)
     */
    fun getOrdersSummary(): Result<AnalyticsSummaryResponse> {
        return try {
            val summary = orderRepository.getOrdersSummaryToday()
            
            val response = AnalyticsSummaryResponse(
                totalOrdersToday = summary["totalOrdersToday"] as Int,
                confirmedOrdersToday = summary["confirmedOrdersToday"] as Int,
                rejectedOrdersToday = summary["rejectedOrdersToday"] as Int,
                pendingOrdersToday = summary["pendingOrdersToday"] as Int,
                totalRevenueToday = summary["totalRevenueToday"] as Double,
                averageOrderValue = summary["averageOrderValue"] as Double
            )
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get top selling products
     */
    fun getTopProducts(limit: Int = 10): Result<List<TopProductResponse>> {
        return try {
            if (limit < 1 || limit > 100) {
                return Result.failure(Exception("Limit must be between 1 and 100"))
            }
            
            val topProducts = orderRepository.getTopProducts(limit)
            
            val response = topProducts.map { product ->
                TopProductResponse(
                    productId = product["productId"] as String,
                    productName = product["productName"] as String,
                    totalQuantitySold = product["totalQuantitySold"] as Int,
                    totalOrders = product["totalOrders"] as Int,
                    totalRevenue = product["totalRevenue"] as Double
                )
            }
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get top retailers by order count
     */
    fun getTopRetailers(limit: Int = 10): Result<List<TopRetailerResponse>> {
        return try {
            if (limit < 1 || limit > 100) {
                return Result.failure(Exception("Limit must be between 1 and 100"))
            }
            
            val topRetailers = orderRepository.getTopRetailers(limit)
            
            val response = topRetailers.map { retailer ->
                TopRetailerResponse(
                    retailerId = retailer["retailerId"] as String,
                    retailerName = retailer["retailerName"] as String,
                    retailerEmail = retailer["retailerEmail"] as String,
                    shopName = retailer["shopName"] as? String,
                    totalOrders = retailer["totalOrders"] as Int,
                    totalSpent = retailer["totalSpent"] as Double
                )
            }
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get weekly order statistics (last 7 days)
     */
    fun getWeeklyOrderStats(): Result<List<WeeklyOrderStats>> {
        return try {
            val weeklyStats = orderRepository.getWeeklyOrderStats()
            
            val response = weeklyStats.map { stat ->
                WeeklyOrderStats(
                    date = stat["date"] as String,
                    totalOrders = stat["totalOrders"] as Int,
                    confirmedOrders = stat["confirmedOrders"] as Int,
                    rejectedOrders = stat["rejectedOrders"] as Int,
                    totalRevenue = stat["totalRevenue"] as Double
                )
            }
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get daily order count for last N days
     */
    fun getDailyOrderCount(days: Int = DEFAULT_DAYS): DailyOrderCountResponse {
        val dailyCounts = analyticsRepository.getDailyOrderCount(days)
        
        return DailyOrderCountResponse(
            days = dailyCounts.map { 
                DailyOrderCountDTO(
                    date = it.date,
                    count = it.count
                )
            }
        )
    }
    
    /**
     * Get daily sales for last N days
     */
    fun getDailySales(days: Int = DEFAULT_DAYS): DailySalesResponse {
        val dailySales = analyticsRepository.getDailySales(days)
        
        return DailySalesResponse(
            days = dailySales.map {
                DailySalesDTO(
                    date = it.date,
                    totalSales = it.totalSales
                )
            }
        )
    }
    
    /**
     * Get pending orders count and list
     */
    fun getPendingOrders(): PendingOrdersResponse {
        val pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING)
        
        return PendingOrdersResponse(
            count = pendingOrders.size,
            orders = pendingOrders.map { order ->
                PendingOrderDTO(
                    id = order.id,
                    orderNumber = order.orderNumber,
                    createdAt = order.createdAt.toString(),
                    retailerId = order.retailerId,
                    retailerName = order.retailerName,
                    shopName = order.shopName,
                    totalAmount = order.totalAmount
                )
            }
        )
    }
    
    /**
     * Get low stock products
     */
    fun getLowStockProducts(): LowStockResponse {
        val allProducts = productRepository.findAll()
        
        val lowStockProducts = allProducts.filter { product ->
            product.stockQuantity < LOW_STOCK_THRESHOLD
        }
        
        return LowStockResponse(
            items = lowStockProducts.map { product ->
                LowStockDTO(
                    productId = product.id,
                    name = product.name,
                    stock = product.stockQuantity,
                    lowStockThreshold = LOW_STOCK_THRESHOLD
                )
            }
        )
    }
}
