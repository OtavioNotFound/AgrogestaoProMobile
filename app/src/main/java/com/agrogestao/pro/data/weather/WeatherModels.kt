package com.agrogestao.pro.data.weather

data class WeatherDay(
    val date: String,
    val weatherCode: Int,
    val minTemperatureC: Double,
    val maxTemperatureC: Double,
    val precipitationMm: Double,
    val maxWindGustKmh: Double
)

data class WeatherForecast(
    val requestedMunicipality: String,
    val resolvedLocation: String,
    val latitude: Double,
    val longitude: Double,
    val fetchedAtEpochMillis: Long,
    val days: List<WeatherDay>
)

enum class WeatherAlertKind { HEAVY_RAIN, HEAT, WIND }

data class WeatherAlert(
    val date: String,
    val kind: WeatherAlertKind,
    val message: String
)

fun buildInformationalWeatherAlerts(forecast: WeatherForecast): List<WeatherAlert> =
    forecast.days.flatMap { day ->
        buildList {
            if (day.precipitationMm >= 50.0) {
                add(WeatherAlert(day.date, WeatherAlertKind.HEAVY_RAIN, "Possibilidade de chuva forte (${day.precipitationMm.toInt()} mm)."))
            }
            if (day.maxTemperatureC >= 38.0) {
                add(WeatherAlert(day.date, WeatherAlertKind.HEAT, "Calor intenso previsto (${day.maxTemperatureC.toInt()} °C)."))
            }
            if (day.maxWindGustKmh >= 60.0) {
                add(WeatherAlert(day.date, WeatherAlertKind.WIND, "Rajadas fortes previstas (${day.maxWindGustKmh.toInt()} km/h)."))
            }
        }
    }

fun weatherCodeLabel(code: Int): String = when (code) {
    0 -> "Céu limpo"
    1, 2 -> "Parcialmente nublado"
    3 -> "Nublado"
    45, 48 -> "Neblina"
    51, 53, 55, 56, 57 -> "Garoa"
    61, 63, 65, 66, 67, 80, 81, 82 -> "Chuva"
    71, 73, 75, 77, 85, 86 -> "Neve"
    95, 96, 99 -> "Trovoada"
    else -> "Condição variável"
}
