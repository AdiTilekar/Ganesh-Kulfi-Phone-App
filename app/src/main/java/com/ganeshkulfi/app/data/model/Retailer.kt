package com.ganeshkulfi.app.data.model

data class Retailer(
    val id: String = "",
    val userId: String = "",  // Backend user ID for API operations
    val name: String = "",
    val shopName: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",  // Store credentials for retailer login
    val address: String = "",
    val city: String = "",
    val pincode: String = "",
    val gstNumber: String = "",
    val pricingTier: PricingTier = PricingTier.SILVER,  // Default pricing tier
    val isActive: Boolean = true,
    val totalOutstanding: Double = 0.0,
    val creditLimit: Double = 0.0,
    val notes: String = "",  // Admin notes about retailer
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun getSampleRetailers(): List<Retailer> = listOf(
            Retailer(
                id = "ret_001",
                name = "Rajesh Kumar",
                shopName = "Kumar Sweet Shop",
                phone = "9876543210",
                email = "kumarsweetshop@ganeshkulfi.com",
                password = "GK123456",
                address = "MG Road, Near City Mall",
                city = "Kopargaon",
                pincode = "423601",
                gstNumber = "27AABCU9603R1Z5",
                pricingTier = PricingTier.GOLD,  // Gold tier - 20% discount
                isActive = true,
                totalOutstanding = 5000.0,
                creditLimit = 50000.0,
                notes = "Long-term customer, always pays on time"
            ),
            Retailer(
                id = "ret_002",
                name = "Priya Sharma",
                shopName = "Sharma Ice Cream Parlor",
                phone = "9876543211",
                email = "sharmaicecreamparlor@ganeshkulfi.com",
                password = "GK789012",
                address = "Station Road",
                city = "Kopargaon",
                pincode = "423601",
                gstNumber = "27BBCDU9603R1Z6",
                pricingTier = PricingTier.SILVER,  // Silver tier - 10% discount
                isActive = true,
                totalOutstanding = 3200.0,
                creditLimit = 30000.0,
                notes = "Regular high-volume buyer"
            )
        )
    }
}
