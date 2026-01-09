package com.ganeshkulfi.backend.routes

import com.ganeshkulfi.backend.data.dto.*
import com.ganeshkulfi.backend.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Authentication Routes
 * Handles user registration, login, and authentication
 */
fun Route.authRoutes(userService: UserService) {
    
    route("/api/auth") {
        
        /**
         * POST /api/auth/register
         * Register a new user
         */
        post("/register") {
            try {
                val request = call.receive<RegisterRequest>()
                
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
                
                // Register user
                val result = userService.register(request)
                
                result.fold(
                    onSuccess = { authResponse ->
                        call.respond(
                            HttpStatusCode.Created,
                            ApiResponse(
                                success = true,
                                message = "User registered successfully",
                                data = authResponse
                            )
                        )
                    },
                    onFailure = { error ->
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(error.message ?: "Registration failed")
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
         * POST /api/auth/login
         * Login user
         */
        post("/login") {
            try {
                val request = call.receive<LoginRequest>()
                
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
                
                // Login user
                val result = userService.login(request)
                
                result.fold(
                    onSuccess = { authResponse ->
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                message = "Login successful",
                                data = authResponse
                            )
                        )
                    },
                    onFailure = { error ->
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ErrorResponse(error.message ?: "Login failed")
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
         * GET /api/auth/me
         * Get current user (requires authentication)
         */
        authenticate("auth-jwt") {
            get("/me") {
                try {
                    // Get user ID from JWT token
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class)
                    
                    if (userId == null) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ErrorResponse("Invalid token")
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
        }
    }
}
