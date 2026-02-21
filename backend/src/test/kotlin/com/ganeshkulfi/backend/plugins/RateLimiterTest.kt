package com.ganeshkulfi.backend.plugins

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for RateLimiter.
 * Tests rate limiting logic for auth endpoints.
 */
class RateLimiterTest {

    @Test
    fun `allows requests under the limit`() {
        val testIp = "test-${System.nanoTime()}"
        // First 10 requests should be allowed
        repeat(10) { i ->
            assertTrue(
                RateLimiter.allowRequest(testIp),
                "Request $i should be allowed"
            )
        }
    }

    @Test
    fun `blocks requests over per-minute limit`() {
        val testIp = "test-burst-${System.nanoTime()}"
        // Exhaust the per-minute limit (10)
        repeat(10) {
            RateLimiter.allowRequest(testIp)
        }
        // 11th request should be blocked
        assertFalse(
            RateLimiter.allowRequest(testIp),
            "11th request should be blocked"
        )
    }

    @Test
    fun `different IPs have independent limits`() {
        val ip1 = "test-ip1-${System.nanoTime()}"
        val ip2 = "test-ip2-${System.nanoTime()}"
        
        // Exhaust ip1's limit
        repeat(10) { RateLimiter.allowRequest(ip1) }
        assertFalse(RateLimiter.allowRequest(ip1))
        
        // ip2 should still be allowed
        assertTrue(RateLimiter.allowRequest(ip2))
    }
}
