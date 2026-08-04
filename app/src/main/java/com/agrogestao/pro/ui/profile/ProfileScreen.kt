package com.agrogestao.pro.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogestao.pro.BuildConfig
import com.agrogestao.pro.data.remote.SupabaseConfig
import com.agrogestao.pro.ui.components.AppScreenHeader
import com.agrogestao.pro.ui.components.InitialsAvatar
import com.agrogestao.pro.ui.components.MenuListItem
import com.agrogestao.pro.ui.components.PrototypeCard
import com.agrogestao.pro.ui.components.SectionLabel
import com.agrogestao.pro.ui.components.SmallIconTile
import com.agrogestao.pro.ui.dashboard.DashboardViewModel
import com.agrogestao.pro.ui.relatorios.EditProfileDialog
import com.agrogestao.pro.ui.relatorios.RelatorioCreditoViewModel
import com.agrogestao.pro.ui.theme.AgroGreen050
import com.agrogestao.pro.ui.theme.AgroGreen100
import com.agrogestao.pro.ui.theme.AgroGreen900
import com.agrogestao.pro.ui.theme.CardBorder
import com.agrogestao.pro.ui.theme.PrimaryAgroGreen
import com.agrogestao.pro.ui.theme.StatusGreen
import com.agrogestao.pro.ui.theme.StatusOrange
import com.agrogestao.pro.ui.theme.StatusRedSoft
import com.agrogestao.pro.ui.theme.SurfaceCard
import com.agrogestao.pro.ui.theme.TextDark
import com.agrogestao.pro.ui.theme.TextMuted

@Composable
fun ProfileScreen(
    dashboardViewModel: DashboardViewModel,
    reportViewModel: RelatorioCreditoViewModel,
    onBack: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToBackup: () -> Unit
) {
    val dashboard by dashboardViewModel.uiState.collectAsState()
    val report by reportViewModel.uiState.collectAsState()
    val producer = dashboard.producer
    var showEditProfile by remember { mutableStateOf(false) }
    var showLogout by remember { mutableStateOf(false) }
    var infoDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    val producerName = producer?.nomeProdutor.orEmpty().ifBlank { "Produtor" }
    val farmName = producer?.nomePropriedade.orEmpty().ifBlank { "Propriedade não informada" }
    val location = producer?.municipioUF.orEmpty().ifBlank { "Localização não informada" }
    val isSynced = producer?.syncStatus == SupabaseConfig.STATUS_SYNCED_CLOUD
    val cropsLabel = dashboard.safrasAtivas.map { it.nomeCultura }.distinct().joinToString()

    Column(modifier = Modifier.fillMaxSize().background(SurfaceCard)) {
        AppScreenHeader(
            title = "Perfil",
            onBack = onBack,
            actionIcon = Icons.Default.Edit,
            actionDescription = "Editar perfil",
            onAction = { showEditProfile = true }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                InitialsAvatar(producerName)
                Column(modifier = Modifier.padding(start = 13.dp)) {
                    Text(producerName, color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("$farmName · $location", color = TextMuted, fontSize = 11.5.sp, lineHeight = 16.sp)
                    Text("✓  Cadastro verificado", color = PrimaryAgroGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !dashboard.isSyncing, onClick = dashboardViewModel::syncNow)
                    .background(AgroGreen050, RoundedCornerShape(14.dp))
                    .padding(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallIconTile(
                    icon = if (isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    backgroundColor = AgroGreen100
                )
                Column(modifier = Modifier.weight(1f).padding(horizontal = 11.dp)) {
                    Text(
                        if (dashboard.isSyncing) "Sincronizando dados..." else if (isSynced) "Dados sincronizados" else "Dados protegidos no celular",
                        color = AgroGreen900,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        dashboard.syncFeedback ?: "Toque para conferir a nuvem · funciona offline",
                        color = PrimaryAgroGreen,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp
                    )
                }
                if (dashboard.isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Box(Modifier.size(8.dp).background(if (isSynced) StatusGreen else StatusOrange, androidx.compose.foundation.shape.CircleShape))
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("Minha propriedade")
            PrototypeCard {
                PropertyRow(Icons.Default.Agriculture, "Área total", "${producer?.areaTotalHectares ?: 0.0} hectares · ${dashboard.safrasAtivas.size} ${if (dashboard.safrasAtivas.size == 1) "talhão" else "talhões"}")
                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(start = 58.dp))
                PropertyRow(Icons.Default.Agriculture, "Culturas", cropsLabel.ifBlank { "Nenhuma cultura cadastrada" })
                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(start = 58.dp))
                PropertyRow(Icons.Default.LocationOn, "Localização", location)
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("Plano")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(AgroGreen900, Color(0xFF263C2B))))
                    .padding(16.dp)
            ) {
                Text(
                    "PLANO ATUAL",
                    color = Color(0xFFCFE8D0),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.background(Color.White.copy(alpha = .1f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text("AgroGestão Pro Beta", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 11.dp))
                Text("Todos os recursos disponíveis durante os testes", color = Color(0xFFCFE8D0), fontSize = 10.5.sp, modifier = Modifier.padding(top = 3.dp))
                Button(
                    onClick = {
                        infoDialog = "AgroGestão Pro Beta" to "Durante a fase beta, os recursos do aplicativo estão liberados para testes. Nenhuma cobrança será feita sem aviso e confirmação."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF49B64E)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 13.dp).height(42.dp)
                ) {
                    Text("Ver detalhes da versão beta", color = AgroGreen900, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("Dados e segurança")
            PrototypeCard {
                MenuListItem(Icons.Default.Edit, "Meus dados", "Nome, propriedade e documentos") { showEditProfile = true }
                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(start = 59.dp))
                MenuListItem(Icons.Default.Backup, "Cópia de segurança", "Salvar ou restaurar um backup protegido") { onNavigateToBackup() }
                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(start = 59.dp))
                MenuListItem(Icons.Default.Lock, "Privacidade e LGPD", "Consentimento e proteção dos seus dados") {
                    infoDialog = "Privacidade e LGPD" to "Seus dados agrícolas são usados somente para as funções do AgroGestão Pro. Relatórios para terceiros exigem autorização antes de serem gerados ou compartilhados."
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("Ferramentas")
            PrototypeCard {
                MenuListItem(Icons.AutoMirrored.Filled.Assignment, "Lembretes de tarefas", "Avisos que funcionam mesmo sem internet") { onNavigateToTasks() }
                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(start = 59.dp))
                MenuListItem(Icons.Default.Description, "Relatórios e consentimento", "PDFs, histórico e crédito rural") { onNavigateToReports() }
                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(start = 59.dp))
                MenuListItem(Icons.Default.Info, "Sobre o AgroGestão Pro", "Versão ${BuildConfig.VERSION_NAME}") {
                    infoDialog = "Sobre o AgroGestão Pro" to "Versão ${BuildConfig.VERSION_NAME}. Aplicativo offline-first para organização de tarefas, safras, custos e relatórios da propriedade."
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showLogout = true }.padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, tint = StatusOrange, modifier = Modifier.size(18.dp))
                Text("Sair da conta", color = StatusOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                "AgroGestão Pro · ${BuildConfig.VERSION_NAME}",
                color = TextMuted,
                fontSize = 10.5.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 28.dp)
            )
        }
    }

    if (showEditProfile) {
        EditProfileDialog(
            current = report.producer,
            onDismiss = { showEditProfile = false },
            onSave = { nome, propriedade, municipio, caf, area ->
                reportViewModel.updateProducerInfo(nome, propriedade, municipio, caf, area)
                showEditProfile = false
            }
        )
    }
    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            title = { Text("Sair da conta?", fontWeight = FontWeight.Bold) },
            text = { Text("Os dados sincronizados continuarão na nuvem e os dados locais permanecerão separados com segurança.") },
            confirmButton = {
                Button(
                    onClick = { showLogout = false; dashboardViewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOrange)
                ) { Text("Sair") }
            },
            dismissButton = { TextButton(onClick = { showLogout = false }) { Text("Cancelar") } }
        )
    }
    infoDialog?.let { (title, message) ->
        AlertDialog(
            onDismissRequest = { infoDialog = null },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { infoDialog = null }) { Text("Fechar") } }
        )
    }
}

@Composable
private fun PropertyRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        SmallIconTile(icon = icon, backgroundColor = Color(0xFFF7F9F7))
        Column(modifier = Modifier.padding(start = 11.dp)) {
            Text(label, color = TextMuted, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = TextDark, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}
