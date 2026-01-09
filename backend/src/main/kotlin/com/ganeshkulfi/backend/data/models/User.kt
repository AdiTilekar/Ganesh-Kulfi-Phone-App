package com.ganeshkulfi.backend.data.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.Column
import org.postgresql.util.PGobject
import java.time.Instant

/**
 * User Roles for Role-Based Access Control
 */
enum class UserRole {
    ADMIN,      // Factory owner - full access
    RETAILER,   // Bulk buyer - can place orders
    CUSTOMER,   // End user - can buy products
    GUEST       // Temporary user
}

/**
 * Day 9: Retailer Tier for Advanced Pricing System
 * Server-side only - NEVER exposed to retailer API responses
 */
enum class RetailerTier {
    BASIC,
    SILVER,
    GOLD;

    companion object {
        fun fromString(value: String): RetailerTier? {
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }
    }
}

/**
 * Users Table - Exposed ORM Table Definition
 * Maps to app_user table in PostgreSQL
 */
object Users : UUIDTable("app_user") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val name = varchar("name", 100)
    val phone = varchar("phone", 20).nullable()
    val role = customEnumeration(
        "role",
        "user_role",
        { value -> UserRole.valueOf(value as String) },
        { PGobject().apply { type = "user_role"; value = it.name } }
    ).default(UserRole.CUSTOMER)
    
    // Retailer-specific fields
    val retailerId = varchar("retailer_id", 50).nullable().uniqueIndex()
    val shopName = varchar("shop_name", 200).nullable()
    val tier = customEnumeration(
        "tier",
        "retailer_tier",
        { value -> RetailerTier.valueOf(value as String) },
        { PGobject().apply { type = "retailer_tier"; value = it.name } }
    ).default(RetailerTier.BASIC) // Day 9: retailer_tier enum in DB
    
    // Timestamps
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())
}

/**
 * User Domain Model
 * Represents a user in the system
 */
data class User(
    val id: String,
    val email: String,
    val passwordHash: String,
    val name: String,
    val phone: String? = null,
    val role: UserRole = UserRole.CUSTOMER,
    val retailerId: String? = null,
    val shopName: String? = null,
    val tier: RetailerTier = RetailerTier.BASIC, // Day 9: RetailerTier enum
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
