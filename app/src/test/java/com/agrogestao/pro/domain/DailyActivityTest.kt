package com.agrogestao.pro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyActivityTest {
    @Test
    fun moneyParserAcceptsTheFormatsProducersUsuallyType() {
        assertEquals(1_250L, parseMoneyTextToCents("12,50"))
        assertEquals(123_456L, parseMoneyTextToCents("R$ 1.234,56"))
        assertEquals(150_000L, parseMoneyTextToCents("1500"))
        assertNull(parseMoneyTextToCents(""))
        assertNull(parseMoneyTextToCents("doze reais"))
    }

    @Test
    fun financialRecordRequiresAPositiveAmount() {
        val request = DailyActivityRequest(
            type = DailyActivityType.BOUGHT,
            dateIso = "2026-08-07",
            amountCents = 0,
            note = "Sementes"
        )

        assertEquals("Informe um valor maior que zero.", dailyActivityValidationError(request))
        assertNull(dailyActivityValidationError(request.copy(amountCents = 2_500)))
    }

    @Test
    fun problemKeepsTheProducersOwnWordsAndBecomesAnOpenTask() {
        val type = DailyActivityType.FOUND_PROBLEM

        assertTrue(type.leavesOpenTask)
        assertEquals(
            "Problema: Cerca quebrada",
            dailyActivityTaskTitle(type, "  Cerca quebrada  ", "Talhão 2")
        )
    }

    @Test
    fun plantedRecordKeepsWhatWasPlantedAndUsesTheSelectedArea() {
        val request = DailyActivityRequest(
            type = DailyActivityType.PLANTED,
            dateIso = "2026-08-07",
            note = "Feijão",
            cropCloudId = "crop-1"
        )

        assertNull(dailyActivityValidationError(request))
        assertEquals(
            "Plantio de Feijão",
            dailyActivityTaskTitle(request.type, request.note, "Feijão")
        )
    }

    @Test
    fun everyQuickActivityRequiresTheProducerToSayWhatItWas() {
        DailyActivityType.entries.forEach { type ->
            val request = DailyActivityRequest(
                type = type,
                dateIso = "2026-08-07",
                amountCents = if (type.requiresAmount) 1_000 else null
            )

            assertEquals(dailyActivitySubjectError(type), dailyActivityValidationError(request))
        }
    }

    @Test
    fun financialTitlesShowWhatWasBoughtSoldOrPaid() {
        assertEquals(
            "Compra: Sementes de milho",
            dailyActivityTaskTitle(DailyActivityType.BOUGHT, "Sementes de milho", null)
        )
        assertEquals(
            "Pagamento: Conta de luz",
            dailyActivityTaskTitle(DailyActivityType.PAID, "Conta de luz", null)
        )
    }
}
