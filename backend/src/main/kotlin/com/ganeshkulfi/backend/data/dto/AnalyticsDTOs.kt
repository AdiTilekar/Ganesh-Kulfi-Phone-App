package com.ganeshkulfi.backend.data.dto

import kotlinx.serialization.Serializable

/**
 * Paginated Response wrapper
 */
@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val totalCount: Int,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int
)

/**
 * Order Filters for history queries
 */
data class OrderFilters(
    val status: String? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val productId: String? = null,
    val minTotal: Double? = null,
    val maxTotal: Double? = null,
    val sortBy: String = "date",  // date | total_amount
    val order: String = "desc",    // asc | desc
    val page: Int = 1,
    val pageSize: Int = 20
)

/**
 * Analytics Summary Response
 */
@Serializable
data class AnalyticsSummaryResponse(
    val totalOrdersToday: Int,
    val confirmedOrdersToday: Int,
    val rejectedOrdersToday: Int,
    val pendingOrdersToday: Int,
    val totalRevenueToday: Double,
    val averageOrderValue: Double
)

/**
 * Top Product Analytics
 */
@Serializable
data class TopProductResponse(
    val productId: String,
    val productName: String,
    val totalQuantitySold: Int,
    val totalOrders: Int,
    val totalRevenue: Double
)

/**
 * Top Retailer Analytics
 */
@Serializable
data class TopRetailerResponse(
    val retailerId: String,
    val retailerName: String,
    val retailerEmail: String,
    val shopName: String?,
    val totalOrders: Int,
    val totalSpent: Double
)

/**
 * Weekly Order Statistics
 */
@Serializable
data class WeeklyOrderStats(
    val date: String,
    val totalOrders: Int,
    val confirmedOrders: Int,
    val rejectedOrders: Int,
    val totalRevenue: Double
)

/**
 * Order History Response (simplified for retailers)
 */
@Serializable
data class OrderHistoryResponse(
    val id: String,
    val orderNumber: String,
    val status: String,
    val totalItems: Int,
    val totalQuantity: Int,
    val totalAmount: Double,
    val createdAt: String,
    val updatedAt: String,
    val confirmedAt: String? = null,
    val rejectedAt: String? = null,
    val factoryNotes: String? = null,
    val rejectionReason: String? = null
)
