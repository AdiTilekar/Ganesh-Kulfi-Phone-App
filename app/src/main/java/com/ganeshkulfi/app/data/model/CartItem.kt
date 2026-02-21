package com.ganeshkulfi.app.data.model

/**
 * DEPRECATED — This CartItem model is unused.
 * The active cart system uses [com.ganeshkulfi.app.presentation.viewmodel.CartViewModel]
 * and its [CartItemNew] / [CartSummaryNew] data classes.
 *
 * Safe to delete once confirmed no screen references this.
 */
@Deprecated("Unused — replaced by CartItemNew in CartViewModel")
data class CartItem(
    val flavor: Flavor,
    val quantity: Int = 1
) {
    val subtotal: Double
        get() = flavor.price * quantity.toDouble()
}
