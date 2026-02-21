package com.ganeshkulfi.backend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours
import kotlin.time.TimeSource

/**
 * Simple in-memory rate limiter for authentication endpoints.
 *
 * Limits:
 *  - 10 requests per minute per IP  (burst protection)
 *  - 50 requests per hour per IP    (sustained abuse protection)
 *
 * Uses a sliding-window approach backed by a ConcurrentHashMap.
 * For multi-instance deployments, swap this for a Redis-backed solution.
 */
object RateLimiter {
    private data class RequestLog(
        val timestamps: MutableList<TimeSource.Monotonic.ValueTimeMark> = mutableListOf()
    )

    private val logs = ConcurrentHashMap<String, RequestLog>()
    private val timeSource = TimeSource.Monotonic

    // Configurable limits
    private const val MAX_PER_MINUTE = 10
    private const val MAX_PER_HOUR = 50
    private val ONE_MINUTE = 1.minutes
    private val ONE_HOUR = 1.hours

    /**
     * Returns `true` if the request should be **allowed**, `false` if rate-limited.
     */
    fun allowRequest(clientIp: String): Boolean {
        val now = timeSource.markNow()
        val log = logs.getOrPut(clientIp) { RequestLog() }

        synchronized(log) {
            // Evict entries older than 1 hour
            log.timestamps.removeAll { (now - it) > ONE_HOUR }

            val countLastMinute = log.timestamps.count { (now - it) <= ONE_MINUTE }
            val countLastHour = log.timestamps.size

            if (countLastMinute >= MAX_PER_MINUTE || countLastHour >= MAX_PER_HOUR) {
                return false
            }

            log.timestamps.add(now)
            return true
        }
    }

    /**
     * Periodic cleanup of stale entries (call from a background coroutine if desired).
     */
    fun cleanup() {
        val now = timeSource.markNow()
        val staleKeys = logs.entries
            .filter { (_, v) -> v.timestamps.all { (now - it) > ONE_HOUR } }
            .map { it.key }
        staleKeys.forEach { logs.remove(it) }
    }
}

/**
 * Ktor plugin that intercepts requests and applies rate limiting.
 * Install it on the auth route group, NOT globally.
 */
val RateLimitPlugin = createRouteScopedPlugin("AuthRateLimit") {
    onCall { call ->
        val clientIp = call.request.origin.remoteAddress
        if (!RateLimiter.allowRequest(clientIp)) {
            call.respond(
                HttpStatusCode.TooManyRequests,
                mapOf(
                    "success" to false,
                    "message" to "Too many requests. Please try again later."
                )
            )
            // Finish the call so downstream handlers are not invoked
            return@onCall
        }
    }
}
