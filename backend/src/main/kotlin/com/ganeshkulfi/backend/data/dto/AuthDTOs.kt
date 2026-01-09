package com.ganeshkulfi.backend.data.dto

import com.ganeshkulfi.backend.data.models.User
import com.ganeshkulfi.backend.data.models.UserRole
import kotlinx.serialization.Serializable

/**
 * Authentication & User Management DTOs
 */

// ============= REQUEST DTOs =============

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val phone: String? = null,
    val role: String = "CUSTOMER"
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class CreateUserRequest(
    val email: String,
    val password: String,
    val name: String,
    val phone: String? = null,
    val role: String,
    val retailerId: String? = null,
    val shopName: String? = null,
    val tier: String? = null
)

@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val retailerId: String? = null,
    val shopName: String? = null,
    val tier: String? = null
)

// ============= RESPONSE DTOs =============

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserResponse
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val name: String,
    val phone: String? = null,
    val role: String,
    val retailerId: String? = null,
    val shopName: String? = null,
    val tier: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun fromUser(user: User): UserResponse {
            return UserResponse(
                id = user.id,
                email = user.email,
                name = user.name,
                phone = user.phone,
                role = user.role.name,
                retailerId = user.retailerId,
                shopName = user.shopName,
                tier = user.tier.name,  // Day 9: Convert enum to String
                createdAt = user.createdAt.toString(),
                updatedAt = user.updatedAt.toString()
            )
        }
    }
}

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

@Serializable
data class ErrorResponse(
    val message: String,
    val success: Boolean = false,
    val error: String? = null
)

@Serializable
data class UsersListResponse(
    val users: List<UserResponse>,
    val total: Int
)
