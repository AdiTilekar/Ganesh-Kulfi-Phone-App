package com.ganeshkulfi.app.data.repository

import com.ganeshkulfi.app.data.model.Flavor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlavorRepository @Inject constructor() {
    
    private val _flavors = MutableStateFlow(Flavor.getDefaultFlavors())
    val flavorsFlow: Flow<List<Flavor>> = _flavors.asStateFlow()

    suspend fun getFlavors(): Result<List<Flavor>> {
        return try {
            Result.success(_flavors.value)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFlavorById(id: String): Result<Flavor> {
        return try {
            val flavor = _flavors.value.find { it.key == id } 
                ?: throw Exception("Flavor not found")
            Result.success(flavor)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFlavor(flavor: Flavor): Result<Unit> {
        return try {
            val updatedList = _flavors.value.map { 
                if (it.key == flavor.key) flavor else it 
            }
            _flavors.value = updatedList
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStock(flavorKey: String, newStock: Int): Result<Unit> {
        return try {
            val updatedList = _flavors.value.map { 
                if (it.key == flavorKey) it.copy(stock = newStock) else it 
            }
            _flavors.value = updatedList
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addFlavor(flavor: Flavor): Result<String> {
        return try {
            _flavors.value = _flavors.value + flavor
            Result.success(flavor.key)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFlavor(flavorKey: String): Result<Unit> {
        return try {
            _flavors.value = _flavors.value.filter { it.key != flavorKey }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
