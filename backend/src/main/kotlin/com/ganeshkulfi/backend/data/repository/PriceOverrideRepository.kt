package com.ganeshkulfi.backend.data.repository

import com.ganeshkulfi.backend.data.models.PriceOverride
import com.ganeshkulfi.backend.data.models.PriceOverrides
import com.ganeshkulfi.backend.data.models.Products
import com.ganeshkulfi.backend.data.models.RetailerTier
import com.ganeshkulfi.backend.data.models.toPriceOverride
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

/**
 * Day 9: Price Override Repository
 * Server-side only price override management
 */
class PriceOverrideRepository {

    /**
     * Create a new price override
     */
    fun create(productId: String, tier: String, overridePrice: Double): PriceOverride = transaction {
        val now = Instant.now()
        val tierEnum = RetailerTier.fromString(tier) ?: RetailerTier.BASIC
        
        val id = PriceOverrides.insert {
            it[PriceOverrides.productId] = productId
            it[PriceOverrides.tier] = tierEnum
            it[PriceOverrides.overridePrice] = overridePrice.toBigDecimal()
            it[PriceOverrides.active] = true
            it[createdAt] = now
            it[updatedAt] = now
        } get PriceOverrides.id

        PriceOverrides.select { PriceOverrides.id eq id }
            .single()
            .toPriceOverride()
    }

    /**
     * Get price override by product and tier
     */
    fun findByProductAndTier(productId: String, tier: String): PriceOverride? = transaction {
        val tierEnum = RetailerTier.fromString(tier) ?: return@transaction null
        PriceOverrides.select {
            (PriceOverrides.productId eq productId) and
            (PriceOverrides.tier eq tierEnum)
        }.singleOrNull()?.toPriceOverride()
    }

    /**
     * Get active price override by product and tier
     */
    fun findActiveByProductAndTier(productId: String, tier: String): PriceOverride? = transaction {
        val tierEnum = RetailerTier.fromString(tier) ?: return@transaction null
        PriceOverrides.select {
            (PriceOverrides.productId eq productId) and
            (PriceOverrides.tier eq tierEnum) and
            (PriceOverrides.active eq true)
        }.singleOrNull()?.toPriceOverride()
    }

    /**
     * Get price override by ID
     */
    fun findById(id: String): PriceOverride? = transaction {
        val uuid = java.util.UUID.fromString(id)
        PriceOverrides.select { PriceOverrides.id eq uuid }
            .singleOrNull()?.toPriceOverride()
    }

    /**
     * Get all price overrides for a product
     */
    fun findByProduct(productId: String): List<PriceOverride> = transaction {
        PriceOverrides.select { PriceOverrides.productId eq productId }
            .map { it.toPriceOverride() }
    }

    /**
     * Get all active price overrides
     */
    fun findAllActive(): List<PriceOverride> = transaction {
        PriceOverrides.select { PriceOverrides.active eq true }
            .map { it.toPriceOverride() }
    }

    /**
     * Get all price overrides (admin only)
     */
    fun findAll(): List<PriceOverride> = transaction {
        PriceOverrides.selectAll()
            .map { it.toPriceOverride() }
    }

    /**
     * Update price override
     */
    fun update(id: String, overridePrice: Double, active: Boolean): Boolean = transaction {
        val uuid = java.util.UUID.fromString(id)
        PriceOverrides.update({ PriceOverrides.id eq uuid }) {
            it[PriceOverrides.overridePrice] = overridePrice.toBigDecimal()
            it[PriceOverrides.active] = active
            it[updatedAt] = Instant.now()
        } > 0
    }

    /**
     * Delete price override (soft delete by setting active = false)
     */
    fun softDelete(id: String): Boolean = transaction {
        val uuid = java.util.UUID.fromString(id)
        PriceOverrides.update({ PriceOverrides.id eq uuid }) {
            it[active] = false
            it[updatedAt] = Instant.now()
        } > 0
    }

    /**
     * Delete price override (hard delete)
     */
    fun delete(id: String): Boolean = transaction {
        val uuid = java.util.UUID.fromString(id)
        PriceOverrides.deleteWhere { PriceOverrides.id eq uuid } > 0
    }

    /**
     * Get all price overrides with product details (for admin UI)
     */
    fun findAllWithProductDetails(): List<Pair<PriceOverride, String>> = transaction {
        (PriceOverrides innerJoin Products)
            .selectAll()
            .map { row ->
                val override = PriceOverride(
                    id = row[PriceOverrides.id].toString(),  // Convert UUID to String
                    productId = row[PriceOverrides.productId],
                    tier = row[PriceOverrides.tier].name,
                    overridePrice = row[PriceOverrides.overridePrice].toDouble(),
                    active = row[PriceOverrides.active],
                    createdAt = row[PriceOverrides.createdAt].toString(),
                    updatedAt = row[PriceOverrides.updatedAt].toString()
                )
                val productName = row[Products.name]
                override to productName
            }
    }

    /**
     * Check if price override exists
     */
    fun exists(productId: String, tier: String): Boolean = transaction {
        val tierEnum = RetailerTier.fromString(tier) ?: return@transaction false
        PriceOverrides.select {
            (PriceOverrides.productId eq productId) and
            (PriceOverrides.tier eq tierEnum)
        }.count() > 0
    }
}
