package com.agrogestao.pro.data.weather

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import org.json.JSONObject

class OpenMeteoWeatherClient {
    fun fetch(municipalityUf: String): WeatherForecast {
        val municipality = municipalityUf.substringBefore("-").trim()
        require(municipality.length >= 2) { "Informe o município no perfil antes de consultar o clima." }
        val stateCode = municipalityUf.substringAfterLast("-", "").trim().uppercase()
        val location = geocode(municipality, stateCode)
        return forecast(municipalityUf, location)
    }

    private fun geocode(municipality: String, stateCode: String): LocationResult {
        val encoded = URLEncoder.encode(municipality, Charsets.UTF_8.name())
        val json = getJson(
            "https://geocoding-api.open-meteo.com/v1/search" +
                "?name=$encoded&count=5&language=pt&countryCode=BR&format=json"
        )
        val results = json.optJSONArray("results")
        check(results != null && results.length() > 0) {
            "Município não encontrado. Confira o município e a UF no perfil."
        }
        val candidates = List(results.length()) { results.getJSONObject(it) }
        val expectedState = brazilianStates[stateCode]
        val first = if (expectedState == null) {
            candidates.first()
        } else {
            candidates.firstOrNull {
                normalize(it.optString("admin1")) == normalize(expectedState)
            } ?: error("Município não encontrado na UF informada. Confira o perfil.")
        }
        return LocationResult(
            name = listOf(first.optString("name"), first.optString("admin1"))
                .filter(String::isNotBlank).joinToString(" - "),
            latitude = first.getDouble("latitude"),
            longitude = first.getDouble("longitude")
        )
    }

    private fun forecast(requested: String, location: LocationResult): WeatherForecast {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${location.latitude}&longitude=${location.longitude}" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,wind_gusts_10m_max" +
            "&timezone=America%2FSao_Paulo&forecast_days=7"
        val daily = getJson(url).getJSONObject("daily")
        val dates = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val maximums = daily.getJSONArray("temperature_2m_max")
        val minimums = daily.getJSONArray("temperature_2m_min")
        val rain = daily.getJSONArray("precipitation_sum")
        val gusts = daily.getJSONArray("wind_gusts_10m_max")
        val size = listOf(dates.length(), codes.length(), maximums.length(), minimums.length(), rain.length(), gusts.length()).min()
        check(size > 0) { "O serviço de clima retornou uma previsão incompleta." }
        return WeatherForecast(
            requestedMunicipality = requested,
            resolvedLocation = location.name,
            latitude = location.latitude,
            longitude = location.longitude,
            fetchedAtEpochMillis = System.currentTimeMillis(),
            days = List(size.coerceAtMost(7)) { index ->
                WeatherDay(
                    date = dates.getString(index),
                    weatherCode = codes.getInt(index),
                    minTemperatureC = minimums.getDouble(index),
                    maxTemperatureC = maximums.getDouble(index),
                    precipitationMm = rain.getDouble(index),
                    maxWindGustKmh = gusts.getDouble(index)
                )
            }
        )
    }

    private fun getJson(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 12_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "AgroGestao-Android")
            val code = connection.responseCode
            check(code in 200..299) { "Serviço de clima indisponível (código $code)." }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            check(body.length <= 1_000_000) { "Resposta do clima excedeu o limite seguro." }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private data class LocationResult(val name: String, val latitude: Double, val longitude: Double)

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()

    private companion object {
        val brazilianStates = mapOf(
            "AC" to "Acre", "AL" to "Alagoas", "AP" to "Amapá", "AM" to "Amazonas",
            "BA" to "Bahia", "CE" to "Ceará", "DF" to "Distrito Federal",
            "ES" to "Espírito Santo", "GO" to "Goiás", "MA" to "Maranhão",
            "MT" to "Mato Grosso", "MS" to "Mato Grosso do Sul", "MG" to "Minas Gerais",
            "PA" to "Pará", "PB" to "Paraíba", "PR" to "Paraná", "PE" to "Pernambuco",
            "PI" to "Piauí", "RJ" to "Rio de Janeiro", "RN" to "Rio Grande do Norte",
            "RS" to "Rio Grande do Sul", "RO" to "Rondônia", "RR" to "Roraima",
            "SC" to "Santa Catarina", "SP" to "São Paulo", "SE" to "Sergipe",
            "TO" to "Tocantins"
        )
    }
}
