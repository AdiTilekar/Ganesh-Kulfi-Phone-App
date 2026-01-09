package com.ganeshkulfi.backend.data.dto.analytics

import com.ganeshkulfi.backend.data.dto.TopProductResponse
import com.ganeshkulfi.backend.data.dto.TopRetailerResponse
import kotlinx.serialization.Serializable

@Serializable
data class DashboardResponse(
    val totalOrders: Int,
    val totalRevenue: Double,
    val dailyOrders: DailyOrderCountResponse,
    val dailySales: DailySalesResponse,
    val topProducts: List<TopProductResponse>?,
    val topRetailers: List<TopRetailerResponse>?
)
