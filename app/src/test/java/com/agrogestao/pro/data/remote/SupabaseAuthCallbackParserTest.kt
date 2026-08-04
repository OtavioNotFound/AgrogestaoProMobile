package com.agrogestao.pro.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseAuthCallbackParserTest {

    @Test
    fun parsesSessionFromImplicitGrantFragment() {
        val callback = SupabaseAuthCallbackParser.parse(
            "com.agrogestao.pro://auth/callback#access_token=jwt-token&" +
                "refresh_token=refresh%2Btoken%3D&expires_in=3600&type=signup"
        )

        assertNotNull(callback)
        assertEquals("jwt-token", callback?.accessToken)
        assertEquals("refresh+token=", callback?.refreshToken)
        assertEquals(3600L, callback?.expiresInSeconds)
        assertEquals("signup", callback?.type)
        assertTrue(callback?.hasSession == true)
    }

    @Test
    fun parsesErrorWithoutTrustingDescription() {
        val callback = SupabaseAuthCallbackParser.parse(
            "com.agrogestao.pro://auth/callback?error=access_denied&" +
                "error_description=mensagem+externa"
        )

        assertEquals("access_denied", callback?.errorCode)
        assertFalse(callback?.hasSession == true)
    }

    @Test
    fun identifiesPasswordRecoverySession() {
        val callback = SupabaseAuthCallbackParser.parse(
            "com.agrogestao.pro://auth/callback#access_token=recovery-access&" +
                "refresh_token=recovery-refresh&expires_in=900&type=recovery"
        )

        assertEquals("recovery", callback?.type)
        assertTrue(callback?.hasSession == true)
        assertEquals(900L, callback?.expiresInSeconds)
    }

    @Test
    fun acceptsOnlyTheExactAppCallback() {
        assertNotNull(
            SupabaseAuthCallbackParser.parse(
                "com.agrogestao.pro://auth/callback/#access_token=a&refresh_token=b"
            )
        )
        assertNull(
            SupabaseAuthCallbackParser.parse(
                "other.app://auth/callback#access_token=a&refresh_token=b"
            )
        )
        assertNull(
            SupabaseAuthCallbackParser.parse(
                "com.agrogestao.pro://other/callback#access_token=a&refresh_token=b"
            )
        )
        assertNull(
            SupabaseAuthCallbackParser.parse(
                "com.agrogestao.pro://auth/other#access_token=a&refresh_token=b"
            )
        )
    }
}
