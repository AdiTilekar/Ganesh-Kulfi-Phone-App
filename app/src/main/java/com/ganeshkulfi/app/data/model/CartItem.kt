package com.ganeshkulfi.app.data.model

data class CartItem(
    val flavor: Flavor,
    val quantity: Int = 1
) {
    val subtotal: Double
        get() = flavor.price * quantity.toDouble()
}
