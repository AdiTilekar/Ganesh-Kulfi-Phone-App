package com.ganeshkulfi.backend.services

import at.favre.lib.crypto.bcrypt.BCrypt

/** * Validation result for input validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val message: String
)

/** * Password Service
 * Handles password hashing and verification using bcrypt
 */
class PasswordService {
    
    private val cost = 12 // bcrypt cost factor (2^12 iterations)
    
    /**
     * Hash a plain text password
     */
    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(cost, password.toCharArray())
    }
    
    /**
     * Verify a plain text password against a hash
     */
    fun verifyPassword(password: String, hash: String): Boolean {
        val result = BCrypt.verifyer().verify(password.toCharArray(), hash)
        return result.verified
    }
    
    /**
     * Validate password strength
     * Requirements:
     * - At least 8 characters
     * - Contains at least one uppercase letter
     * - Contains at least one lowercase letter
     * - Contains at least one number
     */
    fun validatePasswordStrength(password: String): PasswordValidationResult {
        if (password.length < 8) {
            return PasswordValidationResult(
                isValid = false,
                message = "Password must be at least 8 characters long"
            )
        }
        
        if (!password.any { it.isUpperCase() }) {
            return PasswordValidationResult(
                isValid = false,
                message = "Password must contain at least one uppercase letter"
            )
        }
        
        if (!password.any { it.isLowerCase() }) {
            return PasswordValidationResult(
                isValid = false,
                message = "Password must contain at least one lowercase letter"
            )
        }
        
        if (!password.any { it.isDigit() }) {
            return PasswordValidationResult(
                isValid = false,
                message = "Password must contain at least one number"
            )
        }
        
        return PasswordValidationResult(isValid = true, message = "Password is valid")
    }
}

/**
 * Password validation result
 */
data class PasswordValidationResult(
    val isValid: Boolean,
    val message: String
)
