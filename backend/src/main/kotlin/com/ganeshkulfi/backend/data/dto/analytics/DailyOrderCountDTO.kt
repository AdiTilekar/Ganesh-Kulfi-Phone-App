package com.ganeshkulfi.backend.data.dto.analytics

import kotlinx.serialization.Serializable

@Serializable
data class DailyOrderCountDTO(
    val date: String,
    val count: Int
)

@Serializable
data class DailyOrderCountResponse(
    val days: List<DailyOrderCountDTO>
)
