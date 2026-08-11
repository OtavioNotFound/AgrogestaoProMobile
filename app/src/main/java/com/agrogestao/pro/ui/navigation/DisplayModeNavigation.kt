package com.agrogestao.pro.ui.navigation

internal fun displayModeBottomRoutes(simpleMode: Boolean): List<String> =
    if (simpleMode) {
        listOf("dashboard", "kanban", "safras", "more")
    } else {
        listOf("dashboard", "safras", "kanban", "relatorio", "profile")
    }

internal fun displayModeSelectedRoute(simpleMode: Boolean, currentRoute: String?): String? =
    if (
        simpleMode && currentRoute in setOf(
            "more",
            "profile",
            "relatorio",
            "weather",
            "sync_conflicts"
        )
    ) "more" else currentRoute
