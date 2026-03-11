package com.ganeshkulfi.backend.data.dto

import kotlinx.serialization.Serializable

/**
 * Daily Sales DTOs
 */

// ============= REQUEST DTOs =============

@Serializable
data class CreateSaleRequest(
    val productId: String,
    val quantity: Int,
    val sellPrice: Double,
    val paymentMethod: String = "CASH",   // CASH | UPI | CREDIT
    val note: String? = null,
    val idempotencyKey: String? = null
)

// ============= RESPONSE DTOs =============

@Serializable
data class SaleResponse(
    val id: String,
    val userId: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val costPrice: Double,
    val sellPrice: Double,
    val profit: Double,
    val paymentMethod: String,
    val note: String? = null,
    val soldAt: String
)

@Serializable
data class DailySalesSummaryResponse(
    val date: String,
    val sales: List<SaleResponse>,
    val totalRevenue: Double,
    val totalCost: Double,
    val totalProfit: Double,
    val totalUnits: Int
)
