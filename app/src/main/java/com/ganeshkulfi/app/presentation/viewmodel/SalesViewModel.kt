package com.ganeshkulfi.app.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganeshkulfi.app.data.remote.CreateSaleRequest
import com.ganeshkulfi.app.data.remote.DailySalesSummaryResponse
import com.ganeshkulfi.app.data.remote.SaleResponse
import com.ganeshkulfi.app.data.repository.ProductRepository
import com.ganeshkulfi.app.data.repository.SaleRepository
import com.ganeshkulfi.app.data.remote.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val saleRepository: SaleRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    // ── Product list (for dropdown) ─────────────────────────────────────────

    val products: StateFlow<List<Product>> = productRepository.productsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Daily sales data ────────────────────────────────────────────────────

    private val _salesSummary = MutableStateFlow<DailySalesSummaryResponse?>(null)
    val salesSummary: StateFlow<DailySalesSummaryResponse?> = _salesSummary.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now().toString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // ── Loading / error ─────────────────────────────────────────────────────

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ── Add-sale status ─────────────────────────────────────────────────────

    private val _addSaleState = MutableStateFlow<AddSaleState>(AddSaleState.Idle)
    val addSaleState: StateFlow<AddSaleState> = _addSaleState.asStateFlow()

    // ── Export status ───────────────────────────────────────────────────────

    private val _exportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val exportStatus: StateFlow<ExportStatus> = _exportStatus.asStateFlow()

    init {
        viewModelScope.launch {
            productRepository.fetchProducts()
        }
        loadSalesForDate(LocalDate.now().toString())
    }

    // ── Public API ──────────────────────────────────────────────────────────

    fun selectDate(date: String) {
        _selectedDate.value = date
        loadSalesForDate(date)
    }

    fun loadSalesForDate(date: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            saleRepository.getSalesForDate(date).fold(
                onSuccess = { _salesSummary.value = it },
                onFailure = { _errorMessage.value = it.message }
            )
            _isLoading.value = false
        }
    }

    fun addSale(
        productId: String,
        quantity: Int,
        sellPrice: Double,
        paymentMethod: String,
        note: String?
    ) {
        if (productId.isBlank()) {
            _addSaleState.value = AddSaleState.Error("Please select a product")
            return
        }
        if (quantity <= 0) {
            _addSaleState.value = AddSaleState.Error("Quantity must be at least 1")
            return
        }
        if (sellPrice < 0) {
            _addSaleState.value = AddSaleState.Error("Sell price cannot be negative")
            return
        }

        viewModelScope.launch {
            _addSaleState.value = AddSaleState.Loading
            val request = CreateSaleRequest(
                productId      = productId,
                quantity       = quantity,
                sellPrice      = sellPrice,
                paymentMethod  = paymentMethod,
                note           = note.takeUnless { it.isNullOrBlank() },
                idempotencyKey = UUID.randomUUID().toString()
            )
            saleRepository.addSale(request).fold(
                onSuccess = { sale ->
                    _addSaleState.value = AddSaleState.Success(sale)
                    // Refresh today's list if the new sale is for today
                    if (_selectedDate.value == LocalDate.now().toString()) {
                        loadSalesForDate(_selectedDate.value)
                    }
                },
                onFailure = { _addSaleState.value = AddSaleState.Error(it.message ?: "Failed to record sale") }
            )
        }
    }

    fun exportToExcel(context: Context, date: String) {
        viewModelScope.launch {
            _exportStatus.value = ExportStatus.Loading
            saleRepository.exportSales(context, date).fold(
                onSuccess = { path -> _exportStatus.value = ExportStatus.Success(path) },
                onFailure = { _exportStatus.value = ExportStatus.Error(it.message ?: "Export failed") }
            )
        }
    }

    fun resetAddSaleState()  { _addSaleState.value  = AddSaleState.Idle }
    fun resetExportStatus()  { _exportStatus.value  = ExportStatus.Idle }
    fun clearError()         { _errorMessage.value  = null }
}

// ── Sealed states ────────────────────────────────────────────────────────────

sealed class AddSaleState {
    object Idle : AddSaleState()
    object Loading : AddSaleState()
    data class Success(val sale: SaleResponse) : AddSaleState()
    data class Error(val message: String) : AddSaleState()
}

sealed class ExportStatus {
    object Idle : ExportStatus()
    object Loading : ExportStatus()
    data class Success(val filePath: String) : ExportStatus()
    data class Error(val message: String) : ExportStatus()
}
