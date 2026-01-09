package com.ganeshkulfi.backend.data.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import org.postgresql.util.PGobject
import java.time.Instant

/**
 * Day 9: Price Override Model
 * Server-side only price overrides per product and retailer tier
 * NEVER exposed to retailers via API
 */

object PriceOverrides : Table("price_override") {
    val id = uuid("id").autoGenerate()
    val productId = char("product_id", 36)  // Foreign key to Products.id
    val tier = customEnumeration(
        "retailer_tier",  // Database column name
        "retailer_tier",  // PostgreSQL enum type name
        { value -> RetailerTier.valueOf(value as String) },
        { PGobject().apply { type = "retailer_tier"; value = it.name } }
    )
    val overridePrice = decimal("override_price", 10, 2)
    val active = bool("active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

@Serializable
data class PriceOverride(
    val id: String,  // UUID as string for JSON serialization
    val productId: String,  // UUID string
    val tier: String,
    val overridePrice: Double,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String
)

fun ResultRow.toPriceOverride() = PriceOverride(
    id = this[PriceOverrides.id].toString(),  // Convert UUID to String
    productId = this[PriceOverrides.productId],
    tier = this[PriceOverrides.tier].name,
    overridePrice = this[PriceOverrides.overridePrice].toDouble(),
    active = this[PriceOverrides.active],
    createdAt = this[PriceOverrides.createdAt].toString(),
    updatedAt = this[PriceOverrides.updatedAt].toString()
)
