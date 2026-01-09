package com.ganeshkulfi.backend.data.repository

import com.ganeshkulfi.backend.data.models.User
import com.ganeshkulfi.backend.data.models.UserRole
import com.ganeshkulfi.backend.data.models.RetailerTier
import com.ganeshkulfi.backend.data.models.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

/**
 * User Repository
 * Handles all database operations for users
 */
class UserRepository {
    
    /**
     * Create a new user
     */
    fun create(
        email: String,
        passwordHash: String,
        name: String,
        phone: String? = null,
        role: UserRole = UserRole.CUSTOMER,
        retailerId: String? = null,
        shopName: String? = null,
        tier: String = "BASIC"  // Day 9: RetailerTier as String (will be converted)
    ): User? = transaction {
        val userId = UUID.randomUUID()
        val tierEnum = RetailerTier.fromString(tier) ?: RetailerTier.BASIC
        
        Users.insert {
            it[id] = userId
            it[Users.email] = email
            it[Users.passwordHash] = passwordHash
            it[Users.name] = name
            it[Users.phone] = phone
            it[Users.role] = role
            it[Users.retailerId] = retailerId
            it[Users.shopName] = shopName
            it[Users.tier] = tierEnum
            it[createdAt] = Instant.now()
            it[updatedAt] = Instant.now()
        }
        
        findById(userId.toString())
    }
    
    /**
     * Find user by ID
     */
    fun findById(id: String): User? = transaction {
        Users.select { Users.id eq UUID.fromString(id) }
            .mapNotNull { toUser(it) }
            .singleOrNull()
    }
    
    /**
     * Find user by email
     */
    fun findByEmail(email: String): User? = transaction {
        Users.select { Users.email eq email }
            .mapNotNull { toUser(it) }
            .singleOrNull()
    }
    
    /**
     * Find user by retailer ID
     */
    fun findByRetailerId(retailerId: String): User? = transaction {
        Users.select { Users.retailerId eq retailerId }
            .mapNotNull { toUser(it) }
            .singleOrNull()
    }
    
    /**
     * Get all users
     */
    fun findAll(): List<User> = transaction {
        Users.selectAll()
            .mapNotNull { toUser(it) }
    }
    
    /**
     * Get users by role
     */
    fun findByRole(role: UserRole): List<User> = transaction {
        Users.select { Users.role eq role }
            .mapNotNull { toUser(it) }
    }
    
    /**
     * Update user
     */
    fun update(id: String, updates: Map<String, Any?>): User? = transaction {
        Users.update({ Users.id eq UUID.fromString(id) }) {
            updates.forEach { (key, value) ->
                when (key) {
                    "name" -> it[name] = value as String
                    "phone" -> it[phone] = value as? String
                    "role" -> it[role] = UserRole.valueOf(value as String)
                    "retailerId" -> it[retailerId] = value as? String
                    "shopName" -> it[shopName] = value as? String
                    "tier" -> it[tier] = RetailerTier.fromString(value as String) ?: RetailerTier.BASIC  // Day 9: Convert String to enum
                    "passwordHash" -> it[passwordHash] = value as String
                }
            }
            it[updatedAt] = Instant.now()
        }
        findById(id)
    }
    
    /**
     * Delete user
     */
    fun delete(id: String): Boolean = transaction {
        Users.deleteWhere { Users.id eq UUID.fromString(id) } > 0
    }
    
    /**
     * Check if email exists
     */
    fun emailExists(email: String): Boolean = transaction {
        Users.select { Users.email eq email }.count() > 0
    }
    
    /**
     * Check if retailer ID exists
     */
    fun retailerIdExists(retailerId: String): Boolean = transaction {
        Users.select { Users.retailerId eq retailerId }.count() > 0
    }
    
    /**
     * Get total user count
     */
    fun count(): Long = transaction {
        Users.selectAll().count()
    }
    
    /**
     * Map database row to User domain model
     */
    private fun toUser(row: ResultRow): User {
        return User(
            id = row[Users.id].toString(),
            email = row[Users.email],
            passwordHash = row[Users.passwordHash],
            name = row[Users.name],
            phone = row[Users.phone],
            role = row[Users.role],
            retailerId = row[Users.retailerId],
            shopName = row[Users.shopName],
            tier = row[Users.tier],
            createdAt = row[Users.createdAt],
            updatedAt = row[Users.updatedAt]
        )
    }
}
