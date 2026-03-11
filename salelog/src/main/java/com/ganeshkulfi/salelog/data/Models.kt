package com.ganeshkulfi.salelog.data

// Generic API envelope — every backend response is wrapped in this
data class ApiWrapper<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)

// Matches backend ProductResponse DTO
data class Product(
    val id: String,           // UUID string
    val name: String,
    val description: String? = null,
    val basePrice: Double,    // backend field is basePrice
    val category: String = "",
    val isAvailable: Boolean = true,
    val isSeasonal: Boolean = false,
    val stockQuantity: Int? = null,
    val minOrderQuantity: Int = 1
)

// Matches backend ProductsListResponse
data class ProductsListData(
    val products: List<Product>,
    val total: Int
)

data class CartItem(
    val product: Product,
    var quantity: Int,
    var sellPrice: Double
)

data class CreateSaleRequest(
    val productId: String,
    val quantity: Int,
    val sellPrice: Double,
    val paymentMethod: String,
    val note: String?,
    val idempotencyKey: String? = null
)

data class SaleResponse(
    val id: String,
    val productName: String,
    val quantity: Int,
    val sellPrice: Double,
    val costPrice: Double,
    val profit: Double,
    val paymentMethod: String,
    val note: String?,
    val soldAt: String
)

data class DailySummaryResponse(
    val date: String = "",
    val sales: List<SaleResponse>,
    val totalRevenue: Double,
    val totalCost: Double,
    val totalProfit: Double,
    val totalUnits: Int
)
