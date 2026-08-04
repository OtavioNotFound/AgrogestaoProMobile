package com.agrogestao.pro.domain

import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val isoLocale = Locale.US
private val displayLocale = Locale("pt", "BR")

fun todayIso(): String = isoFormatter().format(Date())

fun todayPlusMonthsIso(months: Int): String {
    val calendar = Calendar.getInstance().apply {
        add(Calendar.MONTH, months)
    }
    return isoFormatter().format(calendar.time)
}

fun toIsoDate(year: Int, zeroBasedMonth: Int, dayOfMonth: Int): String =
    String.format(isoLocale, "%04d-%02d-%02d", year, zeroBasedMonth + 1, dayOfMonth)

fun isoDateParts(value: String): Triple<Int, Int, Int>? {
    val parsed = parseIso(value) ?: return null
    val calendar = Calendar.getInstance().apply { time = parsed }
    return Triple(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
}

fun formatDateForDisplay(value: String): String {
    val parsed = parseIso(value) ?: return value
    return SimpleDateFormat("dd/MM/yyyy", displayLocale).format(parsed)
}

fun isIsoDateOnOrAfter(value: String, reference: String): Boolean {
    val date = parseIso(value) ?: return false
    val referenceDate = parseIso(reference) ?: return false
    return !date.before(referenceDate)
}

private fun parseIso(value: String): Date? {
    val position = ParsePosition(0)
    val parsed = isoFormatter().parse(value, position) ?: return null
    return parsed.takeIf { position.index == value.length }
}

private fun isoFormatter() = SimpleDateFormat("yyyy-MM-dd", isoLocale).apply {
    isLenient = false
}
