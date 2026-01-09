package com.ganeshkulfi.app.data.repository

import android.content.SharedPreferences
import com.ganeshkulfi.app.data.model.User
import com.ganeshkulfi.app.data.model.UserRole
import com.ganeshkulfi.app.data.model.PricingTier
import com.ganeshkulfi.app.data.model.UserRoleMapper
import com.ganeshkulfi.app.data.remote.ApiService
import com.ganeshkulfi.app.data.remote.LoginRequest
import com.ganeshkulfi.app.data.remote.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val apiService: ApiService
) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUserFlow: Flow<User?> = _currentUser.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        try {
            // Check if user explicitly logged out - if so, don't auto-login
            val hasLoggedOut = sharedPreferences.getBoolean(KEY_HAS_LOGGED_OUT, false)
            if (hasLoggedOut) {
                _currentUser.value = null
                return
            }

            var userId = sharedPreferences.getString(KEY_USER_ID, null)

            if (userId == null) {
                // No persisted user id — but maybe credentials were stored for re-login.
                val storedEmail = sharedPreferences.getString(KEY_STORED_EMAIL, null)
                if (storedEmail != null) {
                    // Reconstruct a minimal user from stored prefs and persist an id so future loads succeed.
                    val email = storedEmail
                    val name = sharedPreferences.getString(KEY_NAME, "") ?: ""
                    val phone = sharedPreferences.getString(KEY_PHONE, "") ?: ""
                    val roleString = sharedPreferences.getString(KEY_ROLE, UserRole.CUSTOMER.name) ?: UserRole.CUSTOMER.name
                    val role = try {
                        UserRole.valueOf(roleString)
                    } catch (e: IllegalArgumentException) {
                        UserRole.CUSTOMER
                    }

                    val retailerId = sharedPreferences.getString(KEY_RETAILER_ID, null)
                    val shopName = sharedPreferences.getString(KEY_SHOP_NAME, null)
                    val pricingTierString = sharedPreferences.getString(KEY_PRICING_TIER, null)
                    val pricingTier = pricingTierString?.let {
                        try {
                            PricingTier.valueOf(it)
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    }

                    // Generate and persist a user id so subsequent loadCurrentUser calls succeed.
                    userId = "user_${System.currentTimeMillis()}"
                    with(sharedPreferences.edit()) {
                        putString(KEY_USER_ID, userId)
                        putString(KEY_EMAIL, email)
                        putString(KEY_NAME, name)
                        putString(KEY_PHONE, phone)
                        putString(KEY_ROLE, role.name)
                        apply()
                    }

                    _currentUser.value = User(
                        id = userId,
                        email = email,
                        name = name,
                        phone = phone,
                        role = role,
                        retailerId = retailerId,
                        shopName = shopName,
                        pricingTier = pricingTier
                    )
                }
            } else {
                val email = sharedPreferences.getString(KEY_EMAIL, "") ?: ""
                val name = sharedPreferences.getString(KEY_NAME, "") ?: ""
                val phone = sharedPreferences.getString(KEY_PHONE, "") ?: ""
                val roleString = sharedPreferences.getString(KEY_ROLE, UserRole.CUSTOMER.name) ?: UserRole.CUSTOMER.name
                val role = try {
                    UserRole.valueOf(roleString)
                } catch (e: IllegalArgumentException) {
                    UserRole.CUSTOMER
                }

                // Load retailer-specific data if role is RETAILER
                val retailerId = sharedPreferences.getString(KEY_RETAILER_ID, null)
                val shopName = sharedPreferences.getString(KEY_SHOP_NAME, null)
                val pricingTierString = sharedPreferences.getString(KEY_PRICING_TIER, null)
                val pricingTier = pricingTierString?.let {
                    try {
                        PricingTier.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }

                _currentUser.value = User(
                    id = userId,
                    email = email,
                    name = name,
                    phone = phone,
                    role = role,
                    retailerId = retailerId,
                    shopName = shopName,
                    pricingTier = pricingTier
                )
            }
        } catch (e: Exception) {
            // Clear corrupted data but preserve logout flag
            val hasLoggedOut = sharedPreferences.getBoolean(KEY_HAS_LOGGED_OUT, false)
            with(sharedPreferences.edit()) {
                clear()
                if (hasLoggedOut) {
                    putBoolean(KEY_HAS_LOGGED_OUT, true)  // Restore logout flag
                }
                commit()  // Use commit for immediate write
            }
            _currentUser.value = null
        }
    }

    fun isUserLoggedIn(): Boolean = _currentUser.value != null

    /**
     * Get the current authentication token
     */
    fun getAuthToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
    }

    suspend fun signUp(email: String, password: String, name: String, phone: String): Result<User> {
        return try {
            // Simple validation
            if (email.isBlank() || password.length < 6) {
                throw Exception("Invalid email or password too short")
            }

            // Check if user already exists
            val existingEmail = sharedPreferences.getString(KEY_STORED_EMAIL, null)
            if (existingEmail == email) {
                throw Exception("User already exists")
            }

            // Create user
            val userId = "user_${System.currentTimeMillis()}"
            val user = User(
                id = userId,
                email = email,
                name = name,
                phone = phone,
                role = UserRole.CUSTOMER
            )

            // Save user data
            with(sharedPreferences.edit()) {
                putString(KEY_USER_ID, userId)
                putString(KEY_EMAIL, email)
                putString(KEY_NAME, name)
                putString(KEY_PHONE, phone)
                putString(KEY_ROLE, user.role.name)
                putString(KEY_STORED_EMAIL, email)
                putString(KEY_PASSWORD, password) // In production, use proper encryption
                apply()
            }

            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign up a retailer account and persist retailer-specific session data.
     * This will store credentials and retailer metadata so the created account
     * can sign in as a RETAILER.
     * 
     * NOTE: This changes the current user session to the newly created retailer.
     * Use registerRetailerCredentials() if you want to create credentials without logging in.
     */
    suspend fun signUpRetailer(
        email: String,
        password: String,
        name: String,
        phone: String,
        retailerId: String,
        shopName: String,
        pricingTier: PricingTier
    ): Result<User> {
        return try {
            if (email.isBlank() || password.length < 6) {
                throw Exception("Invalid email or password too short")
            }

            val existingEmail = sharedPreferences.getString(KEY_STORED_EMAIL, null)
            if (existingEmail == email) {
                throw Exception("User already exists")
            }

            // Create user with RETAILER role
            val userId = "user_${System.currentTimeMillis()}"
            val user = User(
                id = userId,
                email = email,
                name = name,
                phone = phone,
                role = UserRole.RETAILER,
                retailerId = retailerId,
                shopName = shopName,
                pricingTier = pricingTier
            )

            // Save session and credentials
            with(sharedPreferences.edit()) {
                putString(KEY_USER_ID, userId)
                putString(KEY_EMAIL, email)
                putString(KEY_NAME, name)
                putString(KEY_PHONE, phone)
                putString(KEY_ROLE, user.role.name)

                putString(KEY_RETAILER_ID, retailerId)
                putString(KEY_SHOP_NAME, shopName)
                putString(KEY_PRICING_TIER, pricingTier.name)

                putString(KEY_STORED_EMAIL, email)
                putString(KEY_PASSWORD, password) // In production, encrypt
                apply()
            }

            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Register retailer credentials WITHOUT changing the current user session.
     * Use this when admin creates a retailer account - the admin stays logged in.
     */
    suspend fun registerRetailerCredentials(
        email: String,
        password: String,
        name: String,
        phone: String,
        retailerId: String,
        shopName: String,
        pricingTier: PricingTier
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (email.isBlank() || password.length < 6) {
                    return@withContext Result.failure(Exception("Invalid email or password too short"))
                }

                println("🔐 Attempting to create retailer account: $email")
                
                // Get admin auth token
                val adminToken = getAuthToken()
                if (adminToken == null) {
                    println("❌ ERROR: No admin auth token available")
                    return@withContext Result.failure(Exception("Admin authentication required"))
                }
                
                // Map PricingTier to backend tier format (1:1 mapping now)
                val tier = when (pricingTier) {
                    PricingTier.GOLD -> "GOLD"
                    PricingTier.SILVER -> "SILVER"
                    PricingTier.BASIC -> "BASIC"
                }
                
                // Call backend API to create retailer account with all fields
                val createUserRequest = com.ganeshkulfi.app.data.remote.CreateUserRequest(
                    email = email,
                    password = password,
                    name = name,
                    phone = phone,
                    role = "RETAILER",
                    retailerId = retailerId,
                    shopName = shopName,
                    tier = tier
                )
                
                println("📤 Sending create user request:")
                println("   Email: $email")
                println("   Role: RETAILER")
                println("   RetailerId: $retailerId")
                println("   ShopName: $shopName")
                println("   Tier: $tier")
                
                val response = apiService.createUser("Bearer $adminToken", createUserRequest)
                
                println("📥 Backend response code: ${response.code()}, successful: ${response.isSuccessful}")
                
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    println("❌ ERROR: User creation failed - Status: ${response.code()}, Error: $errorBody")
                    return@withContext Result.failure(Exception("Failed to create retailer: ${response.code()} - $errorBody"))
                }
                
                if (response.body()?.success != true) {
                    val message = response.body()?.message ?: "Unknown error"
                    println("❌ ERROR: Backend returned success=false - Message: $message")
                    return@withContext Result.failure(Exception("Backend error: $message"))
                }

                println("✅ SUCCESS: Retailer account created successfully on backend")
                println("   Email: $email")
                println("   Password: ${password.take(3)}***")
                println("   Retailer can now login with these credentials!")
                
                // Store credentials locally for reference
                val credentialKey = "retailer_cred_$email"
                
                with(sharedPreferences.edit()) {
                    // Store retailer credentials for future sign-in
                    putString("${credentialKey}_email", email)
                    putString("${credentialKey}_password", password) // In production, encrypt
                    putString("${credentialKey}_name", name)
                    putString("${credentialKey}_phone", phone)
                    putString("${credentialKey}_retailer_id", retailerId)
                    putString("${credentialKey}_shop_name", shopName)
                    putString("${credentialKey}_pricing_tier", pricingTier.name)
                    apply()
                }

                println("SUCCESS: Credentials stored locally")
                Result.success(Unit)
            } catch (e: Exception) {
                println("EXCEPTION: Registration failed - ${e.message}")
                android.util.Log.e("AuthRepository", "Registration exception", e)
                Result.failure(e)
            }
        }
    }

    suspend fun signIn(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Call backend API
            val response = apiService.login(LoginRequest(email, password))
            
            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()!!.data!!
                val userDto = authData.user
                
                // Convert role string to UserRole enum
                val userRole = try {
                    UserRole.valueOf(userDto.role.uppercase())
                } catch (e: Exception) {
                    UserRole.CUSTOMER
                }
                
                // Convert tier string to PricingTier enum (if exists)
                val pricingTier = userDto.tier?.let {
                    try {
                        PricingTier.valueOf(it.uppercase())
                    } catch (e: Exception) {
                        null
                    }
                }
                
                // Create User object
                val user = User(
                    id = userDto.id,
                    email = userDto.email,
                    name = userDto.name,
                    phone = userDto.phone ?: "",
                    role = userRole,
                    retailerId = userDto.retailerId,
                    shopName = userDto.shopName,
                    pricingTier = pricingTier
                )
                
                // Save user session
                println("════════════════════════════════════════════")
                println("💾 SAVING USER SESSION in AuthRepository.signIn()")
                println("   User: ${user.email}")
                println("   Role: ${user.role}")
                println("   Token length: ${authData.token.length}")
                println("   Token preview: ${authData.token.take(30)}...")
                
                // Log retailer-specific data BEFORE saving
                if (user.role == UserRole.RETAILER) {
                    println("🏪 RETAILER LOGIN DETECTED:")
                    println("   retailerId from backend: ${userDto.retailerId}")
                    println("   shopName from backend: ${userDto.shopName}")
                    println("   tier from backend: ${userDto.tier}")
                    println("   retailerId in User object: ${user.retailerId}")
                    println("   shopName in User object: ${user.shopName}")
                    println("   pricingTier in User object: ${user.pricingTier}")
                }
                
                with(sharedPreferences.edit()) {
                    putString(KEY_USER_ID, user.id)
                    putString(KEY_EMAIL, user.email)
                    putString(KEY_NAME, user.name)
                    putString(KEY_PHONE, user.phone)
                    putString(KEY_ROLE, user.role.name)
                    
                    // Save retailer-specific data with null checks
                    if (user.role == UserRole.RETAILER) {
                        user.retailerId?.let { 
                            putString(KEY_RETAILER_ID, it)
                            println("   ✅ Saved KEY_RETAILER_ID: $it")
                        } ?: println("   ⚠️ WARNING: retailerId is NULL!")
                        
                        user.shopName?.let { 
                            putString(KEY_SHOP_NAME, it)
                            println("   ✅ Saved KEY_SHOP_NAME: $it")
                        } ?: println("   ⚠️ WARNING: shopName is NULL!")
                        
                        user.pricingTier?.let { 
                            putString(KEY_PRICING_TIER, it.name)
                            println("   ✅ Saved KEY_PRICING_TIER: ${it.name}")
                        } ?: println("   ⚠️ WARNING: pricingTier is NULL!")
                    } else {
                        // Clear retailer data for non-retailer users
                        remove(KEY_RETAILER_ID)
                        remove(KEY_SHOP_NAME)
                        remove(KEY_PRICING_TIER)
                    }
                    
                    putString(KEY_AUTH_TOKEN, authData.token)  // Save token using constant
                    commit()  // Use commit() instead of apply() to ensure synchronous write
                }
                
                // Verify token was saved
                val savedToken = sharedPreferences.getString(KEY_AUTH_TOKEN, null)
                println("✅ Token saved successfully: ${savedToken != null}")
                println("   Saved token length: ${savedToken?.length ?: 0}")
                println("   Tokens match: ${savedToken == authData.token}")
                
                // Verify retailer data was saved
                if (user.role == UserRole.RETAILER) {
                    val savedRetailerId = sharedPreferences.getString(KEY_RETAILER_ID, null)
                    val savedShopName = sharedPreferences.getString(KEY_SHOP_NAME, null)
                    val savedTier = sharedPreferences.getString(KEY_PRICING_TIER, null)
                    println("🔍 VERIFICATION - Data actually saved:")
                    println("   Saved retailerId: $savedRetailerId")
                    println("   Saved shopName: $savedShopName")
                    println("   Saved tier: $savedTier")
                }
                println("════════════════════════════════════════════")
                
                // Clear logout flag on successful login
                sharedPreferences.edit().putBoolean(KEY_HAS_LOGGED_OUT, false).commit()
                
                _currentUser.value = user
                Result.success(user)
            } else {
                val errorMessage = response.body()?.message ?: "Login failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Sign-in failed", e)
            val errorMsg = when (e) {
                is java.net.UnknownHostException -> "Cannot connect to server. Check internet connection."
                is java.net.SocketTimeoutException -> "Connection timeout. Server may be slow or unavailable."
                is javax.net.ssl.SSLException -> "SSL/Certificate error: ${e.message}"
                is java.io.IOException -> "Network error: ${e.message}"
                else -> "Network error: ${e.javaClass.simpleName} - ${e.message}"
            }
            Result.failure(Exception(errorMsg))
        }
    }

    suspend fun signOut() {
        // Sign out from Google (Uncomment when Firebase is ready)
        // googleSignInHelper.signOut()
        
        // CRITICAL: Set logout flag FIRST before clearing, to ensure it persists
        sharedPreferences.edit().putBoolean(KEY_HAS_LOGGED_OUT, true).commit()
        
        // Now clear the user in memory
        _currentUser.value = null
        
        // Clear all other session data (logout flag already set above)
        with(sharedPreferences.edit()) {
            remove(KEY_USER_ID)
            remove(KEY_EMAIL)
            remove(KEY_NAME)
            remove(KEY_PHONE)
            remove(KEY_ROLE)
            remove(KEY_IS_GUEST)
            remove(KEY_RETAILER_ID)
            remove(KEY_SHOP_NAME)
            remove(KEY_PRICING_TIER)
            remove(KEY_AUTH_TOKEN)
            remove(KEY_STORED_EMAIL)
            remove(KEY_PASSWORD)
            // KEY_HAS_LOGGED_OUT is NOT removed - it stays set to true
            commit()
        }
    }
    
    /* ============================================================
     * OAuth Methods - Uncomment when Firebase is connected
     * ============================================================
     
    /**
     * OAuth Sign-In with Google
     * Automatically detects role based on email
     */
    suspend fun signInWithGoogle(firebaseUser: FirebaseUser): Result<User> {
        return try {
            val email = firebaseUser.email ?: throw Exception("Email not found")
            val name = firebaseUser.displayName ?: "User"
            val photoUrl = firebaseUser.photoUrl?.toString()
            
            // Detect role based on email
            val role = UserRoleMapper.getUserRole(email)
            
            // Create user based on role
            val user = when (role) {
                UserRole.ADMIN -> {
                    User(
                        id = firebaseUser.uid,
                        email = email,
                        name = name,
                        phone = "",  // Can be updated later
                        role = UserRole.ADMIN
                    )
                }
                UserRole.RETAILER -> {
                    val retailerInfo = UserRoleMapper.getRetailerInfo(email)
                        ?: throw Exception("Retailer information not found for: $email")
                    
                    User(
                        id = firebaseUser.uid,
                        email = email,
                        name = retailerInfo.name,
                        phone = retailerInfo.phone,
                        role = UserRole.RETAILER,
                        retailerId = retailerInfo.id,
                        shopName = retailerInfo.shopName,
                        pricingTier = retailerInfo.pricingTier
                    )
                }
                UserRole.CUSTOMER -> {
                    User(
                        id = firebaseUser.uid,
                        email = email,
                        name = name,
                        phone = "",
                        role = UserRole.CUSTOMER
                    )
                }
            }
            
            // Save user session
            with(sharedPreferences.edit()) {
                putString(KEY_USER_ID, user.id)
                putString(KEY_EMAIL, user.email)
                putString(KEY_NAME, user.name)
                putString(KEY_PHONE, user.phone)
                putString(KEY_ROLE, user.role.name)
                
                // Save retailer-specific data
                if (user.role == UserRole.RETAILER) {
                    putString(KEY_RETAILER_ID, user.retailerId)
                    putString(KEY_SHOP_NAME, user.shopName)
                    putString(KEY_PRICING_TIER, user.pricingTier?.name)
                }
                
                apply()
            }
            
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Initiates Google Sign-In flow
     * Returns IntentSender to launch Google Sign-In UI
     */
    suspend fun initiateGoogleSignIn(): android.content.IntentSender? {
        return googleSignInHelper.signIn()
    }
    
    /**
     * Handles Google Sign-In result
     */
    suspend fun handleGoogleSignInResult(intent: android.content.Intent): Result<User> {
        return try {
            val firebaseUserResult = googleSignInHelper.handleSignInResult(intent)
            
            if (firebaseUserResult.isSuccess) {
                val firebaseUser = firebaseUserResult.getOrThrow()
                signInWithGoogle(firebaseUser)
            } else {
                Result.failure(firebaseUserResult.exceptionOrNull() ?: Exception("Sign-in failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    ============================================================ */

    suspend fun getUserData(userId: String): Result<User> {
        return try {
            val user = _currentUser.value ?: throw Exception("User not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            val storedEmail = sharedPreferences.getString(KEY_STORED_EMAIL, null)
            if (storedEmail == email) {
                // In a real app, send email or SMS
                Result.success(Unit)
            } else {
                throw Exception("Email not found")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun continueAsGuest(): Result<User> {
        return try {
            val guestUser = User(
                id = "guest_${System.currentTimeMillis()}",
                email = "guest@ganeshkulfi.com",
                name = "Guest User",
                phone = "",
                role = UserRole.CUSTOMER
            )

            // Save guest status (temporary, won't persist on app restart)
            with(sharedPreferences.edit()) {
                putString(KEY_USER_ID, guestUser.id)
                putString(KEY_EMAIL, guestUser.email)
                putString(KEY_NAME, guestUser.name)
                putString(KEY_PHONE, guestUser.phone)
                putString(KEY_ROLE, guestUser.role.name)
                putBoolean(KEY_IS_GUEST, true)
                apply()
            }

            _currentUser.value = guestUser
            Result.success(guestUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isGuestUser(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_GUEST, false)
    }

    companion object {
        // SharedPreferences keys
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_NAME = "name"
        private const val KEY_PHONE = "phone"
        private const val KEY_ROLE = "role"
        private const val KEY_STORED_EMAIL = "stored_email"
        private const val KEY_PASSWORD = "password"
        private const val KEY_IS_GUEST = "is_guest"
        private const val KEY_RETAILER_ID = "retailer_id"
        private const val KEY_SHOP_NAME = "shop_name"
        private const val KEY_PRICING_TIER = "pricing_tier"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_HAS_LOGGED_OUT = "has_logged_out"
    }
}
