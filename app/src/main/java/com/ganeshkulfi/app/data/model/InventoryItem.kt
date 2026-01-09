package com.ganeshkulfi.app.data.model

data class InventoryItem(
    val flavorId: String = "",
    val flavorName: String = "",
    val totalStock: Int = 0,
    val availableStock: Int = 0,
    val stockGivenToRetailers: Int = 0,
    val soldToday: Int = 0,
    val soldThisWeek: Int = 0,
    val soldThisMonth: Int = 0,
    val soldQuantity: Int = 0, // Total quantity sold
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val reorderLevel: Int = 20,
    val lastRestockedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val needsRestock: Boolean
        get() = availableStock <= reorderLevel
    
    val profitMargin: Double
        get() = if (costPrice > 0) ((sellingPrice - costPrice) / costPrice) * 100 else 0.0
    
    val totalProfit: Double
        get() = soldQuantity * (sellingPrice - costPrice)
}
