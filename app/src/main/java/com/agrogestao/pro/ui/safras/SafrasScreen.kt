package com.agrogestao.pro.ui.safras

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogestao.pro.data.local.entities.TransactionType
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.ui.components.CloudSyncStatusBadge
import com.agrogestao.pro.ui.components.AppScreenHeader
import com.agrogestao.pro.ui.components.ConfirmDeleteDialog
import com.agrogestao.pro.ui.components.DateSelectionButton
import com.agrogestao.pro.ui.components.CropSelectionField
import com.agrogestao.pro.ui.components.EasyBigButton
import com.agrogestao.pro.ui.components.EasyCard
import com.agrogestao.pro.ui.components.FilterChoice
import com.agrogestao.pro.ui.components.FilterSelectionField
import com.agrogestao.pro.ui.components.FiltersButton
import com.agrogestao.pro.ui.components.OptionalDateFilterField
import com.agrogestao.pro.ui.components.SimpleStatusBadge
import com.agrogestao.pro.ui.theme.AccentEarthOrange
import com.agrogestao.pro.ui.theme.BackgroundLight
import com.agrogestao.pro.ui.theme.PrimaryAgroGreen
import com.agrogestao.pro.ui.theme.StatusGreen
import com.agrogestao.pro.ui.theme.StatusOrange
import com.agrogestao.pro.ui.theme.TextDark
import com.agrogestao.pro.ui.theme.TextMuted
import com.agrogestao.pro.ui.theme.SurfaceCard
import com.agrogestao.pro.ui.theme.SurfaceSoft
import com.agrogestao.pro.ui.theme.CardBorder
import com.agrogestao.pro.domain.formatDateForDisplay
import com.agrogestao.pro.domain.FILTER_WITHOUT_CROP
import com.agrogestao.pro.domain.FinancialFilterCriteria
import com.agrogestao.pro.domain.todayIso
import com.agrogestao.pro.domain.todayPlusMonthsIso
import com.agrogestao.pro.domain.isIsoDateOnOrAfter
import com.agrogestao.pro.domain.parsePositiveDecimal
import com.agrogestao.pro.domain.parsePercentage
import java.util.Locale

@Composable
fun SafrasScreen(
    viewModel: SafrasViewModel,
    onBack: () -> Unit,
    simpleMode: Boolean = false,
    openFinanceRequested: Boolean = false,
    onFinanceRequestHandled: () -> Unit = {},
    hideFinancialValues: Boolean = false
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCropDialog by remember { mutableStateOf(false) }
    var showFinanceDialog by remember { mutableStateOf(false) }
    var cropBeingEdited by remember { mutableStateOf<CropEntity?>(null) }
    var transactionBeingEdited by remember { mutableStateOf<FinancialEntity?>(null) }
    var cropPendingDeletion by remember { mutableStateOf<Long?>(null) }
    var transactionPendingDeletion by remember { mutableStateOf<Long?>(null) }
    var showFinancialFilters by remember { mutableStateOf(false) }
    var cropSearch by remember { mutableStateOf("") }
    var cropStatusFilter by remember { mutableStateOf("Todos") }
    val visibleCrops = state.safras.filter { crop ->
        val matchesSearch = cropSearch.isBlank() ||
            crop.nomeCultura.contains(cropSearch, ignoreCase = true) ||
            crop.statusManejo.contains(cropSearch, ignoreCase = true)
        val matchesStatus = when (cropStatusFilter) {
            "Saudável" -> crop.statusManejo.contains("saud", ignoreCase = true)
            "Atenção" -> crop.statusManejo.contains("aten", ignoreCase = true)
            "Colheita" -> crop.statusManejo.contains("colhe", ignoreCase = true) || crop.progressoPercentual >= 80
            else -> true
        }
        matchesSearch && matchesStatus
    }

    LaunchedEffect(openFinanceRequested) {
        if (openFinanceRequested) {
            selectedTab = 1
            transactionBeingEdited = null
            showFinanceDialog = true
            onFinanceRequestHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        AppScreenHeader(
            title = if (selectedTab == 0) {
                if (simpleMode) "Meus terrenos" else "Meus talhões"
            } else {
                if (simpleMode) "Meu dinheiro" else "Caixa da propriedade"
            },
            subtitle = if (selectedTab == 0) {
                "${state.safras.size} ${if (state.safras.size == 1) (if (simpleMode) "terreno" else "talhão") else (if (simpleMode) "terrenos" else "talhões")} · ${state.safras.sumOf { it.areaHectares }} ha cultivados"
            } else {
                "${state.totalTransactions} lançamentos cadastrados"
            },
            onBack = if (selectedTab == 0) onBack else ({ selectedTab = 0 }),
            actionIcon = Icons.Default.Add,
            actionDescription = if (selectedTab == 0) {
                if (simpleMode) "Cadastrar terreno" else "Cadastrar safra"
            } else {
                if (simpleMode) "Registrar dinheiro" else "Registrar lançamento"
            },
            onAction = {
                if (selectedTab == 0) {
                    cropBeingEdited = null
                    showCropDialog = true
                } else {
                    transactionBeingEdited = null
                    showFinanceDialog = true
                }
            },
            primaryAction = true
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = PrimaryAgroGreen
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Agriculture, contentDescription = null, modifier = Modifier.height(18.dp).padding(end = 4.dp))
                        Text(text = if (simpleMode) "Terrenos" else "Talhões", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.height(18.dp).padding(end = 4.dp))
                        Text(text = if (simpleMode) "Dinheiro" else "Lançamentos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            if (selectedTab == 0) {
                OutlinedTextField(
                    value = cropSearch,
                    onValueChange = { cropSearch = it },
                    placeholder = { Text(if (simpleMode) "Buscar terreno ou cultura..." else "Buscar talhão ou cultura...", fontSize = 12.5.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    trailingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceSoft,
                        unfocusedContainerColor = SurfaceSoft,
                        focusedBorderColor = PrimaryAgroGreen,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 10.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Todos", "Saudável", "Atenção", "Colheita").forEach { label ->
                        PlotFilterChip(
                            label = label,
                            count = when (label) {
                                "Todos" -> state.safras.size
                                "Saudável" -> state.safras.count { it.statusManejo.contains("saud", true) }
                                "Atenção" -> state.safras.count { it.statusManejo.contains("aten", true) }
                                else -> state.safras.count { it.statusManejo.contains("colhe", true) || it.progressoPercentual >= 80 }
                            },
                            selected = cropStatusFilter == label,
                            onClick = { cropStatusFilter = label }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    PlotSummaryCard(Modifier.weight(1f), "${state.safras.sumOf { it.areaHectares }} ha", "Área total\ncultivada")
                    PlotSummaryCard(Modifier.weight(1f), state.safras.map { it.nomeCultura }.distinct().size.toString(), "Culturas\nativas")
                    PlotSummaryCard(
                        Modifier.weight(1f),
                        state.safras.count { it.statusManejo.contains("aten", true) }.toString(),
                        "Talhões em\natenção",
                        warning = true
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (simpleMode) "Todos os terrenos" else "Todos os talhões", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                    Text("Lista", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = PrimaryAgroGreen)
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(visibleCrops, key = { _, crop -> crop.id }) { index, crop ->
                        PlotListCard(
                            crop = crop,
                            index = index,
                            onEdit = { cropBeingEdited = crop; showCropDialog = true },
                            onDelete = { cropPendingDeletion = crop.id }
                        )
                    }
                }

            } else {
                FiltersButton(
                    activeCount = state.filters.activeCount,
                    onClick = { showFinancialFilters = true }
                )
                Text(
                    text = if (hideFinancialValues) {
                        "Mostrando ${state.transacoes.size} de ${state.totalTransactions} • Saldo filtrado: R$ ••••"
                    } else String.format(
                        Locale("pt", "BR"),
                        "Mostrando %d de %d • Saldo filtrado: R$ %.2f",
                        state.transacoes.size,
                        state.totalTransactions,
                        state.filteredBalance
                    ),
                    fontSize = 13.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.transacoes.isEmpty()) {
                        item {
                            Text(
                                text = if (state.filters.activeCount > 0) {
                                    "Nenhum lançamento corresponde aos filtros escolhidos."
                                } else {
                                    "Nenhum lançamento financeiro cadastrado."
                                },
                                fontSize = 16.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                    items(state.transacoes, key = { it.id }) { trans ->
                        EasyCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (trans.tipo == TransactionType.ENTRADA) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            tint = if (trans.tipo == TransactionType.ENTRADA) StatusGreen else StatusOrange,
                                            modifier = Modifier.height(18.dp).padding(end = 6.dp)
                                        )
                                        Text(
                                            text = trans.descricao,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Data: ${formatDateForDisplay(trans.data)} • Categoria: ${trans.categoria}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                    trans.cropCloudId?.let { cropCloudId ->
                                        val cropName = state.safras.firstOrNull {
                                            it.cloudId == cropCloudId
                                        }?.nomeCultura ?: "Safra removida"
                                        Text(
                                            text = "Safra: $cropName",
                                            fontSize = 11.sp,
                                            color = PrimaryAgroGreen,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (hideFinancialValues) {
                                            if (trans.tipo == TransactionType.ENTRADA) "+ R$ ••••" else "- R$ ••••"
                                        } else String.format(
                                            Locale("pt", "BR"),
                                            "%s R$ %.2f",
                                            if (trans.tipo == TransactionType.ENTRADA) "+" else "-",
                                            trans.valor
                                        ),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (trans.tipo == TransactionType.ENTRADA) StatusGreen else StatusOrange
                                    )
                                    Row {
                                        IconButton(onClick = {
                                            transactionBeingEdited = trans
                                            showFinanceDialog = true
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Editar Lançamento",
                                                tint = PrimaryAgroGreen
                                            )
                                        }
                                        IconButton(onClick = {
                                            transactionPendingDeletion = trans.id
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Apagar Lançamento",
                                                tint = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                EasyBigButton(
                    text = "Registrar Ganho ou Gasto",
                    icon = Icons.Default.Add,
                    onClick = {
                        transactionBeingEdited = null
                        showFinanceDialog = true
                    },
                    containerColor = AccentEarthOrange
                )
            }
        }
    }

    if (showCropDialog) {
        CropFormDialog(
            existing = cropBeingEdited,
            onDismiss = {
                showCropDialog = false
                cropBeingEdited = null
            },
            onConfirm = { nome, area, inicio, colheita, progresso, manejo ->
                viewModel.saveCrop(
                    cropBeingEdited,
                    nome,
                    area,
                    inicio,
                    colheita,
                    progresso,
                    manejo
                )
                showCropDialog = false
                cropBeingEdited = null
            }
        )
    }

    if (showFinanceDialog) {
        FinanceFormDialog(
            existing = transactionBeingEdited,
            crops = state.safras,
            onDismiss = {
                showFinanceDialog = false
                transactionBeingEdited = null
            },
            onConfirm = { desc, valor, tipo, cat, data, cropCloudId ->
                viewModel.saveTransaction(
                    transactionBeingEdited,
                    desc,
                    valor,
                    tipo,
                    cat,
                    data,
                    cropCloudId
                )
                showFinanceDialog = false
                transactionBeingEdited = null
            }
        )
    }

    if (showFinancialFilters) {
        FinancialFiltersDialog(
            initial = state.filters,
            crops = state.safras,
            categories = state.categories,
            onDismiss = { showFinancialFilters = false },
            onClear = {
                viewModel.clearFinancialFilters()
                showFinancialFilters = false
            },
            onApply = {
                viewModel.applyFinancialFilters(it)
                showFinancialFilters = false
            }
        )
    }

    cropPendingDeletion?.let { cropId ->
        ConfirmDeleteDialog(
            itemLabel = "esta safra",
            onConfirm = {
                viewModel.deleteCrop(cropId)
                cropPendingDeletion = null
            },
            onDismiss = { cropPendingDeletion = null }
        )
    }

    transactionPendingDeletion?.let { transactionId ->
        ConfirmDeleteDialog(
            itemLabel = "este lançamento financeiro",
            onConfirm = {
                viewModel.deleteTransaction(transactionId)
                transactionPendingDeletion = null
            },
            onDismiss = { transactionPendingDeletion = null }
        )
    }
}

@Composable
private fun PlotFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(if (selected) PrimaryAgroGreen else SurfaceCard, RoundedCornerShape(22.dp))
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = if (selected) Color.White else TextDark,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            count.toString(),
            color = if (selected) Color.White else TextMuted,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(start = 7.dp)
                .background(if (selected) Color.White.copy(alpha = .2f) else SurfaceSoft, RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun PlotSummaryCard(
    modifier: Modifier,
    value: String,
    label: String,
    warning: Boolean = false
) {
    Column(
        modifier = modifier
            .background(SurfaceSoft, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(
            value,
            color = if (warning) AccentEarthOrange else TextDark,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(label, color = TextMuted, fontSize = 9.5.sp, lineHeight = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun PlotListCard(
    crop: CropEntity,
    index: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val needsAttention = crop.statusManejo.contains("aten", ignoreCase = true)
    val readyToHarvest = crop.statusManejo.contains("colhe", ignoreCase = true) || crop.progressoPercentual >= 80
    val accent = when {
        needsAttention -> AccentEarthOrange
        readyToHarvest -> Color(0xFF2A6FB0)
        else -> StatusGreen
    }
    val tile = when (index % 5) {
        1 -> Color(0xFFF3E3B9)
        2 -> Color(0xFFD9ECD1)
        3 -> Color(0xFFCFE3F4)
        4 -> Color(0xFFE3D8F0)
        else -> Color(0xFFC8E6C9)
    }
    EasyCard {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 76.dp, height = 88.dp)
                    .background(tile, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Agriculture, contentDescription = null, tint = accent.copy(alpha = .75f), modifier = Modifier.size(30.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(9.dp)
                        .background(accent, CircleShape)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Talhão ${index + 1}", color = TextDark, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${crop.nomeCultura} · ${crop.areaHectares} ha", color = TextMuted, fontSize = 10.5.sp)
                    }
                    Text(
                        crop.statusManejo.ifBlank { "Saudável" }.uppercase(),
                        color = accent,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.background(accent.copy(alpha = .1f), RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
                Text(
                    "${crop.progressoPercentual}% do ciclo  ·  Plantio ${formatDateForDisplay(crop.dataInicio)}",
                    color = TextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 9.dp)
                )
                Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(1.dp).background(CardBorder))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Colheita: ${formatDateForDisplay(crop.previsaoColheita)}",
                        color = TextMuted,
                        fontSize = 9.5.sp,
                        modifier = Modifier.weight(1f).padding(top = 7.dp)
                    )
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, "Editar safra", tint = PrimaryAgroGreen, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Delete, "Remover safra", tint = StatusOrange, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialFiltersDialog(
    initial: FinancialFilterCriteria,
    crops: List<CropEntity>,
    categories: List<String>,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onApply: (FinancialFilterCriteria) -> Unit
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    val periodIsValid = draft.fromDate == null || draft.toDate == null ||
        isIsoDateOnOrAfter(draft.toDate.orEmpty(), draft.fromDate.orEmpty())
    val cropLabel = when (draft.cropCloudId) {
        null -> "Todas"
        FILTER_WITHOUT_CROP -> "Sem safra específica"
        else -> crops.firstOrNull { it.cloudId == draft.cropCloudId }?.nomeCultura
            ?: "Safra não encontrada"
    }
    val typeLabel = when (draft.transactionType) {
        null -> "Entradas e saídas"
        TransactionType.ENTRADA -> "Somente entradas"
        TransactionType.SAIDA -> "Somente saídas"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtrar lançamentos", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OptionalDateFilterField(
                    label = "Data a partir de",
                    isoDate = draft.fromDate,
                    onDateSelected = { draft = draft.copy(fromDate = it) },
                    onClear = { draft = draft.copy(fromDate = null) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OptionalDateFilterField(
                    label = "Data até",
                    isoDate = draft.toDate,
                    onDateSelected = { draft = draft.copy(toDate = it) },
                    onClear = { draft = draft.copy(toDate = null) }
                )
                if (!periodIsValid) {
                    Text(
                        "A data final precisa ser igual ou posterior à inicial.",
                        color = StatusOrange,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                FilterSelectionField(
                    label = "Safra",
                    selectedLabel = cropLabel,
                    choices = buildList {
                        add(FilterChoice(null, "Todas as safras"))
                        add(FilterChoice(FILTER_WITHOUT_CROP, "Sem safra específica"))
                        crops.sortedBy { it.nomeCultura.lowercase() }.forEach {
                            add(FilterChoice(it.cloudId, it.nomeCultura))
                        }
                    },
                    onSelected = { draft = draft.copy(cropCloudId = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                FilterSelectionField(
                    label = "Categoria",
                    selectedLabel = draft.category ?: "Todas",
                    choices = listOf(FilterChoice(null, "Todas as categorias")) +
                        categories.map { FilterChoice(it, it) },
                    onSelected = { draft = draft.copy(category = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                FilterSelectionField(
                    label = "Tipo",
                    selectedLabel = typeLabel,
                    choices = listOf(
                        FilterChoice(null, "Entradas e saídas"),
                        FilterChoice(TransactionType.ENTRADA.name, "Somente entradas"),
                        FilterChoice(TransactionType.SAIDA.name, "Somente saídas")
                    ),
                    onSelected = { value ->
                        draft = draft.copy(
                            transactionType = value?.let(TransactionType::valueOf)
                        )
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(draft) },
                enabled = periodIsValid,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAgroGreen)
            ) {
                Text("Aplicar filtros")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text("Limpar") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

@Composable
fun CropFormDialog(
    existing: CropEntity?,
    onDismiss: () -> Unit,
    onConfirm: (
        nome: String,
        area: Double,
        inicio: String,
        colheita: String,
        progresso: Int,
        manejo: String
    ) -> Unit
) {
    var nome by remember(existing?.id) { mutableStateOf(existing?.nomeCultura.orEmpty()) }
    var areaText by remember(existing?.id) {
        mutableStateOf(existing?.areaHectares?.toString().orEmpty())
    }
    var inicio by remember(existing?.id) { mutableStateOf(existing?.dataInicio ?: todayIso()) }
    var colheita by remember(existing?.id) {
        mutableStateOf(existing?.previsaoColheita ?: todayPlusMonthsIso(3))
    }
    var progressoText by remember(existing?.id) {
        mutableStateOf(existing?.progressoPercentual?.toString() ?: "0")
    }
    var manejo by remember(existing?.id) {
        mutableStateOf(existing?.statusManejo ?: "Preparo do solo")
    }
    val area = parsePositiveDecimal(areaText)
    val progresso = parsePercentage(progressoText)
    val harvestDateIsValid = isIsoDateOnOrAfter(colheita, inicio)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) "Cadastrar Cultura / Safra" else "Editar Cultura / Safra",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Cultura Agrícola (ex: Milho, Feijão)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = areaText,
                    onValueChange = { areaText = it },
                    label = { Text("Área em Hectares (ha)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                DateSelectionButton(
                    label = "Data de início",
                    isoDate = inicio,
                    onDateSelected = { inicio = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                DateSelectionButton(
                    label = "Previsão de colheita",
                    isoDate = colheita,
                    onDateSelected = { colheita = it }
                )
                if (!harvestDateIsValid) {
                    Text(
                        text = "A colheita não pode ser anterior ao início da safra.",
                        color = StatusOrange,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = progressoText,
                    onValueChange = { progressoText = it },
                    label = { Text("Progresso da safra (0 a 100%)") },
                    supportingText = if (progresso == null) {
                        { Text("Informe um número inteiro entre 0 e 100.") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = manejo,
                    onValueChange = { manejo = it },
                    label = { Text("Situação do manejo") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        nome,
                        requireNotNull(area),
                        inicio,
                        colheita,
                        requireNotNull(progresso),
                        manejo
                    )
                },
                enabled = nome.isNotBlank() &&
                    area != null &&
                    progresso != null &&
                    manejo.isNotBlank() &&
                    harvestDateIsValid,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAgroGreen)
            ) {
                Text("Salvar Safra", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun FinanceFormDialog(
    existing: FinancialEntity?,
    crops: List<CropEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        desc: String,
        valor: Double,
        tipo: TransactionType,
        cat: String,
        data: String,
        cropCloudId: String?
    ) -> Unit
) {
    var desc by remember(existing?.id) { mutableStateOf(existing?.descricao.orEmpty()) }
    var valorText by remember(existing?.id) {
        mutableStateOf(existing?.valor?.toString().orEmpty())
    }
    var isEntrada by remember(existing?.id) {
        mutableStateOf(existing?.tipo != TransactionType.SAIDA)
    }
    var categoria by remember(existing?.id) {
        mutableStateOf(existing?.categoria ?: "Geral")
    }
    var data by remember(existing?.id) { mutableStateOf(existing?.data ?: todayIso()) }
    var cropCloudId by remember(existing?.id) { mutableStateOf(existing?.cropCloudId) }
    val valor = parsePositiveDecimal(valorText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) "Registrar Movimentação de Caixa" else "Editar Movimentação de Caixa",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { isEntrada = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEntrada) StatusGreen else Color.LightGray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Venda / Receita")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { isEntrada = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isEntrada) StatusOrange else Color.LightGray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Compra / Custo")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descrição (ex: Venda de Produção / Adubo)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text("Categoria") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                DateSelectionButton(
                    label = "Data do lançamento",
                    isoDate = data,
                    onDateSelected = { data = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                CropSelectionField(
                    crops = crops,
                    selectedCropCloudId = cropCloudId,
                    onCropSelected = { cropCloudId = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = valorText,
                    onValueChange = { valorText = it },
                    label = { Text("Valor em Reais (R$)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tipo = if (isEntrada) TransactionType.ENTRADA else TransactionType.SAIDA
                    onConfirm(desc, requireNotNull(valor), tipo, categoria, data, cropCloudId)
                },
                enabled = desc.isNotBlank() && valor != null && categoria.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAgroGreen)
            ) {
                Text("Salvar Lançamento", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
