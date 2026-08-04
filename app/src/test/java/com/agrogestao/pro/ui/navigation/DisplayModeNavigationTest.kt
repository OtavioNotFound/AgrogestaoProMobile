package com.agrogestao.pro.ui.navigation

import org.junit.Assert.assertEquals
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
        assertEquals("kanban", displayModeSelectedRoute(true, "kanban"))
        assertEquals("profile", displayModeSelectedRoute(false, "profile"))
    }
}
