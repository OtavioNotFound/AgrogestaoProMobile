package com.agrogestao.pro.domain

import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.TransactionType

data class FinancialSummary(
    val income: Double,
    val expenses: Double,
    val balance: Double
)

fun calculateFinancialSummary(transactions: List<FinancialEntity>): FinancialSummary {
    val income = transactions.asSequence()
        .filter { it.tipo == TransactionType.ENTRADA }
        .sumOf { it.valor }
    val expenses = transactions.asSequence()
        .filter { it.tipo == TransactionType.SAIDA }
        .sumOf { it.valor }
    return FinancialSummary(income, expenses, income - expenses)
}

fun parsePositiveDecimal(value: String): Double? =
    value.trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0.0 }

fun parsePercentage(value: String): Int? =
    value.trim().toIntOrNull()?.takeIf { it in 0..100 }
