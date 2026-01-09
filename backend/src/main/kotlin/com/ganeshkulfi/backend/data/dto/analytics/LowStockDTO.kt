package com.ganeshkulfi.backend.data.dto.analytics

import kotlinx.serialization.Serializable

@Serializable
data class LowStockDTO(
    val productId: String,
    val name: String,
    val stock: Int,
    val lowStockThreshold: Int
)

@Serializable
data class LowStockResponse(
    val items: List<LowStockDTO>
)
