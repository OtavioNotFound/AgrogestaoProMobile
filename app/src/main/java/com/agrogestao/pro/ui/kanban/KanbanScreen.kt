package com.agrogestao.pro.ui.kanban

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.data.local.entities.TaskStatus
import com.agrogestao.pro.data.local.entities.CropEntity
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
import com.agrogestao.pro.ui.theme.BackgroundLight
import com.agrogestao.pro.ui.theme.PrimaryAgroGreen
import com.agrogestao.pro.ui.theme.StatusBlue
import com.agrogestao.pro.ui.theme.StatusGreen
import com.agrogestao.pro.ui.theme.StatusOrange
import com.agrogestao.pro.ui.theme.TextDark
import com.agrogestao.pro.ui.theme.TextMuted
import com.agrogestao.pro.ui.theme.SurfaceCard
import com.agrogestao.pro.ui.theme.SurfaceSoft
import com.agrogestao.pro.ui.theme.CardBorder
import com.agrogestao.pro.ui.theme.AgroGreen100
import com.agrogestao.pro.ui.theme.AccentEarthOrange
import com.agrogestao.pro.domain.formatDateForDisplay
import com.agrogestao.pro.domain.FILTER_WITHOUT_CROP
import com.agrogestao.pro.domain.TaskFilterCriteria
import com.agrogestao.pro.domain.isIsoDateOnOrAfter
import com.agrogestao.pro.domain.todayIso
import com.agrogestao.pro.domain.TaskReminderSettings
import com.agrogestao.pro.domain.taskReminderScheduleLabel
import androidx.core.content.ContextCompat

@Composable
fun KanbanScreen(viewModel: KanbanViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val notificationPermissionGranted =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var taskBeingEdited by remember { mutableStateOf<TaskEntity?>(null) }
    var taskPendingDeletion by remember { mutableStateOf<Long?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var showReminderSettings by remember { mutableStateOf(false) }
    var pendingPermissionSettings by remember { mutableStateOf<TaskReminderSettings?>(null) }
    var boardFilter by remember { mutableStateOf("Todas") }
    var isKanbanView by remember { mutableStateOf(true) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pending = pendingPermissionSettings
        pendingPermissionSettings = null
        if (granted && pending != null) {
            viewModel.updateReminderSettings(pending).fold(
                onSuccess = { showReminderSettings = false },
                onFailure = { error ->
                    Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                }
            )
        } else if (!granted) {
            Toast.makeText(
                context,
                "A permissão não foi concedida. Os lembretes continuam pausados.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        AppScreenHeader(
            title = "Tarefas",
            subtitle = "${state.safras.size} ${if (state.safras.size == 1) "talhão" else "talhões"} · ${state.totalAFazer + state.totalEmProgresso} tarefas em andamento",
            onBack = onBack,
            actionIcon = Icons.Default.Add,
            actionDescription = "Adicionar tarefa",
            onAction = {
                taskBeingEdited = null
                showDialog = true
            },
            primaryAction = true
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.background(SurfaceSoft, RoundedCornerShape(9.dp)).padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TaskViewChoice("Kanban", Icons.Default.ViewKanban, isKanbanView) { isKanbanView = true }
                    TaskViewChoice("Lista", Icons.AutoMirrored.Filled.ViewList, !isKanbanView) { isKanbanView = false }
                }
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = { showReminderSettings = true },
                    modifier = Modifier.size(38.dp).background(SurfaceSoft, RoundedCornerShape(11.dp))
                ) {
                    Icon(
                        if (state.reminderSettings.enabled && notificationPermissionGranted) Icons.Default.Alarm else Icons.Default.NotificationsOff,
                        "Configurar lembretes",
                        tint = if (state.reminderSettings.enabled && notificationPermissionGranted) StatusGreen else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(7.dp))
                IconButton(
                    onClick = { showFilters = true },
                    modifier = Modifier.size(38.dp).background(SurfaceSoft, RoundedCornerShape(11.dp))
                ) {
                    Icon(Icons.Default.Tune, "Filtros", tint = PrimaryAgroGreen, modifier = Modifier.size(18.dp))
                }
            }

            val quickFilters = listOf("Todas") + state.safras.map { it.nomeCultura }.distinct().take(4) + "Atrasadas"
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickFilters.forEach { label ->
                    TaskQuickFilterChip(label, boardFilter == label) { boardFilter = label }
                }
            }

            val allTasks = state.aFazer + state.emProgresso + state.concluido
            if (!isKanbanView) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(allTasks.filter { matchesQuickTaskFilter(it, boardFilter, state.safras) }, key = { it.id }) { task ->
                        EasyTaskItemCard(
                            task = task,
                            cropName = state.safras.firstOrNull {
                                it.cloudId == task.cropCloudId
                            }?.nomeCultura,
                            onStatusChange = { newStatus -> viewModel.updateTaskStatus(task.id, newStatus) },
                            onEdit = {
                                taskBeingEdited = task
                                showDialog = true
                            },
                            onDelete = { taskPendingDeletion = task.id }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TaskBoardColumn(
                        title = "A FAZER",
                        count = state.aFazer.size,
                        dotColor = Color(0xFFB9C0BA),
                        tasks = state.aFazer.filter { matchesQuickTaskFilter(it, boardFilter, state.safras) },
                        crops = state.safras,
                        onStatusChange = { task -> viewModel.updateTaskStatus(task.id, TaskStatus.EM_PROGRESSO) },
                        onEdit = { task -> taskBeingEdited = task; showDialog = true },
                        onDelete = { task -> taskPendingDeletion = task.id },
                        onAdd = { taskBeingEdited = null; showDialog = true }
                    )
                    TaskBoardColumn(
                        title = "FAZENDO",
                        count = state.emProgresso.size,
                        dotColor = Color(0xFFF5B947),
                        tasks = state.emProgresso.filter { matchesQuickTaskFilter(it, boardFilter, state.safras) },
                        crops = state.safras,
                        onStatusChange = { task -> viewModel.updateTaskStatus(task.id, TaskStatus.CONCLUIDO) },
                        onEdit = { task -> taskBeingEdited = task; showDialog = true },
                        onDelete = { task -> taskPendingDeletion = task.id },
                        onAdd = { taskBeingEdited = null; showDialog = true }
                    )
                    TaskBoardColumn(
                        title = "FEITO",
                        count = state.concluido.size,
                        dotColor = StatusGreen,
                        tasks = state.concluido.filter { matchesQuickTaskFilter(it, boardFilter, state.safras) },
                        crops = state.safras,
                        onStatusChange = { task -> viewModel.updateTaskStatus(task.id, TaskStatus.A_FAZER) },
                        onEdit = { task -> taskBeingEdited = task; showDialog = true },
                        onDelete = { task -> taskPendingDeletion = task.id },
                        onAdd = { taskBeingEdited = null; showDialog = true }
                    )
                }
            }
        }
    }

    if (showReminderSettings) {
        TaskReminderSettingsDialog(
            initial = state.reminderSettings,
            onDismiss = { showReminderSettings = false },
            onSave = { settings ->
                val needsPermission = settings.enabled &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                if (needsPermission) {
                    pendingPermissionSettings = settings
                } else {
                    viewModel.updateReminderSettings(settings).fold(
                        onSuccess = { showReminderSettings = false },
                        onFailure = { error ->
                            Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        )
    }

    pendingPermissionSettings?.let {
        AlertDialog(
            onDismissRequest = { pendingPermissionSettings = null },
            title = { Text("Permitir lembretes?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "O Android precisa da sua permissão para mostrar os avisos. Eles são " +
                        "criados neste celular, funcionam sem internet e não enviam seus dados."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAgroGreen)
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPermissionSettings = null }) {
                    Text("Agora não")
                }
            }
        )
    }

    if (showDialog) {
        TaskFormDialog(
            existing = taskBeingEdited,
            crops = state.safras,
            onDismiss = {
                showDialog = false
                taskBeingEdited = null
            },
            onConfirm = { titulo, desc, cat, dataLimite, cropCloudId ->
                viewModel.saveTask(
                    taskBeingEdited,
                    titulo,
                    desc,
                    cat,
                    dataLimite,
                    cropCloudId
                )
                showDialog = false
                taskBeingEdited = null
            }
        )
    }

    if (showFilters) {
        val selectedStatus = when (selectedTab) {
            0 -> TaskStatus.A_FAZER
            1 -> TaskStatus.EM_PROGRESSO
            else -> TaskStatus.CONCLUIDO
        }
        TaskFiltersDialog(
            initial = state.filters,
            selectedStatus = selectedStatus,
            crops = state.safras,
            categories = state.categories,
            onDismiss = { showFilters = false },
            onClear = {
                viewModel.clearFilters()
                showFilters = false
            },
            onApply = { criteria, status ->
                viewModel.applyFilters(criteria)
                selectedTab = status.ordinal
                showFilters = false
            }
        )
    }

    taskPendingDeletion?.let { taskId ->
        ConfirmDeleteDialog(
            itemLabel = "esta tarefa",
            onConfirm = {
                viewModel.deleteTask(taskId)
                taskPendingDeletion = null
            },
            onDismiss = { taskPendingDeletion = null }
        )
    }
}

@Composable
private fun TaskViewChoice(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(if (selected) SurfaceCard else Color.Transparent, RoundedCornerShape(7.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) PrimaryAgroGreen else TextMuted, modifier = Modifier.size(14.dp))
        Text(
            label,
            color = if (selected) PrimaryAgroGreen else TextMuted,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 5.dp)
        )
    }
}

@Composable
private fun TaskQuickFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.White else TextDark,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(if (selected) PrimaryAgroGreen else SurfaceCard, RoundedCornerShape(20.dp))
            .padding(horizontal = 13.dp, vertical = 8.dp)
    )
}

private fun matchesQuickTaskFilter(task: TaskEntity, filter: String, crops: List<CropEntity>): Boolean = when (filter) {
    "Todas" -> true
    "Atrasadas" -> task.status != TaskStatus.CONCLUIDO && task.dataLimite < todayIso()
    else -> crops.firstOrNull { it.cloudId == task.cropCloudId }?.nomeCultura.equals(filter, ignoreCase = true)
}

@Composable
private fun TaskBoardColumn(
    title: String,
    count: Int,
    dotColor: Color,
    tasks: List<TaskEntity>,
    crops: List<CropEntity>,
    onStatusChange: (TaskEntity) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    onAdd: () -> Unit
) {
    Column(modifier = Modifier.width(252.dp).fillMaxHeight()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(dotColor, CircleShape))
            Text(title, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 7.dp))
            Text(
                count.toString(),
                color = TextMuted,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskBoardCard(
                    task = task,
                    cropName = crops.firstOrNull { it.cloudId == task.cropCloudId }?.nomeCultura,
                    dotColor = dotColor,
                    onStatusChange = { onStatusChange(task) },
                    onEdit = { onEdit(task) },
                    onDelete = { onDelete(task) }
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAdd)
                        .background(SurfaceCard, RoundedCornerShape(12.dp))
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = TextMuted, modifier = Modifier.size(15.dp))
                    Text("Nova tarefa", color = TextMuted, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun TaskBoardCard(
    task: TaskEntity,
    cropName: String?,
    dotColor: Color,
    onStatusChange: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceSoft),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (cropName ?: task.categoria).uppercase(),
                    color = PrimaryAgroGreen,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.background(AgroGreen100, RoundedCornerShape(7.dp)).padding(horizontal = 7.dp, vertical = 4.dp)
                )
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(7.dp).background(dotColor, CircleShape))
            }
            Text(
                task.titulo,
                color = TextDark,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                cropName?.let { "Talhão · $it" } ?: task.descricao.ifBlank { "Tarefa geral" },
                color = TextMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 7.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 9.dp)) {
                Text(
                    formatDateForDisplay(task.dataLimite),
                    color = if (task.status != TaskStatus.CONCLUIDO && task.dataLimite < todayIso()) AccentEarthOrange else TextMuted,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, "Editar", tint = TextMuted, modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, "Excluir", tint = StatusOrange, modifier = Modifier.size(15.dp))
                }
                Text(
                    when (task.status) {
                        TaskStatus.A_FAZER -> "Iniciar"
                        TaskStatus.EM_PROGRESSO -> "Concluir"
                        TaskStatus.CONCLUIDO -> "Reabrir"
                    },
                    color = PrimaryAgroGreen,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clickable(onClick = onStatusChange).padding(start = 5.dp, top = 7.dp, bottom = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun ReminderStatusCard(
    settings: TaskReminderSettings,
    notificationPermissionGranted: Boolean,
    onConfigure: () -> Unit
) {
    val effectivelyActive = settings.enabled && notificationPermissionGranted
    val accent = if (effectivelyActive) StatusGreen else {
        if (settings.enabled) StatusOrange else TextMuted
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.09f)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (effectivelyActive) Icons.Default.Alarm else {
                        Icons.Default.NotificationsOff
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(end = 10.dp)
                )
                Text(
                    text = when {
                        effectivelyActive -> "Lembretes ativos"
                        settings.enabled -> "Lembretes bloqueados pelo Android"
                        else -> "Lembretes pausados"
                    },
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onConfigure) {
                    Text("Configurar", fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = when {
                    effectivelyActive -> taskReminderScheduleLabel(settings)
                    settings.enabled -> "Toque em Configurar para permitir os avisos novamente."
                    else -> "Ative avisos locais para tarefas próximas ou atrasadas."
                },
                fontSize = 13.sp,
                color = TextDark,
                modifier = Modifier.padding(start = 34.dp)
            )
        }
    }
}

@Composable
private fun TaskReminderSettingsDialog(
    initial: TaskReminderSettings,
    onDismiss: () -> Unit,
    onSave: (TaskReminderSettings) -> Unit
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    val daysLabel = when (draft.daysBefore) {
        0 -> "No dia do prazo"
        1 -> "1 dia antes"
        else -> "${draft.daysBefore} dias antes"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lembretes de tarefas", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mostrar lembretes", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Você pode pausar quando quiser.",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = draft.enabled,
                        onCheckedChange = { draft = draft.copy(enabled = it) }
                    )
                }
                FilterSelectionField(
                    label = "Antecedência",
                    selectedLabel = daysLabel,
                    choices = listOf(
                        FilterChoice("0", "No dia do prazo"),
                        FilterChoice("1", "1 dia antes"),
                        FilterChoice("2", "2 dias antes"),
                        FilterChoice("3", "3 dias antes"),
                        FilterChoice("7", "7 dias antes")
                    ),
                    onSelected = { value ->
                        draft = draft.copy(daysBefore = value?.toIntOrNull() ?: 1)
                    }
                )
                Text("Horário do aviso", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedIconButton(
                        onClick = {
                            draft = draft.copy(hourOfDay = (draft.hourOfDay + 23) % 24)
                        }
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Diminuir uma hora"
                        )
                    }
                    Text(
                        "${draft.hourOfDay.toString().padStart(2, '0')}:00",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryAgroGreen
                    )
                    OutlinedIconButton(
                        onClick = {
                            draft = draft.copy(hourOfDay = (draft.hourOfDay + 1) % 24)
                        }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Aumentar uma hora"
                        )
                    }
                }
                Text(
                    "Os avisos ficam neste celular, funcionam sem internet e são " +
                        "recalculados quando a tarefa muda.",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(draft) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAgroGreen)
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun TaskFiltersDialog(
    initial: TaskFilterCriteria,
    selectedStatus: TaskStatus,
    crops: List<CropEntity>,
    categories: List<String>,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onApply: (TaskFilterCriteria, TaskStatus) -> Unit
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    var status by remember(selectedStatus) { mutableStateOf(selectedStatus) }
    val periodIsValid = draft.fromDate == null || draft.toDate == null ||
        isIsoDateOnOrAfter(draft.toDate.orEmpty(), draft.fromDate.orEmpty())
    val cropLabel = when (draft.cropCloudId) {
        null -> "Todas"
        FILTER_WITHOUT_CROP -> "Sem safra específica"
        else -> crops.firstOrNull { it.cloudId == draft.cropCloudId }?.nomeCultura
            ?: "Safra não encontrada"
    }
    val statusLabel = when (status) {
        TaskStatus.A_FAZER -> "Pendente"
        TaskStatus.EM_PROGRESSO -> "Em curso"
        TaskStatus.CONCLUIDO -> "Concluída"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtrar tarefas", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OptionalDateFilterField(
                    label = "Prazo a partir de",
                    isoDate = draft.fromDate,
                    onDateSelected = { draft = draft.copy(fromDate = it) },
                    onClear = { draft = draft.copy(fromDate = null) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OptionalDateFilterField(
                    label = "Prazo até",
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
                    label = "Situação",
                    selectedLabel = statusLabel,
                    choices = listOf(
                        FilterChoice(TaskStatus.A_FAZER.name, "Pendente"),
                        FilterChoice(TaskStatus.EM_PROGRESSO.name, "Em curso"),
                        FilterChoice(TaskStatus.CONCLUIDO.name, "Concluída")
                    ),
                    onSelected = { value ->
                        status = value?.let(TaskStatus::valueOf) ?: TaskStatus.A_FAZER
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(draft, status) },
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
fun EasyTaskItemCard(
    task: TaskEntity,
    cropName: String?,
    onStatusChange: (TaskStatus) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    EasyCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SimpleStatusBadge(
                        text = task.categoria,
                        backgroundColor = PrimaryAgroGreen.copy(alpha = 0.15f),
                        textColor = PrimaryAgroGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CloudSyncStatusBadge(syncStatus = task.syncStatus)
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Tarefa",
                            tint = PrimaryAgroGreen
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remover Tarefa",
                            tint = StatusOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = task.titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            if (task.descricao.isNotBlank()) {
                Text(
                    text = task.descricao,
                    fontSize = 11.5.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                text = "Prazo: ${formatDateForDisplay(task.dataLimite)}",
                fontSize = 10.5.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )

            cropName?.let {
                Text(
                    text = "Safra: $it",
                    fontSize = 10.5.sp,
                    color = PrimaryAgroGreen,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                when (task.status) {
                    TaskStatus.A_FAZER -> {
                        Button(
                            onClick = { onStatusChange(TaskStatus.EM_PROGRESSO) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusBlue)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Iniciar execução", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    TaskStatus.EM_PROGRESSO -> {
                        Button(
                            onClick = { onStatusChange(TaskStatus.CONCLUIDO) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Marcar como concluída", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    TaskStatus.CONCLUIDO -> {
                        Button(
                            onClick = { onStatusChange(TaskStatus.A_FAZER) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Retornar para pendente", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskFormDialog(
    existing: TaskEntity?,
    crops: List<CropEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        titulo: String,
        descricao: String,
        categoria: String,
        dataLimite: String,
        cropCloudId: String?
    ) -> Unit
) {
    var titulo by remember(existing?.id) { mutableStateOf(existing?.titulo.orEmpty()) }
    var descricao by remember(existing?.id) { mutableStateOf(existing?.descricao.orEmpty()) }
    var categoria by remember(existing?.id) {
        mutableStateOf(existing?.categoria ?: "Manejo")
    }
    var dataLimite by remember(existing?.id) {
        mutableStateOf(existing?.dataLimite ?: todayIso())
    }
    var cropCloudId by remember(existing?.id) { mutableStateOf(existing?.cropCloudId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null) "Cadastrar Tarefa Agrícola" else "Editar Tarefa Agrícola",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título (ex: Irrigação da Cultura)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                DateSelectionButton(
                    label = "Prazo",
                    isoDate = dataLimite,
                    onDateSelected = { dataLimite = it }
                )
                Spacer(modifier = Modifier.height(10.dp))
                CropSelectionField(
                    crops = crops,
                    selectedCropCloudId = cropCloudId,
                    onCropSelected = { cropCloudId = it }
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Instruções Operacionais") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text("Categoria (Adubação, Irrigação, Colheita...)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(titulo, descricao, categoria, dataLimite, cropCloudId)
                },
                enabled = titulo.isNotBlank() && categoria.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAgroGreen)
            ) {
                Text("Salvar Atividade", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", fontSize = 16.sp)
            }
        }
    )
}
