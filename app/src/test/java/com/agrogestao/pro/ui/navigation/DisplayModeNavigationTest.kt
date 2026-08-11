package com.agrogestao.pro.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayModeNavigationTest {
    @Test
    fun simpleModeKeepsOnlyFourPrimaryDestinations() {
        assertEquals(
            listOf("dashboard", "kanban", "safras", "more"),
            displayModeBottomRoutes(simpleMode = true)
        )
    }

    @Test
    fun completeModeKeepsAllExistingDestinations() {
        assertEquals(
            listOf("dashboard", "safras", "kanban", "relatorio", "profile"),
            displayModeBottomRoutes(simpleMode = false)
        )
    }

    @Test
    fun secondarySimpleRoutesHighlightMore() {
        assertEquals("more", displayModeSelectedRoute(true, "profile"))
        assertEquals("more", displayModeSelectedRoute(true, "relatorio"))
        assertEquals("more", displayModeSelectedRoute(true, "weather"))
        assertEquals("more", displayModeSelectedRoute(true, "sync_conflicts"))
        assertEquals("kanban", displayModeSelectedRoute(true, "kanban"))
        assertEquals("profile", displayModeSelectedRoute(false, "profile"))
    }

    @Test
    fun simpleModeActuallyEnlargesComponentsAndText() {
        assertEquals(1f, displayModeComponentScale(false))
        assertEquals(1f, displayModeAdditionalFontScale(false))
        assertTrue(displayModeComponentScale(true) >= 1.15f)
        assertTrue(displayModeAdditionalFontScale(true) > 1f)
    }
}
