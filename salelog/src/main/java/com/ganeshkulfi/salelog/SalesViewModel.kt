package com.ganeshkulfi.salelog

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganeshkulfi.salelog.data.CartItem
import com.ganeshkulfi.salelog.data.CreateSaleRequest
import com.ganeshkulfi.salelog.data.DailySummaryResponse
import com.ganeshkulfi.salelog.data.Product
import com.ganeshkulfi.salelog.data.SaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import javax.inject.Inject

sealed class ExportStatus {
    object Idle : ExportStatus()
    object Loading : ExportStatus()
    data class Success(val filePath: String) : ExportStatus()
    data class Error(val message: String) : ExportStatus()
}

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val repository: SaleRepository
) : ViewModel() {

    // ---- Products & Cart ----
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    val cartTotal: StateFlow<Double> get() = _cartTotal
    private val _cartTotal = MutableStateFlow(0.0)

    // ---- UI state ----
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _saleConfirmed = MutableStateFlow(false)
    val saleConfirmed: StateFlow<Boolean> = _saleConfirmed.asStateFlow()

    // ---- Reports ----
    private val _selectedDate = MutableStateFlow(LocalDate.now().toString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _summary = MutableStateFlow<DailySummaryResponse?>(null)
    val summary: StateFlow<DailySummaryResponse?> = _summary.asStateFlow()

    private val _exportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val exportStatus: StateFlow<ExportStatus> = _exportStatus.asStateFlow()

    // ---- Backend connectivity test ----
    private val _pingMessage = MutableStateFlow<String?>(null)
    val pingMessage: StateFlow<String?> = _pingMessage.asStateFlow()

    fun pingBackend() {
        viewModelScope.launch {
            _pingMessage.value = "Testing…"
            repository.getProducts()
                .onSuccess { products ->
                    _pingMessage.value = "✓ Connected — ${products.size} products loaded"
                }
                .onFailure { e ->
                    _pingMessage.value = "✗ Failed: ${e.message}"
                }
        }
    }

    fun clearPingMessage() { _pingMessage.value = null }

    init { loadProducts() }

    // ---- Products ----
    fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.getProducts()
                .onSuccess { _products.value = it }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    // ---- Cart management ----
    fun updateQuantity(product: Product, delta: Int) {
        _cart.update { current ->
            val existing = current.find { it.product.id == product.id }
            val updated = when {
                existing == null && delta > 0 ->
                    current + CartItem(product, delta, product.basePrice)
                existing != null -> {
                    val newQty = existing.quantity + delta
                    if (newQty <= 0) current.filter { it.product.id != product.id }
                    else current.map {
                        if (it.product.id == product.id) it.copy(quantity = newQty) else it
                    }
                }
                else -> current
            }
            recalcTotal(updated)
            updated
        }
    }

    fun updateSellPrice(productId: String, price: Double) {
        _cart.update { current ->
            val updated = current.map {
                if (it.product.id == productId) it.copy(sellPrice = price) else it
            }
            recalcTotal(updated)
            updated
        }
    }

    fun getCartQty(productId: String): Int =
        _cart.value.find { it.product.id == productId }?.quantity ?: 0

    private fun recalcTotal(items: List<CartItem>) {
        _cartTotal.value = items.sumOf { it.quantity * it.sellPrice }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _cartTotal.value = 0.0
    }

    fun resetSaleConfirmed() { _saleConfirmed.value = false }

    fun confirmSale(paymentMethod: String, note: String) {
        val items = _cart.value
        if (items.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val date = LocalDate.now().toString()
            var allOk = true

            for (item in items) {
                val req = CreateSaleRequest(
                    productId     = item.product.id,
                    quantity      = item.quantity,
                    sellPrice     = item.sellPrice,
                    paymentMethod = paymentMethod,
                    note          = note,
                    idempotencyKey = null
                )
                repository.createSale(req).onFailure {
                    allOk = false
                    _errorMessage.value = it.message
                }
                if (!allOk) break
            }

            if (allOk) {
                clearCart()
                _saleConfirmed.value = true
            }
            _isLoading.value = false
        }
    }

    // ---- Reports ----
    fun selectDate(date: String) {
        _selectedDate.value = date
        loadReports(date)
    }

    fun loadReports(date: String = _selectedDate.value) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.getSales(date)
                .onSuccess { _summary.value = it }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun exportExcel(context: Context, date: String) {
        viewModelScope.launch {
            _exportStatus.value = ExportStatus.Loading
            repository.exportSales(date)
                .onSuccess { body ->
                    try {
                        val dir = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS
                        )
                        val file = File(dir, "sales_$date.xlsx")
                        FileOutputStream(file).use { out ->
                            body.byteStream().copyTo(out)
                        }
                        _exportStatus.value = ExportStatus.Success(file.absolutePath)
                    } catch (e: Exception) {
                        _exportStatus.value = ExportStatus.Error(e.message ?: "Save failed")
                    }
                }
                .onFailure { _exportStatus.value = ExportStatus.Error(it.message ?: "Export failed") }
        }
    }

    fun resetExportStatus() { _exportStatus.value = ExportStatus.Idle }
}
