package com.ganeshkulfi.app.data.model

/**
 * Product Catalog Item - Public product information WITHOUT factory stock data
 * This is what retailers and customers should see
 * 
 * DOES NOT include:
 * - Available stock levels (factory secret)
 * - Total stock (factory secret)
 * - Stock given to retailers (factory secret)
 * - Reorder levels (factory secret)
 * - Cost prices (factory secret)
 * 
 * IMPORTANT: Neither retailers nor customers are limited by stock
 * Factory produces on demand to fulfill ANY order quantity
 */
data class ProductCatalogItem(
    val flavorId: String,
    val flavorName: String,
    val sellingPrice: Double
)

/**
 * Convert InventoryItem to ProductCatalogItem
 * Strips away all factory-sensitive information
 * No availability check - factory produces on demand
 */
fun InventoryItem.toProductCatalogItem(): ProductCatalogItem {
    return ProductCatalogItem(
        flavorId = this.flavorId,
        flavorName = this.flavorName,
        sellingPrice = this.sellingPrice
    )
}

/**
 * Convert list of InventoryItem to ProductCatalogItem list
 */
fun List<InventoryItem>.toProductCatalog(): List<ProductCatalogItem> {
    return this.map { it.toProductCatalogItem() }
}
