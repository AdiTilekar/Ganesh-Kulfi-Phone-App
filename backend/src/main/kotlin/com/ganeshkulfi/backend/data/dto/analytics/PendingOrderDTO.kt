package com.ganeshkulfi.backend.data.dto.analytics

import kotlinx.serialization.Serializable

@Serializable
data class PendingOrderDTO(
    val id: String,
    val orderNumber: String,
    val createdAt: String,
    val retailerId: String,
    val retailerName: String,
    val shopName: String?,
    val totalAmount: Double
)

@Serializable
data class PendingOrdersResponse(
    val count: Int,
    val orders: List<PendingOrderDTO>
)
