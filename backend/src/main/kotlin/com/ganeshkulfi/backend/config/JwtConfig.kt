package com.ganeshkulfi.backend.config

/**
 * JWT Configuration
 * ALL values must be provided via environment variables
 * NEVER hardcode secrets in source code
 */
object JwtConfig {
    // JWT Settings - Load from environment variables
    val SECRET: String = System.getenv("JWT_SECRET") 
        ?: throw IllegalStateException("JWT_SECRET environment variable must be set")
    val ISSUER: String = System.getenv("JWT_ISSUER") ?: "ganeshkulfi"
    val AUDIENCE: String = System.getenv("JWT_AUDIENCE") ?: "ganeshkulfi-app"
    const val REALM = "Ganesh Kulfi API"
    const val EXPIRATION_TIME = 604800000L  // 7 days in milliseconds
    
    // Token Claims
    const val CLAIM_USER_ID = "userId"
    const val CLAIM_EMAIL = "email"
    const val CLAIM_ROLE = "role"
}
