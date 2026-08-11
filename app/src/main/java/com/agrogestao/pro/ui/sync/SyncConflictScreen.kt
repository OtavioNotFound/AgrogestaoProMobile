package com.agrogestao.pro.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agrogestao.pro.data.repository.AgroRepository
import com.agrogestao.pro.ui.components.AppScreenHeader
import com.agrogestao.pro.ui.theme.BackgroundLight
import com.agrogestao.pro.ui.theme.SurfaceCard
import com.agrogestao.pro.ui.theme.TextDark
import com.agrogestao.pro.ui.theme.TextMuted
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@Composable
fun SyncConflictScreen(repository: AgroRepository, onBack: () -> Unit) {
    val conflicts by repository.syncConflicts.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(BackgroundLight)) {
        AppScreenHeader(title = "Histórico de conflitos", onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Registra quando uma alteração local pendente encontra outra versão na nuvem. O app mantém a versão com data mais recente.",
                color = TextMuted
            )
            if (conflicts.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard)) {
                    Text("Nenhum conflito registrado nesta conta.", Modifier.padding(18.dp), color = TextDark)
                }
            }
            conflicts.forEach { conflict ->
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(conflict.entityType.replaceFirstChar(Char::uppercase), fontWeight = FontWeight.Bold, color = TextDark)
                        Text(
                            if (conflict.resolution == "REMOTE_WON") "A versão da nuvem foi aplicada" else "A versão deste celular foi mantida",
                            color = TextDark
                        )
                        Text(DateFormat.getDateTimeInstance().format(Date(conflict.detectedAtEpochMillis)), color = TextMuted)
                        Text("ID ${conflict.entityCloudId.take(12)}…", color = TextMuted)
                    }
                }
            }
            if (conflicts.isNotEmpty()) {
                Button(onClick = { scope.launch { repository.clearSyncConflicts() } }, modifier = Modifier.fillMaxWidth()) {
                    Text("Limpar histórico")
                }
            }
        }
    }
}
