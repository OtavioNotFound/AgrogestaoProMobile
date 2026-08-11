package com.agrogestao.pro.domain

import java.math.BigDecimal
import java.math.RoundingMode

enum class DailyActivityType(
    val userLabel: String,
    val category: String,
    val requiresAmount: Boolean = false
) {
    PLANTED("Plantei", "Plantio"),
    HARVESTED("Colhi", "Colheita"),
    BOUGHT("Comprei", "Compra", requiresAmount = true),
    SOLD("Vendi", "Venda", requiresAmount = true),
    PAID("Paguei", "Pagamento", requiresAmount = true),
    RECEIVED("Recebi", "Recebimento", requiresAmount = true),
    USED_INPUT("Usei insumo", "Insumo"),
    FOUND_PROBLEM("Encontrei problema", "Problema"),
    OTHER("Outra atividade", "Outros");

    val createsIncome: Boolean
        get() = this == SOLD || this == RECEIVED

    val createsExpense: Boolean
        get() = this == BOUGHT || this == PAID

    val createsFinancialRecord: Boolean
        get() = createsIncome || createsExpense

    val leavesOpenTask: Boolean
        get() = this == FOUND_PROBLEM
}

data class DailyActivityRequest(
    val type: DailyActivityType,
    val dateIso: String,
    val amountCents: Long? = null,
    val note: String = "",
    val cropCloudId: String? = null
)

fun dailyActivityValidationError(request: DailyActivityRequest): String? = when {
    isoDateParts(request.dateIso) == null -> "A data do registro não é válida."
    request.note.trim().isBlank() -> dailyActivitySubjectError(request.type)
    request.type.requiresAmount && (request.amountCents ?: 0L) <= 0L ->
        "Informe um valor maior que zero."
    else -> null
}

fun dailyActivitySubjectError(type: DailyActivityType): String = when (type) {
    DailyActivityType.PLANTED -> "Informe o que você plantou."
    DailyActivityType.HARVESTED -> "Informe o que você colheu."
    DailyActivityType.BOUGHT -> "Informe o que você comprou."
    DailyActivityType.SOLD -> "Informe o que você vendeu."
    DailyActivityType.PAID -> "Informe o que você pagou."
    DailyActivityType.RECEIVED -> "Informe de quem ou do que você recebeu."
    DailyActivityType.USED_INPUT -> "Informe qual insumo você usou."
    DailyActivityType.FOUND_PROBLEM ->
        "Conte em poucas palavras qual problema você encontrou."
    DailyActivityType.OTHER -> "Conte em poucas palavras o que você fez."
}

fun dailyActivityTaskTitle(
    type: DailyActivityType,
    note: String,
    cropName: String?
): String {
    val cleanNote = note.trim().take(90)
    val place = cropName
        ?.trim()
        .orEmpty()
        .takeIf { it.isNotBlank() && !cleanNote.contains(it, ignoreCase = true) }
        ?.let { " • $it" }
        .orEmpty()
    return when (type) {
        DailyActivityType.PLANTED -> "Plantio de $cleanNote$place"
        DailyActivityType.HARVESTED -> "Colheita de $cleanNote$place"
        DailyActivityType.BOUGHT -> "Compra: $cleanNote"
        DailyActivityType.SOLD -> "Venda: $cleanNote"
        DailyActivityType.PAID -> "Pagamento: $cleanNote"
        DailyActivityType.RECEIVED -> "Recebimento: $cleanNote"
        DailyActivityType.USED_INPUT -> "Insumo usado: $cleanNote$place"
        DailyActivityType.FOUND_PROBLEM -> "Problema: $cleanNote"
        DailyActivityType.OTHER -> cleanNote
    }
}

fun parseMoneyTextToCents(text: String): Long? {
    var normalized = text
        .trim()
        .replace("R$", "", ignoreCase = true)
        .replace(" ", "")
    if (normalized.isBlank()) return null

    val lastComma = normalized.lastIndexOf(',')
    val lastDot = normalized.lastIndexOf('.')
    normalized = when {
        lastComma >= 0 && lastDot >= 0 && lastComma > lastDot ->
            normalized.replace(".", "").replace(',', '.')
        lastComma >= 0 && lastDot >= 0 -> normalized.replace(",", "")
        lastComma >= 0 -> normalized.replace(',', '.')
        else -> normalized
    }

    return runCatching {
        BigDecimal(normalized)
            .setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2)
            .longValueExact()
    }.getOrNull()
}
