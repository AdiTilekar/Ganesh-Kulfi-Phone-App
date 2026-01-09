package com.ganeshkulfi.backend.services

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.ganeshkulfi.backend.data.models.UserRole
import io.ktor.server.config.*
import java.util.*

/**
 * JWT Service
 * Handles JWT token generation and validation
 */
class JWTService(config: ApplicationConfig) {
    
    private val secret = config.property("jwt.secret").getString()
    private val issuer = config.property("jwt.issuer").getString()
    private val audience = config.property("jwt.audience").getString()
    private val realm = config.property("jwt.realm").getString()
    private val expirationTime = config.property("jwt.expirationTime").getString().toLong()
    
    private val algorithm = Algorithm.HMAC256(secret)
    
    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withAudience(audience)
        .withIssuer(issuer)
        .build()
    
    /**
     * Generate JWT token for user
     */
    fun generateToken(userId: String, email: String, role: UserRole): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("role", role.name)
            .withExpiresAt(Date(System.currentTimeMillis() + expirationTime))
            .sign(algorithm)
    }
    
    /**
     * Extract user ID from token
     */
    fun getUserId(token: String): String? {
        return try {
            val decodedJWT = verifier.verify(token)
            decodedJWT.getClaim("userId").asString()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract email from token
     */
    fun getEmail(token: String): String? {
        return try {
            val decodedJWT = verifier.verify(token)
            decodedJWT.getClaim("email").asString()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract role from token
     */
    fun getRole(token: String): UserRole? {
        return try {
            val decodedJWT = verifier.verify(token)
            val roleName = decodedJWT.getClaim("role").asString()
            UserRole.valueOf(roleName)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Validate token
     */
    fun isValid(token: String): Boolean {
        return try {
            verifier.verify(token)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getRealm() = realm
    fun getIssuer() = issuer
    fun getAudience() = audience
}
