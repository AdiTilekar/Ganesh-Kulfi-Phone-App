package com.ganeshkulfi.backend.data.dto

import com.ganeshkulfi.backend.data.models.Product
import com.ganeshkulfi.backend.data.models.ProductCategory
import kotlinx.serialization.Serializable

/**
 * Product Response DTO
 * stockQuantity is null for retailer access (hidden), only visible to admin
 */
@Serializable
data class ProductResponse(
    val id: String,
    val name: String,
    val description: String?,
    val basePrice: Double,
    val category: String,
    val imageUrl: String?,
    val isAvailable: Boolean,
    val isSeasonal: Boolean,
    val stockQuantity: Int? = null, // Hidden from retailers, only admin sees this
    val minOrderQuantity: Int,
    val isActive: Boolean, // Day 13: Product activation status
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        /**
         * Convert Product to Response DTO
         * @param includeStock true to include stock info (admin only), false to hide (retailers)
         */
        fun fromProduct(product: Product, includeStock: Boolean = false): ProductResponse {
            return ProductResponse(
                id = product.id,
                name = product.name,
                description = product.description,
                basePrice = product.basePrice,
                category = product.category.name,
                imageUrl = product.imageUrl,
                isAvailable = product.isAvailable,
                isSeasonal = product.isSeasonal,
                stockQuantity = if (includeStock) product.stockQuantity else null,
                minOrderQuantity = product.minOrderQuantity,
                isActive = product.isActive, // Day 13
                createdAt = product.createdAt.toString(),
                updatedAt = product.updatedAt.toString()
            )
        }
    }
}

/**
 * Create Product Request DTO
 */
@Serializable
data class CreateProductRequest(
    val name: String,
    val description: String?,
    val basePrice: Double,
    val category: String,
    val imageUrl: String? = null,
    val isAvailable: Boolean = true,
    val isSeasonal: Boolean = false,
    val stockQuantity: Int = 0,
    val minOrderQuantity: Int = 1
)

/**
 * Update Product Request DTO
 */
@Serializable
data class UpdateProductRequest(
    val name: String? = null,
    val description: String? = null,
    val basePrice: Double? = null,
    val category: String? = null,
    val imageUrl: String? = null,
    val isAvailable: Boolean? = null,
    val isSeasonal: Boolean? = null,
    val minOrderQuantity: Int? = null,
    val status: String? = null
)

/**
 * Products List Response
 */
@Serializable
data class ProductsListResponse(
    val products: List<ProductResponse>,
    val total: Int,
    val category: String? = null
)
