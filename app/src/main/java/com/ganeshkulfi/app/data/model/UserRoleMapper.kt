package com.ganeshkulfi.app.data.model

/**
 * Maps user emails to their roles and retailer information
 * This allows automatic role detection during OAuth login
 */
object UserRoleMapper {
    
    // Admin emails - Full system access
    private val adminEmails = setOf(
        "admin@ganeshkulfi.com",
        "ganeshkulfi@gmail.com",
        "owner@ganeshkulfi.com"
    )
    
    // Retailer email mappings with their details
    private val retailerMappings = mapOf(
        // Format: email to Retailer Info
        "retailer@test.com" to RetailerInfo(
            id = "ret_001",
            name = "Rajesh Kumar",
            shopName = "Kumar Sweet Shop",
            phone = "9876543210",
            pricingTier = PricingTier.GOLD,
            address = "MG Road, Near City Mall, Kopargaon",
            gstNumber = "27AABCU9603R1Z5"
        ),
        "kumar@shop.com" to RetailerInfo(
            id = "ret_001",
            name = "Rajesh Kumar",
            shopName = "Kumar Sweet Shop",
            phone = "9876543210",
            pricingTier = PricingTier.GOLD,
            address = "MG Road, Near City Mall, Kopargaon",
            gstNumber = "27AABCU9603R1Z5"
        ),
        "priya@shop.com" to RetailerInfo(
            id = "ret_002",
            name = "Priya Sharma",
            shopName = "Sharma Ice Cream Parlor",
            phone = "9876543211",
            pricingTier = PricingTier.SILVER,
            address = "Station Road, Kopargaon",
            gstNumber = "27BBCDU9603R1Z6"
        ),
        "amit@sweets.com" to RetailerInfo(
            id = "ret_003",
            name = "Amit Patel",
            shopName = "Patel Sweets & Snacks",
            phone = "9876543212",
            pricingTier = PricingTier.SILVER,
            address = "Market Yard, Kopargaon",
            gstNumber = "27CCDEU9603R1Z7"
        ),
        "suresh@icecream.com" to RetailerInfo(
            id = "ret_004",
            name = "Suresh Reddy",
            shopName = "Reddy Ice Cream Corner",
            phone = "9876543213",
            pricingTier = PricingTier.BASIC,
            address = "Bus Stand Road, Kopargaon",
            gstNumber = "27DDEFU9603R1Z8"
        ),
        "deepak@store.com" to RetailerInfo(
            id = "ret_005",
            name = "Deepak Gupta",
            shopName = "Gupta General Store",
            phone = "9876543214",
            pricingTier = PricingTier.BASIC,
            address = "College Road, Kopargaon",
            gstNumber = "27EEFGU9603R1Z9"
        )
    )
    
    /**
     * Determines user role based on email address
     */
    fun getUserRole(email: String): UserRole {
        return when {
            adminEmails.contains(email.lowercase()) -> UserRole.ADMIN
            retailerMappings.containsKey(email.lowercase()) -> UserRole.RETAILER
            else -> UserRole.CUSTOMER
        }
    }
    
    /**
     * Gets retailer information for a given email
     * Returns null if email is not a registered retailer
     */
    fun getRetailerInfo(email: String): RetailerInfo? {
        return retailerMappings[email.lowercase()]
    }
    
    /**
     * Checks if email is registered as admin
     */
    fun isAdmin(email: String): Boolean {
        return adminEmails.contains(email.lowercase())
    }
    
    /**
     * Checks if email is registered as retailer
     */
    fun isRetailer(email: String): Boolean {
        return retailerMappings.containsKey(email.lowercase())
    }
    
    /**
     * Gets all registered retailer emails (for admin reference)
     */
    fun getAllRetailerEmails(): List<String> {
        return retailerMappings.keys.toList()
    }
    
    /**
     * Adds a new retailer mapping (for dynamic registration)
     * In production, this would be done through admin panel and stored in backend
     */
    fun addRetailer(email: String, info: RetailerInfo) {
        // Note: This is a temporary in-memory solution
        // In production, store in Firebase/Backend
        (retailerMappings as MutableMap)[email.lowercase()] = info
    }
}

/**
 * Retailer information data class
 */
data class RetailerInfo(
    val id: String,
    val name: String,
    val shopName: String,
    val phone: String,
    val pricingTier: PricingTier,
    val address: String = "",
    val gstNumber: String = "",
    val city: String = "Kopargaon",
    val creditLimit: Double = 50000.0,
    val isActive: Boolean = true
)
