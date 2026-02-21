package com.ganeshkulfi.backend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

/**
 * Configure CORS to allow Android app to communicate with backend
 */
fun Application.configureCORS() {
    install(CORS) {
        // Read allowed origins from env; fall back to anyHost() for local dev only
        val allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS")
        if (allowedOrigins.isNullOrBlank()) {
            anyHost() // Local development only — set CORS_ALLOWED_ORIGINS in production
        } else {
            allowedOrigins.split(",").map { it.trim() }.forEach { origin ->
                allowHost(origin.removePrefix("https://").removePrefix("http://"), schemes = listOf("https", "http"))
            }
        }
        
        // Allowed HTTP methods
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Options)
        
        // Allowed headers
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("X-Requested-With")
        
        // Allow credentials
        allowCredentials = true
        
        // Cache preflight response for 1 day
        maxAgeInSeconds = 86400
    }
}
