package com.ganeshkulfi.backend.routes

import com.ganeshkulfi.backend.data.dto.*
import com.ganeshkulfi.backend.data.models.UserRole
import com.ganeshkulfi.backend.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * User Management Routes
 * Handles CRUD operations for users (admin only)
 */
fun Route.userRoutes(userService: UserService) {
    
    route("/api/users") {
        
        // All routes require authentication
        authenticate("auth-jwt") {
            
            /**
             * GET /api/users
             * Get all users (admin only)
             */
            get {
                try {
                    // Check if user is admin
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.getClaim("role", String::class)
                    
                    if (role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Admin access required")
                        )
                        return@get
                    }
                    
                    // Get all users
                    val result = userService.getAllUsers()
                    
                    result.fold(
                        onSuccess = { users ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Users retrieved successfully",
                                    data = UsersListResponse(
                                        users = users,
                                        total = users.size
                                    )
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(error.message ?: "Failed to retrieve users")
                            )
                        }
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Server error: ${e.message}")
                    )
                }
            }
            
            /**
             * POST /api/users
             * Create a new user (admin only)
             */
            post {
                try {
                    // Check if user is admin
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.getClaim("role", String::class)
                    
                    if (role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Admin access required")
                        )
                        return@post
                    }
                    
                    val request = call.receive<CreateUserRequest>()
                    
                    // Validate request
                    if (request.email.isBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Email is required")
                        )
                        return@post
                    }
                    
                    if (request.password.isBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Password is required")
                        )
                        return@post
                    }
                    
                    if (request.name.isBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Name is required")
                        )
                        return@post
                    }
                    
                    // Create user
                    val result = userService.createUser(request)
                    
                    result.fold(
                        onSuccess = { user ->
                            call.respond(
                                HttpStatusCode.Created,
                                ApiResponse(
                                    success = true,
                                    message = "User created successfully",
                                    data = user
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error.message ?: "Failed to create user")
                            )
                        }
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Server error: ${e.message}")
                    )
                }
            }
            
            /**
             * GET /api/users/:id
             * Get user by ID
             */
            get("/{id}") {
                try {
                    val userId = call.parameters["id"]
                    
                    if (userId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("User ID is required")
                        )
                        return@get
                    }
                    
                    // Check permissions (admin can view any user, users can view themselves)
                    val principal = call.principal<JWTPrincipal>()
                    val currentUserId = principal?.getClaim("userId", String::class)
                    val role = principal?.getClaim("role", String::class)
                    
                    if (role != UserRole.ADMIN.name && currentUserId != userId) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Access denied")
                        )
                        return@get
                    }
                    
                    // Get user
                    val result = userService.getCurrentUser(userId)
                    
                    result.fold(
                        onSuccess = { user ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "User retrieved successfully",
                                    data = user
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.NotFound,
                                ErrorResponse(error.message ?: "User not found")
                            )
                        }
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Server error: ${e.message}")
                    )
                }
            }
            
            /**
             * PUT /api/users/:id
             * Update user
             */
            put("/{id}") {
                try {
                    val userId = call.parameters["id"]
                    
                    if (userId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("User ID is required")
                        )
                        return@put
                    }
                    
                    // Check permissions (admin can update any user, users can update themselves)
                    val principal = call.principal<JWTPrincipal>()
                    val currentUserId = principal?.getClaim("userId", String::class)
                    val role = principal?.getClaim("role", String::class)
                    
                    if (role != UserRole.ADMIN.name && currentUserId != userId) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Access denied")
                        )
                        return@put
                    }
                    
                    val request = call.receive<UpdateUserRequest>()
                    
                    // Non-admin users cannot change their role
                    if (role != UserRole.ADMIN.name && request.role != null) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Cannot change your own role")
                        )
                        return@put
                    }
                    
                    // Update user
                    val result = userService.updateUser(userId, request)
                    
                    result.fold(
                        onSuccess = { user ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "User updated successfully",
                                    data = user
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error.message ?: "Failed to update user")
                            )
                        }
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Server error: ${e.message}")
                    )
                }
            }
            
            /**
             * DELETE /api/users/:id
             * Delete user (admin only)
             */
            delete("/{id}") {
                try {
                    // Check if user is admin
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal?.getClaim("role", String::class)
                    
                    if (role != UserRole.ADMIN.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("Admin access required")
                        )
                        return@delete
                    }
                    
                    val userId = call.parameters["id"]
                    
                    if (userId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("User ID is required")
                        )
                        return@delete
                    }
                    
                    // Delete user
                    val result = userService.deleteUser(userId)
                    
                    result.fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "User deleted successfully",
                                    data = null
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.NotFound,
                                ErrorResponse(error.message ?: "User not found")
                            )
                        }
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Server error: ${e.message}")
                    )
                }
            }
        }
    }
}
