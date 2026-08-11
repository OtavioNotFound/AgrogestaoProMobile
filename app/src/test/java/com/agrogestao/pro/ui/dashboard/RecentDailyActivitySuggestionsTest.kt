package com.agrogestao.pro.ui.dashboard

import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.domain.DailyActivityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RecentDailyActivitySuggestionsTest {
    @Test
    fun newestDistinctAnswersAreOfferedAgainForTheSameActivity() {
        val tasks = listOf(
            task("Sementes", "Compra", updatedAt = 10),
            task("Adubo", "Compra", updatedAt = 30),
            task("adubo", "Compra", updatedAt = 20),
            task("Feijão", "Colheita", updatedAt = 40)
        )

        val suggestions = recentDailyActivitySuggestions(tasks)

        assertEquals(listOf("Adubo", "Sementes"), suggestions[DailyActivityType.BOUGHT])
        assertEquals(listOf("Feijão"), suggestions[DailyActivityType.HARVESTED])
    }

    @Test
    fun genericLegacyDescriptionIsNeverShownAsAQuickChoice() {
        val suggestions = recentDailyActivitySuggestions(
            listOf(task("Registrado rapidamente pela tela Hoje.", "Compra", updatedAt = 1))
        )

        assertFalse(suggestions.containsKey(DailyActivityType.BOUGHT))
    }

    private fun task(description: String, category: String, updatedAt: Long) = TaskEntity(
        titulo = "Registro",
        descricao = description,
        categoria = category,
        dataLimite = "2026-08-08",
        updatedAtEpochMillis = updatedAt
    )
}
