package com.agrogestao.pro.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPolicyTest {
    @Test
    fun `same account may reuse local profile regardless of email case`() {
        assertTrue(canUseLocalProfile("Produtor@Email.com", "produtor@email.com"))
    }

    @Test
    fun `empty device may accept an account`() {
        assertTrue(canUseLocalProfile(null, "produtor@email.com"))
        assertTrue(canUseLocalProfile("", "produtor@email.com"))
    }

    @Test
    fun `different account cannot inherit local farm data`() {
        assertFalse(canUseLocalProfile("conta-a@email.com", "conta-b@email.com"))
    }

    @Test
    fun `token refresh starts before expiration`() {
        assertTrue(shouldRefreshToken(expiresAtEpochSeconds = 1_050, nowEpochSeconds = 1_000))
        assertFalse(shouldRefreshToken(expiresAtEpochSeconds = 1_061, nowEpochSeconds = 1_000))
        assertTrue(shouldRefreshToken(expiresAtEpochSeconds = 900, nowEpochSeconds = 1_000))
    }

    @Test
    fun `token expiration is calculated from server lifetime`() {
        assertTrue(tokenExpiryEpochSeconds(1_000, 3_600) == 4_600L)
    }
}
