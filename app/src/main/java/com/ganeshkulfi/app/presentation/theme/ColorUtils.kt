package com.ganeshkulfi.app.presentation.theme

import androidx.compose.ui.graphics.Color
import com.ganeshkulfi.app.data.model.PricingTier

/**
 * Get color for order status
 */
fun getOrderStatusColor(status: String): Color {
    return when (status.uppercase()) {
        "PENDING" -> StatusPending
        "CONFIRMED" -> StatusConfirmed
        "PACKED" -> StatusPacked
        "OUT_FOR_DELIVERY" -> StatusOutForDelivery
        "DELIVERED" -> StatusDelivered
        "CANCELLED" -> StatusCancelled
        else -> NeutralGray
    }
}

/**
 * Get background color for order status badge
 */
fun getOrderStatusBackgroundColor(status: String): Color {
    return getOrderStatusColor(status).copy(alpha = 0.15f)
}

/**
 * Get stock alert color based on quantity
 */
fun getStockColor(availableStock: Int, lowStockThreshold: Int = 10): Color {
    return when {
        availableStock == 0 -> StockOutOfStock
        availableStock <= lowStockThreshold -> StockLowStock
        else -> StockInStock
    }
}

/**
 * Get stock background color
 */
fun getStockBackgroundColor(availableStock: Int, lowStockThreshold: Int = 10): Color {
    return getStockColor(availableStock, lowStockThreshold).copy(alpha = 0.15f)
}

/**
 * Get pricing tier color
 */
fun getTierColor(tier: PricingTier): Color {
    return when (tier) {
        PricingTier.GOLD -> TierGold
        PricingTier.SILVER -> TierSilver
        PricingTier.BASIC -> TierBronze
    }
}

/**
 * Get pricing tier light color
 */
fun getTierLightColor(tier: PricingTier): Color {
    return when (tier) {
        PricingTier.GOLD -> TierGoldLight
        PricingTier.SILVER -> TierSilverLight
        PricingTier.BASIC -> TierBronzeLight
    }
}

/**
 * Get pricing tier container color
 */
fun getTierContainerColor(tier: PricingTier): Color {
    return when (tier) {
        PricingTier.GOLD -> TierGoldContainer
        PricingTier.SILVER -> TierSilverContainer
        PricingTier.BASIC -> TierBronzeContainer
    }
}

/**
 * Get tier emoji/icon
 */
fun getTierEmoji(tier: PricingTier): String {
    return when (tier) {
        PricingTier.GOLD -> "👑"
        PricingTier.SILVER -> "⭐"
        PricingTier.BASIC -> "🥉"
    }
}

/**
 * Get order status emoji
 */
fun getStatusEmoji(status: String): String {
    return when (status.uppercase()) {
        "PENDING" -> "⏳"
        "CONFIRMED" -> "✅"
        "PACKED" -> "📦"
        "OUT_FOR_DELIVERY" -> "🚚"
        "DELIVERED" -> "✓"
        "CANCELLED" -> "❌"
        else -> "📋"
    }
}

/**
 * Get stock status emoji
 */
fun getStockEmoji(availableStock: Int, lowStockThreshold: Int = 10): String {
    return when {
        availableStock == 0 -> "🚫"
        availableStock <= lowStockThreshold -> "⚠️"
        else -> "✓"
    }
}
