package com.ganeshkulfi.backend.services

import com.ganeshkulfi.backend.data.models.PriceOverrides
import com.ganeshkulfi.backend.data.models.Products
import com.ganeshkulfi.backend.data.models.RetailerTier
import com.ganeshkulfi.backend.data.models.Users
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Day 9: Pricing Service
 * Server-side only pricing logic with retailer tier-based pricing
 * ALL pricing calculations are HIDDEN from retailers
 * 
 * Pricing Logic:
 * 1. Check for tier-specific price override
 * 2. Fall back to product base price
 * 3. Apply quantity discounts (10+ qty = 5%, 50+ qty = 10%, 100+ qty = 15%)
 * 4. Calculate GST (18%)
 * 5. Return final price breakdown (NEVER expose tier or override to retailers)
 */
class PricingService {

    /**
     * Calculate final price for a product based on retailer tier and quantity
     * 
     * @param productId Product ID (UUID string)
     * @param retailerId Retailer user ID (UUID)
     * @param quantity Order quantity
     * @return PriceCalculation with breakdown (server-side only)
     */
    fun calculatePrice(productId: String, retailerId: String, quantity: Int): PriceCalculation = transaction {
        // 1. Get product base price
        val product = Products.select { Products.id eq productId }
            .singleOrNull()
            ?: throw IllegalArgumentException("Product not found: $productId")

        val basePrice = product[Products.basePrice]

        // 2. Get retailer tier (default BASIC)
        val retailerUUID = try {
            java.util.UUID.fromString(retailerId)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid retailer ID: $retailerId")
        }
        
        val retailerTier = Users.select { Users.id eq retailerUUID }
            .singleOrNull()
            ?.get(Users.tier)
            ?: RetailerTier.BASIC

        // 3. Check for tier-specific price override
        val overridePrice = PriceOverrides.select {
            (PriceOverrides.productId eq productId) and
            (PriceOverrides.tier eq retailerTier) and
            (PriceOverrides.active eq true)
        }.singleOrNull()?.get(PriceOverrides.overridePrice)

        // 4. Use override price if available, else base price
        val effectivePrice = overridePrice ?: basePrice

        // 5. Apply quantity discount
        val quantityDiscountPercentage = calculateQuantityDiscount(quantity)
        val discountAmount = effectivePrice * (quantityDiscountPercentage.toBigDecimal() / BigDecimal(100))
        val priceAfterDiscount = effectivePrice - discountAmount

        // 6. Calculate GST (18%)
        val gstPercentage = BigDecimal("18.00")
        val gstAmount = priceAfterDiscount * (gstPercentage / BigDecimal(100))
        val finalUnitPrice = priceAfterDiscount + gstAmount

        // 7. Calculate line total
        val lineTotal = finalUnitPrice * quantity.toBigDecimal()

        PriceCalculation(
            productId = productId,
            retailerId = retailerId,
            tier = retailerTier.name, // Server-side only
            basePrice = basePrice.setScale(2, RoundingMode.HALF_UP).toDouble(),
            overridePrice = overridePrice?.setScale(2, RoundingMode.HALF_UP)?.toDouble(), // Server-side only
            effectivePrice = effectivePrice.setScale(2, RoundingMode.HALF_UP).toDouble(),
            quantity = quantity,
            quantityDiscountPercentage = quantityDiscountPercentage,
            discountAmount = discountAmount.setScale(2, RoundingMode.HALF_UP).toDouble(),
            priceAfterDiscount = priceAfterDiscount.setScale(2, RoundingMode.HALF_UP).toDouble(),
            gstPercentage = gstPercentage.toDouble(),
            gstAmount = gstAmount.setScale(2, RoundingMode.HALF_UP).toDouble(),
            unitPriceFinal = finalUnitPrice.setScale(2, RoundingMode.HALF_UP).toDouble(),
            lineTotal = lineTotal.setScale(2, RoundingMode.HALF_UP).toDouble()
        )
    }

    /**
     * Calculate quantity discount percentage
     * 10+ qty = 5%, 50+ qty = 10%, 100+ qty = 15%
     */
    private fun calculateQuantityDiscount(quantity: Int): Double {
        return when {
            quantity >= 100 -> 15.0
            quantity >= 50 -> 10.0
            quantity >= 10 -> 5.0
            else -> 0.0
        }
    }

    /**
     * Get retailer-safe pricing info (HIDES tier, base price, override price)
     * Only exposes final unit price, tax amount, discount amount, line total
     */
    fun getRetailerPricing(calculation: PriceCalculation): RetailerPricing {
        return RetailerPricing(
            unitPriceFinal = calculation.unitPriceFinal,
            taxAmount = calculation.gstAmount,
            discountAmount = calculation.discountAmount,
            lineTotal = calculation.lineTotal
        )
    }
}

/**
 * Full price calculation breakdown (SERVER-SIDE ONLY)
 * NEVER send this to retailers - use getRetailerPricing() instead
 */
data class PriceCalculation(
    val productId: String,  // UUID string
    val retailerId: String,
    val tier: String, // HIDDEN from retailers
    val basePrice: Double, // HIDDEN from retailers
    val overridePrice: Double?, // HIDDEN from retailers
    val effectivePrice: Double, // HIDDEN from retailers
    val quantity: Int,
    val quantityDiscountPercentage: Double, // HIDDEN from retailers
    val discountAmount: Double,
    val priceAfterDiscount: Double, // HIDDEN from retailers
    val gstPercentage: Double, // HIDDEN from retailers (retailers see final tax amount only)
    val gstAmount: Double,
    val unitPriceFinal: Double, // Exposed to retailers
    val lineTotal: Double // Exposed to retailers
)

/**
 * Retailer-safe pricing (HIDES all sensitive pricing logic)
 * Only shows final prices, tax amount, discount amount
 */
data class RetailerPricing(
    val unitPriceFinal: Double,
    val taxAmount: Double,
    val discountAmount: Double,
    val lineTotal: Double
)
