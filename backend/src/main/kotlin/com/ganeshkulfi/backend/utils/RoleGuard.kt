package com.ganeshkulfi.backend.utils

import com.ganeshkulfi.backend.data.dto.ErrorResponse
import com.ganeshkulfi.backend.data.models.UserRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

/**
 * Role Guard Helper
 * Utility functions to restrict endpoints by role
 */

/**
 * Check if the authenticated user has the required role
 * Returns null if authorized, or ErrorResponse if not authorized
 */
suspend fun ApplicationCall.requireRole(requiredRole: UserRole): ErrorResponse? {
    val principal = principal<JWTPrincipal>()
    
    if (principal == null) {
        return ErrorResponse("Authentication required")
    }
    
    val userRole = principal.payload.getClaim("role").asString()
    
    if (userRole != requiredRole.name) {
        return ErrorResponse("${requiredRole.name} access required. Your role: $userRole")
    }
    
    return null
}

/**
 * Check if user has ADMIN role
 */
suspend fun ApplicationCall.requireAdmin(): ErrorResponse? = requireRole(UserRole.ADMIN)

/**
 * Check if user is ADMIN or RETAILER
 */
suspend fun ApplicationCall.requireAdminOrRetailer(): ErrorResponse? {
    val principal = principal<JWTPrincipal>()
    
    if (principal == null) {
        return ErrorResponse("Authentication required")
    }
    
    val userRole = principal.payload.getClaim("role").asString()
    
    if (userRole != UserRole.ADMIN.name && userRole != UserRole.RETAILER.name) {
        return ErrorResponse("ADMIN or RETAILER access required. Your role: $userRole")
    }
    
    return null
}

/**
 * Get the current user's role
 */
fun ApplicationCall.getUserRole(): UserRole? {
    val principal = principal<JWTPrincipal>() ?: return null
    val roleString = principal.payload.getClaim("role").asString()
    return try {
        UserRole.valueOf(roleString)
    } catch (e: Exception) {
        null
    }
}

/**
 * Get the current user's ID
 */
fun ApplicationCall.getUserId(): String? {
    val principal = principal<JWTPrincipal>() ?: return null
    return principal.payload.getClaim("userId").asString()
}

/**
 * Get the current user's email
 */
fun ApplicationCall.getUserEmail(): String? {
    val principal = principal<JWTPrincipal>() ?: return null
    return principal.payload.getClaim("email").asString()
}

/**
 * Extension function to respond with 403 Forbidden if role check fails
 * Usage:
 * ```
 * get("/admin-only") {
 *     call.requireAdmin()?.let { error ->
 *         call.respond(HttpStatusCode.Forbidden, error)
 *         return@get
 *     }
 *     // Admin-only code here
 * }
 * ```
 */
suspend inline fun ApplicationCall.guardWithRole(
    role: UserRole,
    block: () -> Unit
) {
    requireRole(role)?.let { error ->
        respond(HttpStatusCode.Forbidden, error)
        return
    }
    block()
}
