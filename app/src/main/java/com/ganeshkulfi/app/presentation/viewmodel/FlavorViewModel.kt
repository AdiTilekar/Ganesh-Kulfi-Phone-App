package com.ganeshkulfi.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganeshkulfi.app.data.model.Flavor
import com.ganeshkulfi.app.data.repository.FlavorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlavorViewModel @Inject constructor(
    private val flavorRepository: FlavorRepository
) : ViewModel() {

    private val _flavors = MutableStateFlow<List<Flavor>>(emptyList())
    val flavors: StateFlow<List<Flavor>> = _flavors.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadFlavors()
    }

    private fun loadFlavors() {
        viewModelScope.launch {
            _isLoading.value = true
            flavorRepository.flavorsFlow.collect { flavors ->
                _flavors.value = flavors
                _isLoading.value = false
            }
        }
    }

    fun refreshFlavors() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = flavorRepository.getFlavors()
            _isLoading.value = false
            
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to load flavors"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
