package com.ganeshkulfi.app.data.repository

import com.ganeshkulfi.app.data.model.Flavor
import com.ganeshkulfi.app.data.remote.ApiService
import com.ganeshkulfi.app.data.remote.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Product Repository for Retailers
 * 
 * Fetches products from backend WITHOUT stock information
 * Retailers should be able to order any quantity they need
 * Stock management is handled by the backend
 */
@Singleton
class ProductRepository @Inject constructor(
    private val apiService: ApiService
) {
    
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val productsFlow: Flow<List<Product>> = _products.asStateFlow()

    init {
        // Initialize with default flavors (fallback if backend unavailable)
        initializeDefaultProducts()
    }

    private fun initializeDefaultProducts() {
        val flavors = Flavor.getDefaultFlavors()
        _products.value = flavors.map { flavor ->
            Product(
                id = flavor.key,
                name = flavor.nameEn,
                description = flavor.descriptionEn,
                basePrice = flavor.price.toDouble(),
                imageUrl = flavor.image,
                isActive = flavor.isAvailable
            )
        }
    }

    /**
     * Fetch products from backend
     * NO stock information is included - retailers can order any quantity
     */
    suspend fun fetchProducts(): Result<List<Product>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getProducts()
                if (response.isSuccessful && response.body()?.success == true) {
                    val products = response.body()?.data?.products ?: emptyList()
                    _products.value = products
                    Result.success(products)
                } else {
                    val errorMsg = "Failed to fetch products (${response.code()})"
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                // Keep default products on error
                android.util.Log.e("ProductRepository", "Failed to fetch products from backend", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getProductById(productId: String): Result<Product> {
        return try {
            val product = _products.value.find { it.id == productId }
            if (product != null) {
                Result.success(product)
            } else {
                Result.failure(Exception("Product not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
