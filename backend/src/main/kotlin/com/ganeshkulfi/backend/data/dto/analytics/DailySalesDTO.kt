package com.ganeshkulfi.backend.data.dto.analytics

import kotlinx.serialization.Serializable

@Serializable
data class DailySalesDTO(
    val date: String,
    val totalSales: Double
)

@Serializable
data class DailySalesResponse(
    val days: List<DailySalesDTO>
)
