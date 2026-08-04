package com.agrogestao.pro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountPasswordTest {
    @Test
    fun rejectsShortPassword() {
        assertEquals(
            "A senha precisa ter pelo menos 8 caracteres.",
            accountPasswordError("1234567")
        )
    }

    @Test
    fun rejectsDifferentConfirmation() {
        assertEquals("As senhas não são iguais.", accountPasswordError("12345678", "87654321"))
    }

    @Test
    fun acceptsMatchingPassword() {
        assertNull(accountPasswordError("senha-segura", "senha-segura"))
    }
}
