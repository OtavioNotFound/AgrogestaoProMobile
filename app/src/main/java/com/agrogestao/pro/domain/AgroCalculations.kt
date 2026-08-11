package com.agrogestao.pro.domain

import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

data class FinancialSummary(
    val income: Double,
    val expenses: Double,
    val balance: Double
)

fun calculateFinancialSummary(transactions: List<FinancialEntity>): FinancialSummary {
    val incomeCents = transactions.asSequence()
        .filter { it.tipo == TransactionType.ENTRADA }
        .sumOf { it.valorCentavos }
    val expensesCents = transactions.asSequence()
        .filter { it.tipo == TransactionType.SAIDA }
        .sumOf { it.valorCentavos }
    val income = centsToDouble(incomeCents)
    val expenses = centsToDouble(expensesCents)
    return FinancialSummary(income, expenses, income - expenses)
}

fun moneyToCents(value: Double): Long = BigDecimal.valueOf(value)
    .setScale(2, RoundingMode.HALF_UP)
    .movePointRight(2)
    .longValueExact()

fun centsToDouble(value: Long): Double = BigDecimal.valueOf(value, 2).toDouble()

fun parsePositiveDecimal(value: String): Double? =
    value.trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0.0 }

fun parsePositiveMoneyCents(value: String): Long? = runCatching {
    BigDecimal(value.trim().replace(',', '.'))
        .setScale(2, RoundingMode.HALF_UP)
        .movePointRight(2)
        .longValueExact()
        .takeIf { it > 0L }
}.getOrNull()

fun parsePercentage(value: String): Int? =
    value.trim().toIntOrNull()?.takeIf { it in 0..100 }
