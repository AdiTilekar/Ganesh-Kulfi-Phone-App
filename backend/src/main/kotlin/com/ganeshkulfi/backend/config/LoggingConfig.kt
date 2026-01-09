package com.ganeshkulfi.backend.config

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import org.slf4j.event.Level

/**
 * Day 14: Logging Configuration
 * Production-ready logging setup
 */
fun Application.configureLogging() {
    install(CallLogging) {
        level = Level.INFO
        
        // Filter out static file requests from logs
        filter { call -> 
            !call.request.path().startsWith("/uploads") &&
            !call.request.path().startsWith("/static")
        }
        
        // Log request details
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val path = call.request.path()
            
            "Status: $status, HTTP method: $httpMethod, Path: $path, User agent: $userAgent"
        }
    }
}
