package com.ganeshkulfi.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganeshkulfi.app.data.remote.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Note: Using different package prefix to avoid conflicts with RetailerViewModel CartItem
data class CartItemNew(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double,
    val imageUrl: String? = null
)

data class CartSummaryNew(
    val subtotal: Double,
    val itemCount: Int,
    val totalItems: Int
)

@HiltViewModel
class CartViewModel @Inject constructor() : ViewModel() {
    
    private val _cartItems = MutableStateFlow<Map<String, CartItemNew>>(emptyMap())
    val cartItems: StateFlow<Map<String, CartItemNew>> = _cartItems.asStateFlow()
    
    private val _cartSummary = MutableStateFlow(CartSummaryNew(0.0, 0, 0))
    val cartSummary: StateFlow<CartSummaryNew> = _cartSummary.asStateFlow()
    
    fun addToCart(product: Product, quantity: Int = 1) {
        viewModelScope.launch {
            val currentCart = _cartItems.value.toMutableMap()
            val existingItem = currentCart[product.id]
            
            val newQuantity = if (existingItem != null) {
                existingItem.quantity + quantity
            } else {
                quantity
            }
            
            val lineTotal = product.basePrice * newQuantity
            
            currentCart[product.id] = CartItemNew(
                productId = product.id,
                productName = product.name,
                quantity = newQuantity,
                unitPrice = product.basePrice,
                lineTotal = lineTotal,
                imageUrl = null
            )
            
            _cartItems.value = currentCart
            updateCartSummary()
        }
    }
    
    fun updateQuantity(productId: String, newQuantity: Int) {
        viewModelScope.launch {
            if (newQuantity <= 0) {
                removeFromCart(productId)
                return@launch
            }
            
            val currentCart = _cartItems.value.toMutableMap()
            val item = currentCart[productId] ?: return@launch
            
            val lineTotal = item.unitPrice * newQuantity
            currentCart[productId] = item.copy(
                quantity = newQuantity,
                lineTotal = lineTotal
            )
            
            _cartItems.value = currentCart
            updateCartSummary()
        }
    }
    
    fun removeFromCart(productId: String) {
        viewModelScope.launch {
            val currentCart = _cartItems.value.toMutableMap()
            currentCart.remove(productId)
            _cartItems.value = currentCart
            updateCartSummary()
        }
    }
    
    fun clearCart() {
        viewModelScope.launch {
            _cartItems.value = emptyMap()
            updateCartSummary()
        }
    }
    
    fun getQuantityForProduct(productId: String): Int {
        return _cartItems.value[productId]?.quantity ?: 0
    }
    
    private fun updateCartSummary() {
        val items = _cartItems.value.values
        val subtotal = items.sumOf { it.lineTotal }
        val itemCount = items.size
        val totalItems = items.sumOf { it.quantity }
        
        _cartSummary.value = CartSummaryNew(
            subtotal = subtotal,
            itemCount = itemCount,
            totalItems = totalItems
        )
    }
}
