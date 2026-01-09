package com.ganeshkulfi.app.data.model

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val role: UserRole = UserRole.CUSTOMER,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Retailer-specific fields (only used when role = RETAILER)
    val retailerId: String? = null,  // Links to Retailer data model
    val shopName: String? = null,
    val pricingTier: PricingTier? = null
)

enum class UserRole {
    CUSTOMER,
    RETAILER,  // New role for wholesale retailers
    ADMIN
}
