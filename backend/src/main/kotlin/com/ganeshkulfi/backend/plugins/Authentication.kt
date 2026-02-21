package com.ganeshkulfi.backend.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ganeshkulfi.backend.data.dto.ErrorResponse
import com.ganeshkulfi.backend.services.JWTService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

/**
 * JWT Authentication Plugin
 * Configures JWT authentication for protected routes
 */
fun Application.configureAuthentication(jwtService: JWTService) {
    
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtService.getRealm()
            
            verifier(jwtService.verifier)
            
            validate { credential ->
                // Validate JWT token
                if (credential.payload.getClaim("userId").asString() != null &&
                    credential.payload.getClaim("email").asString() != null &&
                    credential.payload.getClaim("role").asString() != null
                ) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Token is not valid or has expired")
                )
            }
        }
    }
}
