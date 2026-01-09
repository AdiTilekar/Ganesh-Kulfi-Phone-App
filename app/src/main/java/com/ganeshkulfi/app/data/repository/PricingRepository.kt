package com.ganeshkulfi.app.data.repository

import com.ganeshkulfi.app.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository to manage retailer-specific pricing
 */
@Singleton
class PricingRepository @Inject constructor() {

    // Custom pricing for specific retailer-flavor combinations
    private val _customPricing = MutableStateFlow<List<RetailerPricing>>(getSampleCustomPricing())
    val customPricing: StateFlow<List<RetailerPricing>> = _customPricing.asStateFlow()

    // Bulk pricing rules
    private val _bulkPricingRules = MutableStateFlow(BulkPricingRule.getDefaultRules())
    val bulkPricingRules: StateFlow<List<BulkPricingRule>> = _bulkPricingRules.asStateFlow()

    /**
     * Get price for a specific retailer and flavor
     * Priority: Custom Price > Bulk Discount > Tier Discount > Base Price
     */
    fun getRetailerPrice(
        retailer: Retailer,
        flavorId: String,
        basePrice: Double,
        quantity: Int = 1
    ): PriceInfo {
        // 1. Check for custom pricing first
        val customPrice = _customPricing.value.find {
            it.retailerId == retailer.id && it.flavorId == flavorId && it.isActive
        }

        if (customPrice != null && customPrice.customPrice != null) {
            // Apply custom price with additional discount if any
            val finalPrice = customPrice.customPrice * (1 - customPrice.discount / 100)
            return PriceInfo(
                basePrice = basePrice,
                retailerPrice = finalPrice,
                discount = customPrice.discount,
                pricingTier = PricingTier.BASIC,
                isCustomPrice = true,
                minimumQuantity = customPrice.minimumQuantity,
                savings = basePrice - finalPrice
            )
        }

        // 2. Check for bulk pricing based on quantity
        val bulkDiscount = getBulkDiscount(flavorId, quantity)
        
        // 3. Apply tier discount
        val tierDiscount = retailer.pricingTier.discountPercentage
        
        // Use the higher discount (bulk or tier)
        val effectiveDiscount = maxOf(bulkDiscount, tierDiscount)
        val finalPrice = basePrice * (1 - effectiveDiscount / 100)

        return PriceInfo(
            basePrice = basePrice,
            retailerPrice = finalPrice,
            discount = effectiveDiscount,
            pricingTier = if (bulkDiscount > tierDiscount) PricingTier.SILVER else retailer.pricingTier,
            isCustomPrice = false,
            minimumQuantity = 0,
            savings = basePrice - finalPrice
        )
    }

    /**
     * Get bulk discount percentage based on quantity
     */
    private fun getBulkDiscount(flavorId: String, quantity: Int): Double {
        val applicableRules = _bulkPricingRules.value
            .filter { it.isActive && (it.flavorId == flavorId || it.flavorId == "all") }
            .filter { quantity >= it.minimumQuantity }
            .sortedByDescending { it.discountPercentage }

        return applicableRules.firstOrNull()?.discountPercentage ?: 0.0
    }

    /**
     * Set custom price for a retailer-flavor combination
     */
    fun setCustomPrice(
        retailerId: String,
        flavorId: String,
        customPrice: Double,
        discount: Double = 0.0,
        minimumQuantity: Int = 0
    ) {
        val existingIndex = _customPricing.value.indexOfFirst {
            it.retailerId == retailerId && it.flavorId == flavorId
        }

        val newPricing = RetailerPricing(
            id = if (existingIndex >= 0) _customPricing.value[existingIndex].id else "custom_${System.currentTimeMillis()}",
            retailerId = retailerId,
            flavorId = flavorId,
            customPrice = customPrice,
            discount = discount,
            minimumQuantity = minimumQuantity,
            isActive = true,
            updatedAt = System.currentTimeMillis()
        )

        val updatedList = _customPricing.value.toMutableList()
        if (existingIndex >= 0) {
            updatedList[existingIndex] = newPricing
        } else {
            updatedList.add(newPricing)
        }
        _customPricing.value = updatedList
    }

    /**
     * Remove custom pricing for a retailer-flavor combination
     */
    fun removeCustomPrice(retailerId: String, flavorId: String) {
        _customPricing.value = _customPricing.value.filter {
            !(it.retailerId == retailerId && it.flavorId == flavorId)
        }
    }

    /**
     * Get all custom pricing for a specific retailer
     */
    fun getRetailerCustomPricing(retailerId: String): List<RetailerPricing> {
        return _customPricing.value.filter { it.retailerId == retailerId && it.isActive }
    }

    /**
     * Add or update bulk pricing rule
     */
    fun addBulkPricingRule(rule: BulkPricingRule) {
        val existingIndex = _bulkPricingRules.value.indexOfFirst { it.id == rule.id }
        val updatedList = _bulkPricingRules.value.toMutableList()
        
        if (existingIndex >= 0) {
            updatedList[existingIndex] = rule
        } else {
            updatedList.add(rule)
        }
        _bulkPricingRules.value = updatedList
    }

    /**
     * Remove bulk pricing rule
     */
    fun removeBulkPricingRule(ruleId: String) {
        _bulkPricingRules.value = _bulkPricingRules.value.filter { it.id != ruleId }
    }

    /**
     * Calculate total amount for a transaction
     */
    fun calculateTransactionAmount(
        retailer: Retailer,
        flavorId: String,
        basePrice: Double,
        quantity: Int
    ): Pair<Double, PriceInfo> {
        val priceInfo = getRetailerPrice(retailer, flavorId, basePrice, quantity)
        val totalAmount = priceInfo.retailerPrice * quantity
        return Pair(totalAmount, priceInfo)
    }

    /**
     * Get price breakdown for UI display
     */
    fun getPriceBreakdown(
        retailer: Retailer,
        flavorId: String,
        flavorName: String,
        basePrice: Double,
        quantity: Int
    ): PriceBreakdown {
        val priceInfo = getRetailerPrice(retailer, flavorId, basePrice, quantity)
        val subtotal = basePrice * quantity
        val discountAmount = (basePrice - priceInfo.retailerPrice) * quantity
        val total = priceInfo.retailerPrice * quantity

        return PriceBreakdown(
            flavorName = flavorName,
            quantity = quantity,
            basePrice = basePrice,
            retailerPrice = priceInfo.retailerPrice,
            subtotal = subtotal,
            discountPercentage = priceInfo.discount,
            discountAmount = discountAmount,
            total = total,
            pricingTier = priceInfo.pricingTier,
            isCustomPrice = priceInfo.isCustomPrice
        )
    }

    /**
     * Sample custom pricing data
     */
    private fun getSampleCustomPricing(): List<RetailerPricing> = listOf(
        // Example: Special price for Kumar Sweet Shop on Chocolate Kulfi
        RetailerPricing(
            id = "custom_001",
            retailerId = "ret_001",
            flavorId = "chocolate",
            customPrice = 25.0,  // Custom price instead of tier discount
            discount = 0.0,
            minimumQuantity = 50,
            isActive = true
        )
    )
}

/**
 * Price breakdown for displaying detailed pricing to user
 */
data class PriceBreakdown(
    val flavorName: String,
    val quantity: Int,
    val basePrice: Double,
    val retailerPrice: Double,
    val subtotal: Double,
    val discountPercentage: Double,
    val discountAmount: Double,
    val total: Double,
    val pricingTier: PricingTier,
    val isCustomPrice: Boolean
)
