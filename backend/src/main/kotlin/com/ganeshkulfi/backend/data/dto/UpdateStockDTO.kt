package com.ganeshkulfi.backend.data.dto

import kotlinx.serialization.Serializable

/**
 * Day 13: Update Stock DTO
 */
@Serializable
data class UpdateStockDTO(
    val stockQuantity: Int
)
