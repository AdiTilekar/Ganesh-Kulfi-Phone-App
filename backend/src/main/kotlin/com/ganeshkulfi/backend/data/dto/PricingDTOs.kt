package com.ganeshkulfi.backend.data.dto

import kotlinx.serialization.Serializable

/**
 * Day 9: Pricing DTOs
 * Admin price override management DTOs
 * Retailer-safe pricing DTOs (hide tier, base price, override price)
 */

// ============ ADMIN PRICE OVERRIDE DTOs ============

@Serializable
data class CreatePriceOverrideRequest(
    val productId: String,  // UUID string
    val tier: String, // BASIC, SILVER, GOLD, PLATINUM
    val overridePrice: Double
)

@Serializable
data class UpdatePriceOverrideRequest(
    val overridePrice: Double,
    val active: Boolean
)

@Serializable
data class PriceOverrideResponse(
    val id: String,  // UUID as string
    val productId: String,  // UUID string
    val productName: String, // For admin convenience
    val tier: String,
    val overridePrice: Double,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class PriceOverrideListResponse(
    val overrides: List<PriceOverrideResponse>,
    val total: Int
)

// ============ RETAILER-SAFE PRICING DTOs ============

/**
 * Retailer-safe order item pricing (HIDES all server-side logic)
 * Only shows final prices, tax amount, discount amount
 */
@Serializable
data class RetailerOrderItemPricing(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPriceFinal: Double, // Final unit price after all calculations
    val taxAmount: Double, // GST amount per unit
    val discountAmount: Double, // Discount amount per unit
    val lineTotal: Double // Total for this line item
)

/**
 * Retailer-safe order summary pricing
 */
@Serializable
data class RetailerOrderPricing(
    val subtotal: Double,
    val totalTax: Double,
    val totalDiscount: Double,
    val totalAmount: Double
)

// ============ ADMIN-ONLY FULL PRICING BREAKDOWN ============

/**
 * Admin-only full pricing details (shows all server-side logic)
 */
@Serializable
data class AdminOrderItemPricing(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val tier: String, // ADMIN ONLY
    val basePrice: Double, // ADMIN ONLY
    val overridePrice: Double?, // ADMIN ONLY
    val effectivePrice: Double, // ADMIN ONLY
    val quantityDiscountPercentage: Double, // ADMIN ONLY
    val discountAmount: Double,
    val priceAfterDiscount: Double, // ADMIN ONLY
    val gstPercentage: Double, // ADMIN ONLY
    val gstAmount: Double,
    val unitPriceFinal: Double,
    val lineTotal: Double
)

@Serializable
data class AdminOrderPricing(
    val items: List<AdminOrderItemPricing>,
    val subtotal: Double,
    val totalTax: Double,
    val totalDiscount: Double,
    val totalAmount: Double
)
