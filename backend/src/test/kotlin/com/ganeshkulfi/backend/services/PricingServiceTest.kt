package com.ganeshkulfi.backend.services

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for PricingService.
 * Tests price calculation logic with various tier discounts.
 */
class PricingServiceTest {

    private val pricingService = PricingService()

    @Test
    fun `BASIC tier gets no discount`() {
        val price = pricingService.calculatePrice(
            basePrice = 100.0,
            tier = "BASIC",
            quantity = 10
        )
        // BASIC tier should pay full price
        assertTrue(price >= 100.0 * 10)
    }

    @Test
    fun `GOLD tier gets best discount`() {
        val basicPrice = pricingService.calculatePrice(
            basePrice = 100.0,
            tier = "BASIC",
            quantity = 10
        )
        val goldPrice = pricingService.calculatePrice(
            basePrice = 100.0,
            tier = "GOLD",
            quantity = 10
        )
        // GOLD should be cheaper or equal to BASIC
        assertTrue(goldPrice <= basicPrice, "GOLD ($goldPrice) should be <= BASIC ($basicPrice)")
    }

    @Test
    fun `zero quantity returns zero`() {
        val price = pricingService.calculatePrice(
            basePrice = 100.0,
            tier = "BASIC",
            quantity = 0
        )
        assertEquals(0.0, price)
    }
}
