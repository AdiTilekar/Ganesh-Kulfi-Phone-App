package com.ganeshkulfi.backend.services

import com.ganeshkulfi.backend.data.models.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for PasswordService.
 * Tests password hashing, verification, and strength validation.
 */
class PasswordServiceTest {

    private val passwordService = PasswordService()

    @Test
    fun `hashPassword returns non-empty string`() {
        val hash = passwordService.hashPassword("TestPassword1")
        assertNotNull(hash)
        assertTrue(hash.isNotBlank())
    }

    @Test
    fun `verifyPassword returns true for correct password`() {
        val password = "TestPassword1"
        val hash = passwordService.hashPassword(password)
        assertTrue(passwordService.verifyPassword(password, hash))
    }

    @Test
    fun `verifyPassword returns false for wrong password`() {
        val hash = passwordService.hashPassword("TestPassword1")
        assertTrue(!passwordService.verifyPassword("WrongPassword1", hash))
    }

    @Test
    fun `validatePasswordStrength rejects short passwords`() {
        val result = passwordService.validatePasswordStrength("Ab1")
        assertTrue(result.isFailure)
    }

    @Test
    fun `validatePasswordStrength rejects passwords without uppercase`() {
        val result = passwordService.validatePasswordStrength("abcdefg1")
        assertTrue(result.isFailure)
    }

    @Test
    fun `validatePasswordStrength rejects passwords without digit`() {
        val result = passwordService.validatePasswordStrength("Abcdefgh")
        assertTrue(result.isFailure)
    }

    @Test
    fun `validatePasswordStrength accepts valid password`() {
        val result = passwordService.validatePasswordStrength("ValidPass1")
        assertTrue(result.isSuccess)
    }
}
