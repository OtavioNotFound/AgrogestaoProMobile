package com.agrogestao.pro.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogestao.pro.data.backup.AgroBackupCodec
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.data.local.entities.TaskStatus
import com.agrogestao.pro.data.remote.SupabaseConfig
import com.agrogestao.pro.domain.formatDateForDisplay
import com.agrogestao.pro.domain.todayIso
import com.agrogestao.pro.ui.components.AgroSectionHeader
import com.agrogestao.pro.ui.components.PrototypeCard
import com.agrogestao.pro.ui.components.SmallIconTile
import com.agrogestao.pro.ui.theme.AccentEarthOrange
import com.agrogestao.pro.ui.theme.AgroAmber100
import com.agrogestao.pro.ui.theme.AgroAmberBorder
import com.agrogestao.pro.ui.theme.AgroGreen050
import com.agrogestao.pro.ui.theme.AgroGreen100
import com.agrogestao.pro.ui.theme.AgroGreen900
import com.agrogestao.pro.ui.theme.CardBorder
import com.agrogestao.pro.ui.theme.PrimaryAgroGreen
import com.agrogestao.pro.ui.theme.StatusGreen
import com.agrogestao.pro.ui.theme.StatusOrange
import com.agrogestao.pro.ui.theme.SurfaceCard
import com.agrogestao.pro.ui.theme.SurfaceSoft
import com.agrogestao.pro.ui.theme.TextDark
import com.agrogestao.pro.ui.theme.TextMuted
import com.agrogestao.pro.ui.theme.TextSecondary
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    simpleMode: Boolean = false,
    openBackupRequested: Boolean = false,
    onBackupRequestHandled: () -> Unit = {},
    onNavigateToTasks: () -> Unit,
    onNavigateToSafras: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var backupDialog by remember { mutableStateOf<BackupDialogMode?>(null) }
    var backupPassword by remember { mutableStateOf("") }
    var backupConfirmation by remember { mutableStateOf("") }
    var backupDialogError by remember { mutableStateOf<String?>(null) }
    var pendingBackupContent by remember { mutableStateOf<String?>(null) }
    var pendingRestorePassword by remember { mutableStateOf("") }

    LaunchedEffect(openBackupRequested) {
        if (openBackupRequested) {
            backupDialog = BackupDialogMode.EXPORT
            backupPassword = ""
            backupConfirmation = ""
            backupDialogError = null
            onBackupRequestHandled()
        }
    }

    val createBackupFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(AgroBackupCodec.MIME_TYPE)
    ) { uri ->
        val content = pendingBackupContent
        pendingBackupContent = null
        if (uri == null || content == null) {
            viewModel.reportBackupError("Salvamento do backup cancelado.")
        } else {
            scope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "wt")
                            ?.bufferedWriter()?.use { it.write(content) }
                            ?: throw IOException("O Android não permitiu gravar o arquivo.")
                    }
                }
                viewModel.reportBackupFileSaved(result.isSuccess, result.exceptionOrNull()?.message)
            }
        }
    }
    val openBackupFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val password = pendingRestorePassword
        pendingRestorePassword = ""
        if (uri == null) {
            viewModel.reportBackupError("Restauração do backup cancelada.")
        } else {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use(::readBackupWithLimit)
                            ?: throw IOException("O Android não permitiu abrir o arquivo.")
                    }
                }.fold(
                    onSuccess = { viewModel.restoreBackup(it, password) },
                    onFailure = { viewModel.reportBackupError(it.message ?: "Não foi possível ler o arquivo selecionado.") }
                )
            }
        }
    }

    val producerName = state.producer?.nomeProdutor.orEmpty().ifBlank { "Produtor" }
    val firstName = producerName.trim().substringBefore(" ").ifBlank { "Produtor" }
    val farmName = state.producer?.nomePropriedade.orEmpty().ifBlank { "Minha propriedade" }
    val location = state.producer?.municipioUF.orEmpty().ifBlank { "Localização não informada" }
    val synced = state.producer?.syncStatus == SupabaseConfig.STATUS_SYNCED_CLOUD

    Column(modifier = Modifier.fillMaxSize().background(SurfaceCard)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(7.dp).background(if (synced) StatusGreen else AccentEarthOrange, CircleShape)
                    )
                    Text(
                        farmName,
                        color = TextDark,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = viewModel::syncNow,
                        enabled = !state.isSyncing,
                        modifier = Modifier.size(40.dp).background(SurfaceSoft, RoundedCornerShape(12.dp))
                    ) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Sync, "Sincronizar", tint = TextSecondary, modifier = Modifier.size(19.dp))
                        }
                    }
                }
                Text(
                    "Bom dia, $firstName 👋",
                    color = TextDark,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Text(
                    if (simpleMode) "Veja o que precisa de atenção hoje" else "Acompanhe sua propriedade hoje",
                    color = TextMuted,
                    fontSize = 12.5.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoChip(Icons.Default.LocationOn, location)
                    InfoChip(
                        if (synced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        if (synced) "Nuvem atualizada" else "Salvo no celular"
                    )
                }
            }

            DashboardSection {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    DashboardKpi(Modifier.weight(1f), Icons.Default.Agriculture, state.safrasAtivas.size.toString(), "Talhões\nativos")
                    DashboardKpi(Modifier.weight(1f), Icons.AutoMirrored.Filled.Assignment, state.tarefasPendentes.size.toString(), "Tarefas\npendentes")
                    DashboardKpi(Modifier.weight(1f), Icons.AutoMirrored.Filled.TrendingUp, currencyShort(state.saldoTotal), "Saldo\natual")
                }
                if (state.tarefasPendentes.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .clickable(onClick = onNavigateToTasks)
                            .background(AgroAmber100, RoundedCornerShape(14.dp))
                            .padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallIconTile(Icons.Default.Warning, AccentEarthOrange, Color(0xFFFBE3B0), 30)
                        Text(
                            "Você tem ${state.tarefasPendentes.size} ${if (state.tarefasPendentes.size == 1) "tarefa pendente" else "tarefas pendentes"} para acompanhar.",
                            color = Color(0xFF7A4E10),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                }
            }

            DashboardSection {
                AgroSectionHeader(if (simpleMode) "O que fazer hoje" else "Tarefas de hoje", "Ver todas", onNavigateToTasks)
                Spacer(Modifier.height(13.dp))
                PrototypeCard {
                    if (state.tarefasPendentes.isEmpty()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = StatusGreen)
                            Text("Manejo em dia. Nenhuma tarefa pendente.", color = TextSecondary, fontSize = 12.5.sp, modifier = Modifier.padding(start = 9.dp))
                        }
                    } else {
                        val todo = state.tarefasPendentes.filter { it.status == TaskStatus.A_FAZER }
                        val doing = state.tarefasPendentes.filter { it.status == TaskStatus.EM_PROGRESSO }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp).clickable(onClick = onNavigateToTasks),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DashboardTaskColumn(Modifier.weight(1f), "A FAZER", Color(0xFFB9C0BA), todo.size, todo.firstOrNull())
                            DashboardTaskColumn(Modifier.weight(1f), "FAZENDO", Color(0xFFF5B947), doing.size, doing.firstOrNull())
                            DashboardTaskColumn(Modifier.weight(1f), "FEITO", StatusGreen, 0, null)
                        }
                    }
                }
            }

            DashboardSection {
                AgroSectionHeader("Meus talhões", "Ver todos", onNavigateToSafras)
                Spacer(Modifier.height(13.dp))
                if (state.safrasAtivas.isEmpty()) {
                    PrototypeCard {
                        Text("Cadastre sua primeira safra para acompanhar a produção.", color = TextMuted, fontSize = 12.5.sp, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        state.safrasAtivas.take(6).forEachIndexed { index, crop ->
                            CropPreview(crop, index, onNavigateToSafras)
                        }
                    }
                }
            }

            DashboardSection {
                AgroSectionHeader("Custos do período")
                Spacer(Modifier.height(13.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(AgroGreen900, PrimaryAgroGreen)))
                        .padding(18.dp)
                ) {
                    Text("SALDO OPERACIONAL", color = Color(0xFFCFE8D0), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Text(currency(state.saldoTotal), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
                    Row(modifier = Modifier.padding(top = 14.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Receitas", color = Color(0xFFCFE8D0), fontSize = 10.5.sp)
                            Text(currency(state.totalEntradas), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Custos", color = Color(0xFFCFE8D0), fontSize = 10.5.sp)
                            Text(currency(state.totalSaidas), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            DashboardSection {
                AgroSectionHeader("Mais ferramentas")
                Spacer(Modifier.height(13.dp))
                PrototypeCard {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        SmallIconTile(Icons.Default.Lock)
                        Column(modifier = Modifier.padding(start = 11.dp)) {
                            Text("Cópia de segurança", color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Protegida por senha e separada por conta", color = TextMuted, fontSize = 10.5.sp)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        OutlinedButton(
                            onClick = { backupDialog = BackupDialogMode.EXPORT; backupPassword = ""; backupConfirmation = ""; backupDialogError = null },
                            enabled = !state.isBackupBusy,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(11.dp)
                        ) { Icon(Icons.Default.Download, null, modifier = Modifier.size(17.dp)); Text("Salvar", modifier = Modifier.padding(start = 6.dp), fontSize = 12.sp) }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { backupDialog = BackupDialogMode.RESTORE; backupPassword = ""; backupConfirmation = ""; backupDialogError = null },
                            enabled = !state.isBackupBusy,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(11.dp)
                        ) { Icon(Icons.Default.Upload, null, modifier = Modifier.size(17.dp)); Text("Restaurar", modifier = Modifier.padding(start = 6.dp), fontSize = 12.sp) }
                    }
                    if (state.isBackupBusy) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Conferindo o backup...", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 9.dp))
                        }
                    }
                    state.backupFeedback?.let {
                        Text(it, color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    backupDialog?.let { mode ->
        AlertDialog(
            onDismissRequest = { if (!state.isBackupBusy) backupDialog = null },
            title = { Text(if (mode == BackupDialogMode.EXPORT) "Proteger o backup" else "Abrir o backup") },
            text = {
                Column {
                    Text(if (mode == BackupDialogMode.EXPORT) "Crie uma senha com pelo menos 8 caracteres. Ela não poderá ser recuperada." else "Digite a senha usada ao salvar. A restauração combina os dados e não apaga os demais registros.")
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it; backupDialogError = null },
                        label = { Text("Senha do backup") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    )
                    if (mode == BackupDialogMode.EXPORT) {
                        OutlinedTextField(
                            value = backupConfirmation,
                            onValueChange = { backupConfirmation = it; backupDialogError = null },
                            label = { Text("Repita a senha") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                    backupDialogError?.let { Text(it, color = StatusOrange, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
                }
            },
            confirmButton = {
                Button(
                    enabled = !state.isBackupBusy,
                    onClick = {
                        when {
                            backupPassword.length < AgroBackupCodec.MIN_PASSWORD_LENGTH -> backupDialogError = "Use pelo menos 8 caracteres."
                            mode == BackupDialogMode.EXPORT && backupPassword != backupConfirmation -> backupDialogError = "As senhas digitadas são diferentes."
                            mode == BackupDialogMode.EXPORT -> {
                                val password = backupPassword
                                backupDialog = null; backupPassword = ""; backupConfirmation = ""
                                viewModel.createBackup(password) { result ->
                                    result.fold(
                                        onSuccess = { pendingBackupContent = it; createBackupFile.launch("AgroGestao-${todayIso()}.${AgroBackupCodec.FILE_EXTENSION}") },
                                        onFailure = { viewModel.reportBackupError(it.message ?: "Não foi possível criar o backup.") }
                                    )
                                }
                            }
                            else -> {
                                pendingRestorePassword = backupPassword
                                backupDialog = null; backupPassword = ""
                                openBackupFile.launch(arrayOf(AgroBackupCodec.MIME_TYPE, "application/octet-stream", "text/plain"))
                            }
                        }
                    }
                ) { Text(if (mode == BackupDialogMode.EXPORT) "Criar arquivo" else "Escolher arquivo") }
            },
            dismissButton = { TextButton(onClick = { backupDialog = null }, enabled = !state.isBackupBusy) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun DashboardSection(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 11.dp), content = content)
}

@Composable
private fun InfoChip(icon: ImageVector, label: String) {
    Surface(color = SurfaceSoft, shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = TextMuted, modifier = Modifier.size(14.dp))
            Text(label, color = TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun DashboardKpi(modifier: Modifier, icon: ImageVector, value: String, label: String) {
    Column(modifier = modifier.background(SurfaceSoft, RoundedCornerShape(16.dp)).padding(12.dp)) {
        SmallIconTile(icon, size = 30)
        Text(value, color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, modifier = Modifier.padding(top = 11.dp))
        Text(label, color = TextMuted, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun DashboardTaskColumn(
    modifier: Modifier,
    label: String,
    color: Color,
    count: Int,
    task: TaskEntity?
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(7.dp).background(color, CircleShape))
            Text(label, color = TextMuted, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 5.dp))
            Text(count.toString(), color = TextMuted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 5.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(SurfaceSoft, RoundedCornerShape(10.dp))
                .padding(9.dp)
        ) {
            Text(
                task?.titulo ?: if (label == "FEITO") "Tudo em dia" else "Sem tarefas",
                color = if (task == null) TextMuted else TextDark,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                task?.categoria.orEmpty().ifBlank { "—" },
                color = color,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(top = 7.dp)
            )
        }
    }
}

@Composable
private fun CropPreview(crop: CropEntity, index: Int, onClick: () -> Unit) {
    val backgrounds = listOf(Color(0xFFC8E6C9), Color(0xFFD9ECD1), Color(0xFFF3E3B9), Color(0xFFCFE3F4))
    Column(
        modifier = Modifier.width(142.dp).clickable(onClick = onClick).background(SurfaceCard, RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(72.dp).background(backgrounds[index % backgrounds.size], RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            Icon(Icons.Default.Agriculture, null, tint = PrimaryAgroGreen.copy(alpha = .7f), modifier = Modifier.align(Alignment.Center).size(32.dp))
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(9.dp).background(if (crop.progressoPercentual >= 80) Color(0xFF2A6FB0) else StatusGreen, CircleShape))
        }
        Column(modifier = Modifier.fillMaxWidth().background(SurfaceCard).padding(11.dp)) {
            Text("Talhão ${index + 1}", color = TextDark, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${crop.nomeCultura} · ${crop.areaHectares} ha", color = TextMuted, fontSize = 10.5.sp, modifier = Modifier.padding(top = 2.dp))
            Text(crop.statusManejo.ifBlank { "Em acompanhamento" }, color = PrimaryAgroGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
        }
    }
}

private fun currency(value: Double): String = NumberFormatHolder.currency.format(value)
private fun currencyShort(value: Double): String = when {
    kotlin.math.abs(value) >= 1_000_000 -> String.format(Locale("pt", "BR"), "%.1f mi", value / 1_000_000)
    kotlin.math.abs(value) >= 1_000 -> String.format(Locale("pt", "BR"), "%.1f mil", value / 1_000)
    else -> String.format(Locale("pt", "BR"), "%.0f", value)
}

private object NumberFormatHolder {
    val currency = java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
}

private enum class BackupDialogMode { EXPORT, RESTORE }

private fun readBackupWithLimit(reader: java.io.BufferedReader): String {
    val output = StringBuilder()
    val buffer = CharArray(8_192)
    while (true) {
        val read = reader.read(buffer)
        if (read < 0) break
        if (output.length + read > AgroBackupCodec.MAX_FILE_CHARS) {
            throw IOException("O arquivo selecionado é grande demais para ser um backup válido.")
        }
        output.append(buffer, 0, read)
    }
    return output.toString()
}
