package com.ganeshkulfi.backend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

/**
 * Configure CORS to allow Android app to communicate with backend
 */
fun Application.configureCORS() {
    install(CORS) {
        // Allow requests from Android app
        anyHost() // For development - restrict in production
        
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
