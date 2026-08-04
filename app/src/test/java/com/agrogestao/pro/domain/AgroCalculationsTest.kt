package com.agrogestao.pro.domain

import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgroCalculationsTest {
    @Test
    fun `financial summary separates income and expenses`() {
        val summary = calculateFinancialSummary(
            listOf(
                transaction(1, 1_500.0, TransactionType.ENTRADA),
                transaction(2, 250.0, TransactionType.SAIDA),
                transaction(3, 100.0, TransactionType.SAIDA)
            )
        )

        assertEquals(1_500.0, summary.income, 0.0)
        assertEquals(350.0, summary.expenses, 0.0)
        assertEquals(1_150.0, summary.balance, 0.0)
    }

    @Test
    fun `positive decimal accepts Brazilian comma`() {
        assertEquals(2.5, requireNotNull(parsePositiveDecimal(" 2,5 ")), 0.0)
    }

    @Test
    fun `positive decimal rejects invalid values`() {
        listOf("", "abc", "0", "-1", "NaN", "Infinity").forEach {
            assertNull(parsePositiveDecimal(it))
        }
    }

    @Test
    fun `percentage accepts only whole values from zero to one hundred`() {
        assertEquals(0, parsePercentage("0"))
        assertEquals(65, parsePercentage(" 65 "))
        assertEquals(100, parsePercentage("100"))
        listOf("", "-1", "101", "20,5", "texto").forEach {
            assertNull(parsePercentage(it))
        }
    }

    private fun transaction(id: Long, value: Double, type: TransactionType) =
        FinancialEntity(
            id = id,
            descricao = "Teste",
            valor = value,
            tipo = type,
            data = "2026-07-29",
            categoria = "Teste"
        )
}
