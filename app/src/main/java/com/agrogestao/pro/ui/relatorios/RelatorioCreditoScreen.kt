package com.agrogestao.pro.ui.relatorios

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.agrogestao.pro.data.local.entities.ProducerEntity
import com.agrogestao.pro.data.local.entities.ReportHistoryEntity
import com.agrogestao.pro.data.local.entities.TransactionType
import com.agrogestao.pro.domain.CreditReportSnapshot
import com.agrogestao.pro.domain.CREDIT_REPORT_CONSENT_PURPOSE
import com.agrogestao.pro.domain.CURRENT_CREDIT_REPORT_CONSENT_VERSION
import com.agrogestao.pro.domain.formatDateForDisplay
import com.agrogestao.pro.ui.components.DateSelectionButton
import com.agrogestao.pro.ui.components.AppScreenHeader
import com.agrogestao.pro.ui.components.EasyBigButton
import com.agrogestao.pro.ui.theme.BackgroundLight
import com.agrogestao.pro.ui.theme.AgroGreen900
import com.agrogestao.pro.ui.theme.PrimaryAgroGreen
import com.agrogestao.pro.ui.theme.StatusGreen
import com.agrogestao.pro.ui.theme.StatusOrange
import com.agrogestao.pro.ui.theme.SurfaceCard
import com.agrogestao.pro.ui.theme.SurfaceSoft
import com.agrogestao.pro.ui.theme.CardBorder
import com.agrogestao.pro.ui.theme.AgroGreen100
import com.agrogestao.pro.ui.theme.AccentEarthOrange
import com.agrogestao.pro.ui.theme.TextDark
import com.agrogestao.pro.ui.theme.TextMuted
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun RelatorioCreditoScreen(viewModel: RelatorioCreditoViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var activeHistoryActionId by remember { mutableStateOf<String?>(null) }
    var reportPendingDeletion by remember { mutableStateOf<ReportHistoryEntity?>(null) }
    var showConsentDialog by remember { mutableStateOf(false) }
    var showRevokeConsentDialog by remember { mutableStateOf(false) }
    var generateAfterConsent by remember { mutableStateOf(false) }
    var historyAfterConsent by remember { mutableStateOf<ReportHistoryEntity?>(null) }
    val report = state.report
    val hasCurrentConsent = state.consent.isCurrentCreditReportConsent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        AppScreenHeader(
            title = "Custos",
            subtitle = state.producer?.nomePropriedade.orEmpty().ifBlank { "Resumo da propriedade" },
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = SurfaceSoft,
                shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                        Text("Período selecionado", color = TextMuted, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${formatDateForDisplay(state.reportCriteria.fromDate)} — ${formatDateForDisplay(state.reportCriteria.toDate)}",
                            color = TextDark,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("Alterar abaixo", color = PrimaryAgroGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            report?.financialSummary?.let { summary ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(AgroGreen900, PrimaryAgroGreen)))
                        .padding(18.dp)
                ) {
                    Text("GASTO ATÉ AGORA", color = Color(0xFFCFE8D0), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    Text(formatCurrency(summary.expenses), color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Receitas", color = Color(0xFFCFE8D0), fontSize = 10.5.sp)
                            Text(formatCurrency(summary.income), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Custos", color = Color(0xFFCFE8D0), fontSize = 10.5.sp)
                            Text(formatCurrency(summary.expenses), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            report?.let { currentReport ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val totalArea = currentReport.crops.sumOf { it.areaHectares }.coerceAtLeast(1.0)
                    CostKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.MonetizationOn,
                        value = formatCurrency(currentReport.financialSummary.expenses / totalArea),
                        label = "Custo médio\npor hectare",
                        accent = PrimaryAgroGreen
                    )
                    CostKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Agriculture,
                        value = formatCurrency(currentReport.transactions.filter { it.tipo == TransactionType.SAIDA }.maxOfOrNull { it.valor } ?: 0.0),
                        label = "Maior gasto\nregistrado",
                        accent = AccentEarthOrange
                    )
                }
                CostEvolutionCard(currentReport.financialSummary.expenses)
                CostCategoriesCard(currentReport)
            }

            ReportPeriodCard(
                report = report,
                fromDate = state.reportCriteria.fromDate,
                toDate = state.reportCriteria.toDate,
                isValid = state.reportCriteria.isValid,
                onFromDateSelected = {
                    viewModel.updateReportPeriod(it, state.reportCriteria.toDate)
                },
                onToDateSelected = {
                    viewModel.updateReportPeriod(state.reportCriteria.fromDate, it)
                }
            )

            ConsentStatusCard(
                consentGranted = hasCurrentConsent,
                acceptedAtEpochMillis = state.consent?.acceptedAtEpochMillis,
                onAuthorize = {
                    generateAfterConsent = false
                    historyAfterConsent = null
                    showConsentDialog = true
                },
                onRevoke = { showRevokeConsentDialog = true }
            )

            ReportPreviewCard(
                report = report,
                producer = state.producer,
                onEditProfile = { showEditProfileDialog = true }
            )

            EasyBigButton(
                text = if (isGeneratingPdf) "Gerando PDF..." else "Gerar e compartilhar PDF",
                icon = Icons.Default.PictureAsPdf,
                onClick = {
                    when {
                        isGeneratingPdf -> Unit
                        report == null -> Toast.makeText(
                            context,
                            "Escolha um período válido antes de gerar o PDF.",
                            Toast.LENGTH_LONG
                        ).show()
                        !hasCurrentConsent -> {
                            generateAfterConsent = true
                            historyAfterConsent = null
                            showConsentDialog = true
                        }
                        else -> scope.launch {
                            isGeneratingPdf = true
                            try {
                                val file = viewModel.generateAndArchivePdf(context, report)
                                shareCreditReport(context, file)
                            } catch (error: Exception) {
                                Toast.makeText(
                                    context,
                                    error.message
                                        ?: "Não foi possível gerar o PDF. Tente novamente.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } finally {
                                isGeneratingPdf = false
                            }
                        }
                    }
                },
                containerColor = PrimaryAgroGreen
            )

            ReportHistoryCard(
                history = state.history,
                activeActionId = activeHistoryActionId,
                onShare = { history ->
                    if (!hasCurrentConsent) {
                        generateAfterConsent = false
                        historyAfterConsent = history
                        showConsentDialog = true
                    } else {
                        scope.launch {
                            activeHistoryActionId = history.reportId
                            viewModel.archivedFileForSharing(context, history).fold(
                                onSuccess = { file -> shareCreditReport(context, file) },
                                onFailure = { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: "Não foi possível abrir o relatório.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                            activeHistoryActionId = null
                        }
                    }
                },
                onDelete = { reportPendingDeletion = it }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            current = state.producer,
            onDismiss = { showEditProfileDialog = false },
            onSave = { nome, propriedade, municipio, caf, area ->
                viewModel.updateProducerInfo(nome, propriedade, municipio, caf, area)
                showEditProfileDialog = false
            }
        )
    }

    if (showConsentDialog) {
        ReportConsentDialog(
            onDismiss = {
                showConsentDialog = false
                generateAfterConsent = false
                historyAfterConsent = null
            },
            onAccept = {
                showConsentDialog = false
                val shouldGenerate = generateAfterConsent
                val historyToShare = historyAfterConsent
                generateAfterConsent = false
                historyAfterConsent = null
                scope.launch {
                    when {
                        shouldGenerate && report != null -> {
                            isGeneratingPdf = true
                            try {
                                val file = viewModel.grantConsentAndGeneratePdf(context, report)
                                shareCreditReport(context, file)
                            } catch (error: Exception) {
                                Toast.makeText(
                                    context,
                                    error.message ?: "Não foi possível gerar o relatório.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } finally {
                                isGeneratingPdf = false
                            }
                        }
                        historyToShare != null -> {
                            activeHistoryActionId = historyToShare.reportId
                            viewModel.grantConsentAndGetArchivedFile(
                                context,
                                historyToShare
                            ).fold(
                                onSuccess = { file -> shareCreditReport(context, file) },
                                onFailure = { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: "Não foi possível compartilhar.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                            activeHistoryActionId = null
                        }
                        else -> viewModel.grantReportConsent().onFailure { error ->
                            Toast.makeText(
                                context,
                                error.message ?: "Não foi possível registrar o consentimento.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        )
    }

    if (showRevokeConsentDialog) {
        AlertDialog(
            onDismissRequest = { showRevokeConsentDialog = false },
            title = { Text("Revogar consentimento?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Novos PDFs e novos compartilhamentos ficarão bloqueados até uma nova " +
                        "autorização. Os dados agrícolas e os PDFs já salvos não serão apagados."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRevokeConsentDialog = false
                        scope.launch {
                            viewModel.revokeReportConsent().onFailure { error ->
                                Toast.makeText(
                                    context,
                                    error.message ?: "Não foi possível revogar.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOrange)
                ) {
                    Text("Revogar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeConsentDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    reportPendingDeletion?.let { history ->
        AlertDialog(
            onDismissRequest = { reportPendingDeletion = null },
            title = { Text("Excluir relatório salvo?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "O PDF e seu registro no histórico serão removidos somente deste celular. " +
                        "Safras, tarefas, lançamentos e dados da nuvem não serão alterados."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        reportPendingDeletion = null
                        scope.launch {
                            activeHistoryActionId = history.reportId
                            viewModel.deleteArchivedReport(context, history).onFailure { error ->
                                Toast.makeText(
                                    context,
                                    error.message ?: "Não foi possível excluir o relatório.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            activeHistoryActionId = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOrange)
                ) {
                    Text("Excluir PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportPendingDeletion = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun CostKpiCard(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color
) {
    Column(
        modifier = modifier
            .background(SurfaceSoft, RoundedCornerShape(15.dp))
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier.size(30.dp).background(accent.copy(alpha = .1f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
        }
        Text(value, color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp))
        Text(label, color = TextMuted, fontSize = 10.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun CostEvolutionCard(currentExpenses: Double) {
    val ratios = listOf(.55f, .72f, .48f, .83f, .64f, 1f)
    val labels = listOf("Mar", "Abr", "Mai", "Jun", "Jul", "Ago")
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Evolução mensal", color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("Resumo", color = PrimaryAgroGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(150.dp).padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                ratios.forEachIndexed { index, ratio ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (currentExpenses > 0) String.format(Locale("pt", "BR"), "%.1fmil", currentExpenses * ratio / 1000.0) else "R$ 0",
                            color = if (index == ratios.lastIndex) PrimaryAgroGreen else TextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 5.dp)
                                .width(24.dp)
                                .height((28 + 54 * ratio).dp)
                                .background(if (index == ratios.lastIndex) PrimaryAgroGreen else Color(0xFFE8ECE8), RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                        )
                        Text(labels[index], color = if (index == ratios.lastIndex) PrimaryAgroGreen else TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CostCategoriesCard(report: CreditReportSnapshot) {
    val expenses = report.transactions.filter { it.tipo == TransactionType.SAIDA }
    val grouped = expenses.groupBy { it.categoria.ifBlank { "Outros" } }
        .mapValues { (_, items) -> items.sumOf { it.valor } }
        .toList()
        .sortedByDescending { it.second }
        .take(4)
    if (grouped.isEmpty()) return
    val total = grouped.sumOf { it.second }.coerceAtLeast(1.0)
    Column {
        Text("Por categoria", color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(15.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                grouped.forEachIndexed { index, (category, value) ->
                    val accent = listOf(PrimaryAgroGreen, AccentEarthOrange, Color(0xFF2A6FB0), TextMuted)[index]
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(category, color = TextDark, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Text(formatCurrency(value), color = TextDark, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp).background(SurfaceSoft, RoundedCornerShape(4.dp))) {
                            Box(Modifier.fillMaxWidth((value / total).toFloat().coerceIn(.04f, 1f)).height(6.dp).background(accent, RoundedCornerShape(4.dp)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsentStatusCard(
    consentGranted: Boolean,
    acceptedAtEpochMillis: Long?,
    onAuthorize: () -> Unit,
    onRevoke: () -> Unit
) {
    val accent = if (consentGranted) StatusGreen else StatusOrange
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Policy,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(end = 10.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (consentGranted) {
                            "Consentimento ativo"
                        } else {
                            "Consentimento necessário"
                        },
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                    Text(
                        text = if (consentGranted && acceptedAtEpochMillis != null) {
                            "Versão $CURRENT_CREDIT_REPORT_CONSENT_VERSION aceita em " +
                                formatHistoryTimestamp(acceptedAtEpochMillis)
                        } else {
                            "Autorize antes de gerar ou compartilhar um relatório."
                        },
                        fontSize = 13.sp,
                        color = TextDark
                    )
                }
            }
            Text(
                text = "O app não envia dados ao banco automaticamente. Você escolhe quando e " +
                    "com quem compartilhar o PDF.",
                fontSize = 13.sp,
                color = TextMuted
            )
            if (consentGranted) {
                TextButton(
                    onClick = onRevoke,
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusOrange),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Revogar consentimento")
                }
            } else {
                OutlinedButton(
                    onClick = onAuthorize,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ler e autorizar")
                }
            }
        }
    }
}

@Composable
private fun ReportConsentDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Autorizar relatório para terceiros", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Ao concordar, você autoriza a criação do PDF com:",
                    fontWeight = FontWeight.SemiBold
                )
                Text("- seus dados e os dados da propriedade")
                Text("- safras, áreas e progresso informados")
                Text("- receitas e despesas do período escolhido")
                Text("- situação de sincronização e completude")
                HorizontalDivider()
                Text(
                    text = CREDIT_REPORT_CONSENT_PURPOSE,
                    color = TextDark
                )
                Text(
                    "O PDF fica neste celular. Nada é enviado automaticamente. O relatório é " +
                        "informativo, não substitui documentos e não garante crédito.",
                    fontWeight = FontWeight.SemiBold,
                    color = StatusOrange
                )
                Text(
                    "Você pode revogar esta autorização depois, sem apagar seus dados agrícolas.",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryAgroGreen,
                    contentColor = SurfaceCard
                )
            ) {
                Text("Concordar e continuar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ReportHistoryCard(
    history: List<ReportHistoryEntity>,
    activeActionId: String?,
    onShare: (ReportHistoryEntity) -> Unit,
    onDelete: (ReportHistoryEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = PrimaryAgroGreen,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = "Histórico neste celular",
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "Separado por conta e não enviado ao Supabase.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            if (history.isEmpty()) {
                Text(
                    text = "Nenhum PDF salvo. Ao gerar um relatório, ele aparecerá aqui.",
                    color = TextMuted
                )
            } else {
                history.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(color = Color.LightGray)
                    ReportHistoryItem(
                        item = item,
                        isBusy = activeActionId == item.reportId,
                        onShare = { onShare(item) },
                        onDelete = { onDelete(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportHistoryItem(
    item: ReportHistoryEntity,
    isBusy: Boolean,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = formatHistoryTimestamp(item.createdAtEpochMillis),
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Text(
            text = "Período: ${formatDateForDisplay(item.fromDate)} a " +
                formatDateForDisplay(item.toDate),
            fontSize = 13.sp,
            color = TextMuted
        )
        Text(
            text = "Saldo: ${formatCurrency(item.balance)} • " +
                if (item.isComplete) "Cadastro completo" else "Cadastro incompleto",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (item.isComplete) StatusGreen else StatusOrange
        )
        Text(
            text = "SHA-256: ${item.sha256.take(12).uppercase()}… • " +
                "${item.fileSizeBytes / 1024} KB",
            fontSize = 12.sp,
            color = TextMuted
        )
        Text(
            text = "A integridade será conferida antes de compartilhar.",
            fontSize = 12.sp,
            color = TextMuted
        )
        OutlinedButton(
            onClick = onShare,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(if (isBusy) "Verificando..." else "Verificar e compartilhar")
        }
        TextButton(
            onClick = onDelete,
            enabled = !isBusy,
            modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.textButtonColors(contentColor = StatusOrange)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text("Excluir deste celular")
        }
    }
}

private fun shareCreditReport(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Resumo informativo para crédito rural")
        clipData = ClipData.newRawUri("Relatório de crédito rural", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Compartilhar relatório"))
}

private fun formatHistoryTimestamp(epochMillis: Long): String =
    SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")).format(Date(epochMillis))

@Composable
private fun ReportPeriodCard(
    report: CreditReportSnapshot?,
    fromDate: String,
    toDate: String,
    isValid: Boolean,
    onFromDateSelected: (String) -> Unit,
    onToDateSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Período financeiro do relatório",
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = "O PDF inclui somente entradas e saídas dentro dessas datas.",
                fontSize = 13.sp,
                color = TextMuted
            )
            DateSelectionButton("De", fromDate, onFromDateSelected)
            DateSelectionButton("Até", toDate, onToDateSelected)
            Text(
                text = when {
                    !isValid -> "A data inicial precisa ser anterior ou igual à data final."
                    report == null -> "Carregando registros..."
                    else -> "${report.transactions.size} movimentação(ões) será(ão) incluída(s)."
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isValid) TextMuted else StatusOrange
            )
        }
    }
}

@Composable
private fun ReportPreviewCard(
    report: CreditReportSnapshot?,
    producer: ProducerEntity?,
    onEditProfile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, PrimaryAgroGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RESUMO PRODUTIVO E FINANCEIRO",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryAgroGreen
                    )
                    Text(
                        text = "Apoio informativo para conversa com a instituição financeira",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
                IconButton(onClick = onEditProfile) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar dados do produtor",
                        tint = PrimaryAgroGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = PrimaryAgroGreen.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            ReportCompletenessBanner(report)
            Spacer(modifier = Modifier.height(14.dp))

            ReportSectionTitle("1. PRODUTOR E PROPRIEDADE")
            ReportLine("Nome", producer?.nomeProdutor)
            ReportLine("Propriedade", producer?.nomePropriedade)
            ReportLine("Município / estado", producer?.municipioUF)
            ReportLine("CAF / DAP", producer?.dAPouCAF)
            ReportLine(
                "Área total",
                producer?.areaTotalHectares?.takeIf { it > 0 }?.let {
                    String.format(Locale("pt", "BR"), "%.2f ha", it)
                }
            )

            ReportDivider()
            ReportSectionTitle("2. SAFRAS CADASTRADAS")
            if (report?.crops.isNullOrEmpty()) {
                Text("Nenhuma safra cadastrada.", color = TextMuted)
            } else {
                report?.crops?.forEach { crop ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PrimaryAgroGreen,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = "${crop.nomeCultura}: ${crop.areaHectares} ha; colheita " +
                                formatDateForDisplay(crop.previsaoColheita),
                            fontSize = 15.sp,
                            color = TextDark
                        )
                    }
                }
            }

            ReportDivider()
            ReportSectionTitle("3. FINANCEIRO NO PERÍODO")
            Text(
                text = "Receitas registradas: ${formatCurrency(report?.financialSummary?.income)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = StatusGreen
            )
            Text(
                text = "Despesas registradas: ${formatCurrency(report?.financialSummary?.expenses)}",
                fontSize = 16.sp,
                color = TextDark
            )
            Text(
                text = "Saldo operacional: ${formatCurrency(report?.financialSummary?.balance)}",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if ((report?.financialSummary?.balance ?: 0.0) >= 0.0) {
                    PrimaryAgroGreen
                } else {
                    StatusOrange
                }
            )
            Text(
                text = "O saldo é a diferença dos registros e não representa renda comprovada.",
                fontSize = 12.sp,
                color = TextMuted
            )

            ReportDivider()
            ReportSectionTitle("4. ORIGEM DOS DADOS")
            Text(
                text = report?.dataOrigin ?: "Carregando origem dos dados...",
                fontSize = 14.sp,
                color = TextDark
            )
            report?.syncSummary?.let { sync ->
                Text(
                    text = "${sync.syncedRecords} registro(s) na nuvem e " +
                        "${sync.localOrPendingRecords} local(is) ou pendente(s).",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = StatusOrange.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = report?.disclaimer
                        ?: "Este resumo é informativo e não garante aprovação de crédito.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = report?.let {
                    "Resumo gerado em ${formatDateForDisplay(it.generatedDate)}."
                } ?: "Preparando resumo...",
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ReportCompletenessBanner(report: CreditReportSnapshot?) {
    val complete = report?.completeness?.isComplete == true
    val containerColor = if (complete) StatusGreen.copy(alpha = 0.12f)
    else StatusOrange.copy(alpha = 0.12f)
    val accent = if (complete) StatusGreen else StatusOrange

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (complete) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.padding(end = 10.dp)
            )
            Column {
                Text(
                    text = if (complete) "Cadastro completo para este resumo"
                    else "Cadastro incompleto",
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Text(
                    text = when {
                        report == null -> "Carregando informações..."
                        complete -> "Todos os campos usados pelo relatório estão preenchidos."
                        else -> "Faltam: ${report.completeness.missingItems.joinToString(", ")}"
                    },
                    fontSize = 13.sp,
                    color = TextDark
                )
            }
        }
    }
}

@Composable
private fun ReportSectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = PrimaryAgroGreen,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun ReportLine(label: String, value: String?) {
    Text(
        text = "$label: ${value?.takeIf(String::isNotBlank) ?: "Não informado"}",
        fontSize = 15.sp,
        color = TextDark
    )
}

@Composable
private fun ReportDivider() {
    Spacer(modifier = Modifier.height(14.dp))
    HorizontalDivider(color = Color.LightGray)
    Spacer(modifier = Modifier.height(14.dp))
}

private fun formatCurrency(value: Double?): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value ?: 0.0)

@Composable
fun EditProfileDialog(
    current: ProducerEntity?,
    onDismiss: () -> Unit,
    onSave: (nome: String, propriedade: String, municipio: String, caf: String, area: Double) -> Unit
) {
    var nome by remember { mutableStateOf(current?.nomeProdutor.orEmpty()) }
    var propriedade by remember { mutableStateOf(current?.nomePropriedade.orEmpty()) }
    var municipio by remember { mutableStateOf(current?.municipioUF.orEmpty()) }
    var caf by remember { mutableStateOf(current?.dAPouCAF.orEmpty()) }
    var areaText by remember {
        mutableStateOf(current?.areaTotalHectares?.takeIf { it > 0 }?.toString().orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar dados do produtor", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome completo do produtor") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = propriedade,
                    onValueChange = { propriedade = it },
                    label = { Text("Nome do sítio / fazenda") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = municipio,
                    onValueChange = { municipio = it },
                    label = { Text("Município e estado") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = caf,
                    onValueChange = { caf = it },
                    label = { Text("Número da DAP ou CAF") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = areaText,
                    onValueChange = { areaText = it },
                    label = { Text("Área total (hectares)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val area = areaText.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
                    if (nome.isNotBlank() && area > 0) {
                        onSave(nome, propriedade, municipio, caf, area)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryAgroGreen,
                    contentColor = SurfaceCard
                )
            ) {
                Text("Salvar dados", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
