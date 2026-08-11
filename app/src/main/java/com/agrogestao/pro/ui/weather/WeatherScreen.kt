package com.agrogestao.pro.ui.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agrogestao.pro.data.repository.AgroRepository
import com.agrogestao.pro.data.weather.OpenMeteoWeatherClient
import com.agrogestao.pro.data.weather.WeatherForecast
import com.agrogestao.pro.data.weather.WeatherPreferences
import com.agrogestao.pro.data.weather.buildInformationalWeatherAlerts
import com.agrogestao.pro.data.weather.weatherCodeLabel
import com.agrogestao.pro.domain.formatDateForDisplay
import com.agrogestao.pro.ui.components.AppScreenHeader
import com.agrogestao.pro.ui.theme.BackgroundLight
import com.agrogestao.pro.ui.theme.PrimaryAgroGreen
import com.agrogestao.pro.ui.theme.StatusOrange
import com.agrogestao.pro.ui.theme.SurfaceCard
import com.agrogestao.pro.ui.theme.TextDark
import com.agrogestao.pro.ui.theme.TextMuted
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val STALE_AFTER_MILLIS = 6 * 60 * 60 * 1000L

@Composable
fun WeatherScreen(repository: AgroRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    val producer by repository.producerProfile.collectAsState(initial = null)
    val owner by repository.activeOwnerUserId.collectAsState(initial = "")
    val preferences = remember { WeatherPreferences(context.applicationContext) }
    val client = remember { OpenMeteoWeatherClient() }
    val scope = rememberCoroutineScope()
    var hasConsent by remember(owner) { mutableStateOf(preferences.hasConsent(owner)) }
    var forecast by remember(owner) { mutableStateOf(preferences.read(owner)) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConsent by remember { mutableStateOf(false) }

    fun refresh() {
        if (!hasConsent) {
            showConsent = true
            return
        }
        val municipality = producer?.municipioUF.orEmpty()
        scope.launch {
            isLoading = true
            errorMessage = null
            runCatching {
                withContext(Dispatchers.IO) { client.fetch(municipality) }
            }.fold(
                onSuccess = {
                    preferences.save(owner, it)
                    forecast = it
                },
                onFailure = {
                    errorMessage = it.message ?: "Não foi possível atualizar a previsão."
                }
            )
            isLoading = false
        }
    }

    LaunchedEffect(owner, hasConsent) {
        if (owner.isNotBlank() && hasConsent && forecast == null && !isLoading) refresh()
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        AppScreenHeader(title = "Clima", subtitle = producer?.municipioUF.orEmpty(), onBack = onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Previsão opcional por município. Não usa a localização precisa do celular.",
                color = TextMuted
            )

            if (!hasConsent) {
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = PrimaryAgroGreen)
                        Text("Consulta desativada", color = TextDark, fontWeight = FontWeight.Bold)
                        Text(
                            "O município informado no perfil só será enviado ao Open-Meteo depois da sua autorização.",
                            color = TextMuted
                        )
                        Button(onClick = { showConsent = true }) { Text("Autorizar consulta") }
                    }
                }
            }

            errorMessage?.let {
                Card(colors = CardDefaults.cardColors(containerColor = StatusOrange.copy(alpha = 0.12f))) {
                    Text(it, modifier = Modifier.padding(16.dp), color = TextDark)
                }
            }

            if (isLoading) {
                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = PrimaryAgroGreen)
                }
            }

            forecast?.let { current ->
                ForecastSummary(current)
                val alerts = buildInformationalWeatherAlerts(current)
                if (alerts.isNotEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = StatusOrange.copy(alpha = 0.12f))) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = StatusOrange)
                                Text(" Alertas informativos", fontWeight = FontWeight.Bold, color = TextDark)
                            }
                            alerts.forEach { Text("${formatDateForDisplay(it.date)}: ${it.message}", color = TextDark) }
                            Text("Não são recomendações agronômicas.", color = TextMuted)
                        }
                    }
                }
            }

            if (hasConsent) {
                Button(onClick = ::refresh, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text(" Atualizar previsão")
                }
                OutlinedButton(
                    onClick = {
                        preferences.revokeConsent(owner)
                        hasConsent = false
                        forecast = null
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Desativar e apagar previsão salva") }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showConsent) {
        AlertDialog(
            onDismissRequest = { showConsent = false },
            title = { Text("Autorizar consulta de clima?") },
            text = {
                Text(
                    "O app enviará o município e a UF do seu perfil ao Open-Meteo para localizar a cidade e obter a previsão. A última resposta ficará salva neste celular para uso offline. Nenhum GPS será acessado."
                )
            },
            confirmButton = {
                Button(onClick = {
                    preferences.grantConsent(owner)
                    hasConsent = true
                    showConsent = false
                }) { Text("Autorizar") }
            },
            dismissButton = { TextButton(onClick = { showConsent = false }) { Text("Agora não") } }
        )
    }
}

@Composable
private fun ForecastSummary(forecast: WeatherForecast) {
    val isStale = System.currentTimeMillis() - forecast.fetchedAtEpochMillis > STALE_AFTER_MILLIS
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(forecast.resolvedLocation, color = TextDark, fontWeight = FontWeight.ExtraBold)
            Text(
                "Fonte: Open-Meteo • Atualizada ${DateFormat.getDateTimeInstance().format(Date(forecast.fetchedAtEpochMillis))}" +
                    if (isStale) " • dados salvos/desatualizados" else "",
                color = if (isStale) StatusOrange else TextMuted
            )
            forecast.days.forEach { day ->
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "${formatDateForDisplay(day.date)} — ${weatherCodeLabel(day.weatherCode)}",
                        color = TextDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${day.minTemperatureC.toInt()}–${day.maxTemperatureC.toInt()} °C • chuva ${day.precipitationMm.toInt()} mm • rajadas ${day.maxWindGustKmh.toInt()} km/h",
                        color = TextMuted
                    )
                }
            }
            Text("Previsões podem mudar e não substituem orientação técnica.", color = TextMuted)
        }
    }
}
