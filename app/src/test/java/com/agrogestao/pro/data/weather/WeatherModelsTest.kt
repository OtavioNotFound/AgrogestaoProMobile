package com.agrogestao.pro.data.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherModelsTest {
    @Test
    fun `alerts are informational and use explicit thresholds`() {
        val alerts = buildInformationalWeatherAlerts(
            WeatherForecast(
                requestedMunicipality = "Teste - BA",
                resolvedLocation = "Teste - Bahia",
                latitude = 0.0,
                longitude = 0.0,
                fetchedAtEpochMillis = 1,
                days = listOf(WeatherDay("2026-08-05", 95, 20.0, 39.0, 55.0, 65.0))
            )
        )

        assertEquals(3, alerts.size)
        assertTrue(alerts.all { it.date == "2026-08-05" })
    }

    @Test
    fun `weather codes have safe fallback`() {
        assertEquals("Trovoada", weatherCodeLabel(95))
        assertEquals("Condição variável", weatherCodeLabel(999))
    }
}
