package com.ganeshkulfi.backend.services

import com.ganeshkulfi.backend.data.dto.*
import com.ganeshkulfi.backend.data.models.ProductCategory
import com.ganeshkulfi.backend.data.models.ProductStatus
import com.ganeshkulfi.backend.data.models.UserRole
import com.ganeshkulfi.backend.data.repository.ProductRepository

/**
 * Product Service
 * Business logic for product operations
 */
class ProductService(private val productRepository: ProductRepository) {
    
    /**
     * Get all products (Admin view - includes stock)
     */
    fun getAllProducts(): Result<List<ProductResponse>> {
        val products = productRepository.findAll()
        return Result.success(products.map { ProductResponse.fromProduct(it, includeStock = true) })
    }
    
    /**
     * Get available products only (Retailer view - no stock info)
     */
    fun getAvailableProducts(): Result<List<ProductResponse>> {
        val products = productRepository.findAvailable()
        return Result.success(products.map { ProductResponse.fromProduct(it, includeStock = false) })
    }
    
    /**
     * Get seasonal products (Retailer view - no stock info)
     */
    fun getSeasonalProducts(): Result<List<ProductResponse>> {
        val products = productRepository.findSeasonal()
        return Result.success(products.map { ProductResponse.fromProduct(it, includeStock = false) })
    }
    
    /**
     * Get product by ID
     * @param includeStock true for admin, false for retailer
     */
    fun getProductById(id: String, includeStock: Boolean = false): Result<ProductResponse> {
        val product = productRepository.findById(id)
            ?: return Result.failure(IllegalArgumentException("Product not found"))
        
        return Result.success(ProductResponse.fromProduct(product, includeStock = includeStock))
    }
    
    /**
     * Get products by category (Retailer view - no stock info)
     */
    fun getProductsByCategory(categoryName: String): Result<List<ProductResponse>> {
        val category = try {
            ProductCategory.valueOf(categoryName.uppercase())
        } catch (e: IllegalArgumentException) {
            return Result.failure(IllegalArgumentException("Invalid category: $categoryName"))
        }
        
        val products = productRepository.findByCategory(category)
        return Result.success(products.map { ProductResponse.fromProduct(it, includeStock = false) })
    }
    
    /**
     * Create new product (Admin only)
     */
    fun createProduct(request: CreateProductRequest): Result<ProductResponse> {
        // Validate product name
        if (request.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Product name is required"))
        }
        
        // Check if name already exists
        if (productRepository.nameExists(request.name)) {
            return Result.failure(IllegalArgumentException("Product with this name already exists"))
        }
        
        // Validate price
        if (request.basePrice <= 0) {
            return Result.failure(IllegalArgumentException("Price must be greater than zero"))
        }
        
        // Validate category
        val category = try {
            ProductCategory.valueOf(request.category.uppercase())
        } catch (e: IllegalArgumentException) {
            return Result.failure(IllegalArgumentException("Invalid category: ${request.category}"))
        }
        
        // Create product
        val product = productRepository.create(
            name = request.name,
            description = request.description,
            basePrice = request.basePrice,
            category = category,
            imageUrl = request.imageUrl,
            isAvailable = request.isAvailable,
            isSeasonal = request.isSeasonal,
            stockQuantity = request.stockQuantity,
            minOrderQuantity = request.minOrderQuantity
        ) ?: return Result.failure(IllegalStateException("Failed to create product"))
        
        return Result.success(ProductResponse.fromProduct(product, includeStock = true))
    }
    
    /**
     * Update product (Admin only)
     */
    fun updateProduct(id: String, request: UpdateProductRequest): Result<ProductResponse> {
        // Check if product exists
        val existingProduct = productRepository.findById(id)
            ?: return Result.failure(IllegalArgumentException("Product not found"))
        
        // Build updates map
        val updates = mutableMapOf<String, Any?>()
        
        request.name?.let {
            if (it.isBlank()) {
                return Result.failure(IllegalArgumentException("Product name cannot be empty"))
            }
            // Check if new name already exists (excluding current product)
            if (it != existingProduct.name && productRepository.nameExists(it)) {
                return Result.failure(IllegalArgumentException("Product with this name already exists"))
            }
            updates["name"] = it
        }
        
        request.description?.let { updates["description"] = it }
        
        request.basePrice?.let {
            if (it <= 0) {
                return Result.failure(IllegalArgumentException("Price must be greater than zero"))
            }
            updates["basePrice"] = it
        }
        
        request.category?.let { categoryStr ->
            val category = try {
                ProductCategory.valueOf(categoryStr.uppercase())
            } catch (e: IllegalArgumentException) {
                return Result.failure(IllegalArgumentException("Invalid category: $categoryStr"))
            }
            updates["category"] = category
        }
        
        request.imageUrl?.let { updates["imageUrl"] = it }
        request.isAvailable?.let { updates["isAvailable"] = it }
        request.isSeasonal?.let { updates["isSeasonal"] = it }
        request.minOrderQuantity?.let { updates["minOrderQuantity"] = it }
        
        // Update product
        val updatedProduct = productRepository.update(id, updates)
            ?: return Result.failure(IllegalStateException("Failed to update product"))
        
        return Result.success(ProductResponse.fromProduct(updatedProduct, includeStock = true))
    }
    
    /**
     * Delete product (Admin only)
     */
    fun deleteProduct(id: String): Result<Unit> {
        val deleted = productRepository.delete(id)
        return if (deleted) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Product not found"))
        }
    }
    
    /**
     * Update stock quantity
     */
    fun updateStock(id: String, quantity: Int): Result<Unit> {
        if (quantity < 0) {
            return Result.failure(IllegalArgumentException("Stock quantity cannot be negative"))
        }
        
        val updated = productRepository.updateStock(id, quantity)
        return if (updated) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Product not found"))
        }
    }
    
    /**
     * Create new product (Factory owner only)
     */
    fun createProductFactory(request: CreateProductRequest, userRole: UserRole): Result<ProductInventoryResponse> {
        if (userRole != UserRole.ADMIN) {
            return Result.failure(SecurityException("Only factory owners can create products"))
        }
        
        if (request.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Product name is required"))
        }
        
        if (productRepository.nameExists(request.name)) {
            return Result.failure(IllegalArgumentException("Product with this name already exists"))
        }
        
        if (request.basePrice <= 0) {
            return Result.failure(IllegalArgumentException("Price must be greater than zero"))
        }
        
        val category = try {
            ProductCategory.valueOf(request.category.uppercase())
        } catch (e: IllegalArgumentException) {
            return Result.failure(IllegalArgumentException("Invalid category: ${request.category}"))
        }
        
        val product = productRepository.createProduct(
            name = request.name,
            description = request.description,
            basePrice = request.basePrice,
            category = category,
            imageUrl = request.imageUrl,
            isAvailable = request.isAvailable,
            isSeasonal = request.isSeasonal,
            stockQuantity = request.stockQuantity,
            minOrderQuantity = request.minOrderQuantity
        )
        
        return Result.success(ProductInventoryResponse.fromProduct(product))
    }
    
    /**
     * Update product (Factory owner only)
     */
    fun updateProductFactory(id: String, request: UpdateProductRequest, userRole: UserRole): Result<ProductInventoryResponse> {
        if (userRole != UserRole.ADMIN) {
            return Result.failure(SecurityException("Only factory owners can update products"))
        }
        
        // Check if product exists
        productRepository.findById(id) ?: return Result.failure(IllegalArgumentException("Product not found"))
        
        val updates = mutableMapOf<String, Any?>()
        
        request.name?.let {
            if (it.isBlank()) return Result.failure(IllegalArgumentException("Product name cannot be empty"))
            updates["name"] = it
        }
        request.description?.let { updates["description"] = it }
        request.basePrice?.let {
            if (it <= 0) return Result.failure(IllegalArgumentException("Price must be greater than zero"))
            updates["basePrice"] = it
        }
        request.category?.let {
            try {
                ProductCategory.valueOf(it.uppercase())
                updates["category"] = it.uppercase()
            } catch (e: IllegalArgumentException) {
                return Result.failure(IllegalArgumentException("Invalid category: $it"))
            }
        }
        request.imageUrl?.let { updates["imageUrl"] = it }
        request.isAvailable?.let { updates["isAvailable"] = it }
        request.isSeasonal?.let { updates["isSeasonal"] = it }
        request.minOrderQuantity?.let {
            if (it < 1) return Result.failure(IllegalArgumentException("Min order quantity must be at least 1"))
            updates["minOrderQuantity"] = it
        }
        request.status?.let {
            try {
                ProductStatus.valueOf(it.uppercase())
                updates["status"] = it.uppercase()
            } catch (e: IllegalArgumentException) {
                return Result.failure(IllegalArgumentException("Invalid status: $it"))
            }
        }
        
        val updated = productRepository.updateProduct(id, updates)
            ?: return Result.failure(IllegalStateException("Failed to update product"))
        
        return Result.success(ProductInventoryResponse.fromProduct(updated))
    }
    
    /**
     * Get all products with inventory details (Factory owner only)
     */
    fun getAllProductsWithInventory(userRole: UserRole): Result<List<ProductInventoryResponse>> {
        if (userRole != UserRole.ADMIN) {
            return Result.failure(SecurityException("Only factory owners can view inventory details"))
        }
        
        val products = productRepository.getAllWithInventory()
        return Result.success(products.map { ProductInventoryResponse.fromProduct(it) })
    }
    
    /**
     * Day 13: Update product image
     */
    fun updateProductImage(id: String, imageUrl: String): Result<ProductResponse> {
        val product = productRepository.findById(id)
            ?: return Result.failure(IllegalArgumentException("Product not found"))
        
        val updated = productRepository.updateProductImage(id, imageUrl)
            ?: return Result.failure(IllegalStateException("Failed to update product image"))
        
        return Result.success(ProductResponse.fromProduct(updated, includeStock = true))
    }
    
    /**
     * Day 13: Update product stock (Admin only)
     */
    fun updateProductStock(id: String, stockQuantity: Int): Result<ProductResponse> {
        if (stockQuantity < 0) {
            return Result.failure(IllegalArgumentException("Stock quantity cannot be negative"))
        }
        
        val product = productRepository.findById(id)
            ?: return Result.failure(IllegalArgumentException("Product not found"))
        
        val updated = productRepository.updateProductStock(id, stockQuantity)
            ?: return Result.failure(IllegalStateException("Failed to update product stock"))
        
        return Result.success(ProductResponse.fromProduct(updated, includeStock = true))
    }
    
    /**
     * Day 13: Activate product
     */
    fun activateProduct(id: String): Result<ProductResponse> {
        val product = productRepository.findById(id)
            ?: return Result.failure(IllegalArgumentException("Product not found"))
        
        val updated = productRepository.activateProduct(id)
            ?: return Result.failure(IllegalStateException("Failed to activate product"))
        
        return Result.success(ProductResponse.fromProduct(updated, includeStock = true))
    }
    
    /**
     * Day 13: Deactivate product
     */
    fun deactivateProduct(id: String): Result<ProductResponse> {
        val product = productRepository.findById(id)
            ?: return Result.failure(IllegalArgumentException("Product not found"))
        
        val updated = productRepository.deactivateProduct(id)
            ?: return Result.failure(IllegalStateException("Failed to deactivate product"))
        
        return Result.success(ProductResponse.fromProduct(updated, includeStock = true))
    }
}
