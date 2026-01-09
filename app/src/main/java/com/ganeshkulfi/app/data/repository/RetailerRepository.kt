package com.ganeshkulfi.app.data.repository

import android.content.SharedPreferences
import com.ganeshkulfi.app.data.model.Retailer
import com.ganeshkulfi.app.data.model.PricingTier
import com.ganeshkulfi.app.data.model.UserRole
import com.ganeshkulfi.app.data.remote.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

// Helper function to parse ISO timestamp to epoch millis
private fun parseIsoTimestamp(isoString: String?): Long {
    if (isoString.isNullOrBlank()) return 0L
    return try {
        Instant.parse(isoString).toEpochMilli()
    } catch (e: DateTimeParseException) {
        android.util.Log.w("RetailerRepository", "Failed to parse timestamp: $isoString", e)
        0L
    }
}

@Singleton
class RetailerRepository @Inject constructor(
    private val apiService: ApiService,
    private val sharedPreferences: SharedPreferences
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _retailers = MutableStateFlow<List<Retailer>>(emptyList())
    val retailersFlow: Flow<List<Retailer>> = _retailers.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: Flow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: Flow<String?> = _error.asStateFlow()
    
    init {
        // Start auto-refresh every 30 seconds
        repositoryScope.launch {
            var failureCount = 0
            while (isActive) {
                try {
                    fetchRetailersFromBackend()
                    failureCount = 0 // Reset on success
                    delay(30_000) // Refresh every 30 seconds
                } catch (e: Exception) {
                    failureCount++
                    val backoffDelay = minOf(60_000L * failureCount, 300_000L) // Max 5 min
                    android.util.Log.e("RetailerRepository", "Auto-refresh failed (attempt $failureCount), retrying in ${backoffDelay/1000}s", e)
                    delay(backoffDelay)
                }
            }
        }
    }
    
    private suspend fun fetchRetailersFromBackend() {
        try {
            val token = sharedPreferences.getString("auth_token", null)
            if (token != null) {
                _isLoading.value = true
                val response = apiService.getUsers("Bearer $token")
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val usersResponse = response.body()?.data
                    if (usersResponse != null) {
                        // Convert UserDto to Retailer, filtering only RETAILER role
                        val retailers = usersResponse.users
                            .filter { it.role.uppercase() == "RETAILER" }
                            .map { userDto ->
                                val retailer = Retailer(
                                    id = userDto.retailerId ?: userDto.id,
                                    userId = userDto.id,  // Store user ID for API operations
                                    name = userDto.name,
                                    shopName = userDto.shopName ?: "Unknown Shop",
                                    phone = userDto.phone ?: "",
                                    email = userDto.email,
                                    address = "", // Address not in backend model
                                    totalOutstanding = 0.0, // Will be fetched from orders/payments
                                    creditLimit = when (userDto.tier?.uppercase()) {
                                        "GOLD" -> 50000.0
                                        "SILVER" -> 30000.0
                                        "BASIC" -> 10000.0
                                        else -> 10000.0
                                    },
                                    pricingTier = when (userDto.tier?.uppercase()) {
                                        "GOLD" -> PricingTier.GOLD
                                        "SILVER" -> PricingTier.SILVER
                                        "BASIC" -> PricingTier.BASIC
                                        else -> PricingTier.BASIC
                                    },
                                    isActive = userDto.isActive ?: true,
                                    createdAt = parseIsoTimestamp(userDto.createdAt),
                                    updatedAt = parseIsoTimestamp(userDto.updatedAt)
                                )
                                retailer
                            }
                        
                        _retailers.value = retailers
                        _error.value = null
                    }
                } else {
                    val errorMsg = response.body()?.message ?: "Failed to fetch retailers"
                    _error.value = errorMsg
                }
            } else {
                // No auth token available
            }
        } catch (e: Exception) {
            android.util.Log.e("RetailerRepository", "Failed to fetch retailers", e)
            _error.value = e.message
        } finally {
            _isLoading.value = false
        }
    }
    
    suspend fun refreshRetailers() {
        fetchRetailersFromBackend()
    }

    suspend fun getAllRetailers(): Result<List<Retailer>> {
        return try {
            Result.success(_retailers.value)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRetailerById(id: String): Result<Retailer> {
        return try {
            val retailer = _retailers.value.find { it.id == id }
            if (retailer != null) {
                Result.success(retailer)
            } else {
                Result.failure(Exception("Retailer not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addRetailer(retailer: Retailer): Result<Retailer> {
        return try {
            val newRetailer = retailer.copy(
                id = "ret_${System.currentTimeMillis()}",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            _retailers.value = _retailers.value + newRetailer
            // Refresh from backend after adding
            refreshRetailers()
            Result.success(newRetailer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRetailer(retailer: Retailer): Result<Retailer> {
        return try {
            val updatedRetailer = retailer.copy(updatedAt = System.currentTimeMillis())
            _retailers.value = _retailers.value.map {
                if (it.id == retailer.id) updatedRetailer else it
            }
            // Refresh from backend after updating
            refreshRetailers()
            Result.success(updatedRetailer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRetailer(id: String): Result<Unit> {
        return try {
            _retailers.value = _retailers.value.filter { it.id != id }
            // Refresh from backend after deleting
            refreshRetailers()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOutstanding(retailerId: String, amount: Double): Result<Unit> {
        return try {
            _retailers.value = _retailers.value.map { retailer ->
                if (retailer.id == retailerId) {
                    retailer.copy(
                        totalOutstanding = retailer.totalOutstanding + amount,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    retailer
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveRetailers(): Result<List<Retailer>> {
        return try {
            Result.success(_retailers.value.filter { it.isActive })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRetailerAccount(retailer: Retailer, email: String, password: String): Result<Retailer> {
        return try {
            // Create the retailer with credentials
            val newRetailer = retailer.copy(
                id = "ret_${System.currentTimeMillis()}",
                email = email,
                password = password, // In production, this should be hashed
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Add to retailers list
            _retailers.value = _retailers.value + newRetailer
            
            // In a real Firebase implementation, you would also create the Firebase Auth user here:
            // firebaseAuth.createUserWithEmailAndPassword(email, password)
            //     .addOnSuccessListener { authResult ->
            //         // Store user data in Firestore
            //     }
            
            Result.success(newRetailer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Cancel background coroutines when repository is no longer needed
     * Call this to prevent memory leaks
     */
    fun close() {
        repositoryScope.cancel()
    }
}
