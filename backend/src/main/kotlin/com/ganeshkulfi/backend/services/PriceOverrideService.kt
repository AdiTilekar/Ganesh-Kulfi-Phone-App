package com.ganeshkulfi.backend.services

import com.ganeshkulfi.backend.data.dto.CreatePriceOverrideRequest
import com.ganeshkulfi.backend.data.dto.PriceOverrideListResponse
import com.ganeshkulfi.backend.data.dto.PriceOverrideResponse
import com.ganeshkulfi.backend.data.dto.UpdatePriceOverrideRequest
import com.ganeshkulfi.backend.data.models.RetailerTier
import com.ganeshkulfi.backend.data.repository.PriceOverrideRepository
import com.ganeshkulfi.backend.data.repository.ProductRepository

/**
 * Day 9: Price Override Service
 * Admin-only price override management
 */
class PriceOverrideService(
    private val priceOverrideRepository: PriceOverrideRepository,
    private val productRepository: ProductRepository
) {

    /**
     * Create a new price override (admin only)
     */
    fun createPriceOverride(request: CreatePriceOverrideRequest): PriceOverrideResponse {
        // Validate tier
        val tier = RetailerTier.fromString(request.tier)
            ?: throw IllegalArgumentException("Invalid tier: ${request.tier}. Must be BASIC, SILVER, GOLD, or PLATINUM")

        // Validate product exists
        val product = productRepository.findById(request.productId)
            ?: throw IllegalArgumentException("Product not found: ${request.productId}")

        // Validate override price
        if (request.overridePrice < 0) {
            throw IllegalArgumentException("Override price must be >= 0")
        }

        // Check if override already exists
        if (priceOverrideRepository.exists(request.productId, tier.name)) {
            throw IllegalArgumentException("Price override already exists for product ${request.productId} and tier ${tier.name}")
        }

        // Create override
        val override = priceOverrideRepository.create(
            productId = request.productId,
            tier = tier.name,
            overridePrice = request.overridePrice
        )

        return PriceOverrideResponse(
            id = override.id,
            productId = override.productId,
            productName = product.name,
            tier = override.tier,
            overridePrice = override.overridePrice,
            active = override.active,
            createdAt = override.createdAt,
            updatedAt = override.updatedAt
        )
    }

    /**
     * Get price override by ID (admin only)
     */
    fun getPriceOverride(id: String): PriceOverrideResponse {
        val override = priceOverrideRepository.findById(id)
            ?: throw IllegalArgumentException("Price override not found: $id")

        val product = productRepository.findById(override.productId)
            ?: throw IllegalArgumentException("Product not found: ${override.productId}")

        return PriceOverrideResponse(
            id = override.id,
            productId = override.productId,
            productName = product.name,
            tier = override.tier,
            overridePrice = override.overridePrice,
            active = override.active,
            createdAt = override.createdAt,
            updatedAt = override.updatedAt
        )
    }

    /**
     * Get all price overrides (admin only)
     */
    fun getAllPriceOverrides(): PriceOverrideListResponse {
        val overridesWithProducts = priceOverrideRepository.findAllWithProductDetails()

        val responses = overridesWithProducts.map { (override, productName) ->
            PriceOverrideResponse(
                id = override.id,
                productId = override.productId,
                productName = productName,
                tier = override.tier,
                overridePrice = override.overridePrice,
                active = override.active,
                createdAt = override.createdAt,
                updatedAt = override.updatedAt
            )
        }

        return PriceOverrideListResponse(
            overrides = responses,
            total = responses.size
        )
    }

    /**
     * Get price overrides by product ID (admin only)
     */
    fun getPriceOverridesByProduct(productId: String): PriceOverrideListResponse {
        val product = productRepository.findById(productId)
            ?: throw IllegalArgumentException("Product not found: $productId")

        val overrides = priceOverrideRepository.findByProduct(productId)

        val responses = overrides.map { override ->
            PriceOverrideResponse(
                id = override.id,
                productId = override.productId,
                productName = product.name,
                tier = override.tier,
                overridePrice = override.overridePrice,
                active = override.active,
                createdAt = override.createdAt,
                updatedAt = override.updatedAt
            )
        }

        return PriceOverrideListResponse(
            overrides = responses,
            total = responses.size
        )
    }

    /**
     * Update price override (admin only)
     */
    fun updatePriceOverride(id: String, request: UpdatePriceOverrideRequest): PriceOverrideResponse {
        // Validate override exists
        val existing = priceOverrideRepository.findById(id)
            ?: throw IllegalArgumentException("Price override not found: $id")

        // Validate override price
        if (request.overridePrice < 0) {
            throw IllegalArgumentException("Override price must be >= 0")
        }

        // Update
        val updated = priceOverrideRepository.update(
            id = id,
            overridePrice = request.overridePrice,
            active = request.active
        )

        if (!updated) {
            throw IllegalStateException("Failed to update price override: $id")
        }

        // Get updated override
        return getPriceOverride(id)
    }

    /**
     * Delete price override (admin only)
     */
    fun deletePriceOverride(id: String): Boolean {
        val existing = priceOverrideRepository.findById(id)
            ?: throw IllegalArgumentException("Price override not found: $id")

        return priceOverrideRepository.delete(id)
    }

    /**
     * Soft delete price override (admin only)
     */
    fun deactivatePriceOverride(id: String): Boolean {
        val existing = priceOverrideRepository.findById(id)
            ?: throw IllegalArgumentException("Price override not found: $id")

        return priceOverrideRepository.softDelete(id)
    }
}
