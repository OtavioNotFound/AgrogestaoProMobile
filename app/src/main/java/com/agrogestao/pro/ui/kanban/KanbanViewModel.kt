package com.agrogestao.pro.ui.kanban

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.data.local.entities.TaskStatus
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.repository.AgroRepository
import com.agrogestao.pro.data.reminders.DisabledTaskReminderGateway
import com.agrogestao.pro.data.reminders.TaskReminderGateway
import com.agrogestao.pro.domain.TaskFilterCriteria
import com.agrogestao.pro.domain.TaskReminderSettings
import com.agrogestao.pro.domain.filterTasks
import com.agrogestao.pro.domain.taskFilterCategories
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class KanbanUiState(
    val aFazer: List<TaskEntity> = emptyList(),
    val emProgresso: List<TaskEntity> = emptyList(),
    val concluido: List<TaskEntity> = emptyList(),
    val safras: List<CropEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val filters: TaskFilterCriteria = TaskFilterCriteria(),
    val reminderOwnerUserId: String = "",
    val reminderSettings: TaskReminderSettings = TaskReminderSettings(),
    val totalAFazer: Int = 0,
    val totalEmProgresso: Int = 0,
    val totalConcluido: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class KanbanViewModel(
    private val repository: AgroRepository,
    private val reminderGateway: TaskReminderGateway = DisabledTaskReminderGateway
) : ViewModel() {
    private val filters = MutableStateFlow(TaskFilterCriteria())
    private val reminderState = repository.activeOwnerUserId.flatMapLatest { ownerUserId ->
        if (ownerUserId.isBlank()) {
            flowOf(AccountReminderState("", TaskReminderSettings()))
        } else {
            reminderGateway.observe(ownerUserId).map { settings ->
                AccountReminderState(ownerUserId, settings)
            }
        }
    }

    val uiState: StateFlow<KanbanUiState> = combine(
        repository.allTasks,
        repository.allCrops,
        filters,
        reminderState
    ) { tasks, crops, criteria, reminders ->
        val filteredTasks = filterTasks(tasks, criteria)
        KanbanUiState(
            aFazer = filteredTasks.filter { it.status == TaskStatus.A_FAZER },
            emProgresso = filteredTasks.filter { it.status == TaskStatus.EM_PROGRESSO },
            concluido = filteredTasks.filter { it.status == TaskStatus.CONCLUIDO },
            safras = crops,
            categories = taskFilterCategories(tasks),
            filters = criteria,
            reminderOwnerUserId = reminders.ownerUserId,
            reminderSettings = reminders.settings,
            totalAFazer = tasks.count { it.status == TaskStatus.A_FAZER },
            totalEmProgresso = tasks.count { it.status == TaskStatus.EM_PROGRESSO },
            totalConcluido = tasks.count { it.status == TaskStatus.CONCLUIDO }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KanbanUiState()
    )

    fun updateTaskStatus(taskId: Long, newStatus: TaskStatus) {
        viewModelScope.launch {
            repository.updateTaskStatus(taskId, newStatus)
        }
    }

    fun saveTask(
        existing: TaskEntity?,
        titulo: String,
        descricao: String,
        categoria: String,
        dataLimite: String,
        cropCloudId: String?
    ) {
        viewModelScope.launch {
            val task = (existing ?: TaskEntity(
                titulo = titulo.trim(),
                descricao = descricao.trim(),
                categoria = categoria.trim(),
                dataLimite = dataLimite,
                status = TaskStatus.A_FAZER,
                cropCloudId = cropCloudId
            )).copy(
                titulo = titulo.trim(),
                descricao = descricao.trim(),
                categoria = categoria.trim(),
                dataLimite = dataLimite,
                cropCloudId = cropCloudId
            )
            if (existing == null) repository.insertTask(task) else repository.updateTask(task)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    fun applyFilters(criteria: TaskFilterCriteria) {
        filters.value = criteria
    }

    fun clearFilters() {
        filters.value = TaskFilterCriteria()
    }

    fun updateReminderSettings(settings: TaskReminderSettings): Result<Unit> = runCatching {
        check(settings.isValid) { "Escolha um horário e uma antecedência válidos." }
        check(uiState.value.reminderOwnerUserId.isNotBlank()) {
            "Entre em uma conta antes de configurar os lembretes."
        }
        check(reminderGateway.save(uiState.value.reminderOwnerUserId, settings)) {
            "Não foi possível salvar os lembretes neste celular."
        }
    }
}

private data class AccountReminderState(
    val ownerUserId: String,
    val settings: TaskReminderSettings
)

class KanbanViewModelFactory(
    private val repository: AgroRepository,
    private val reminderGateway: TaskReminderGateway = DisabledTaskReminderGateway
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KanbanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return KanbanViewModel(repository, reminderGateway) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
