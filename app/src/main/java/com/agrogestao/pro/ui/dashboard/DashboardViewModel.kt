package com.agrogestao.pro.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.ProducerEntity
import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.data.local.entities.TaskStatus
import com.agrogestao.pro.data.repository.AgroRepository
import com.agrogestao.pro.domain.calculateFinancialSummary
import com.agrogestao.pro.domain.accountPasswordError
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val producer: ProducerEntity? = null,
    val totalEntradas: Double = 0.0,
    val totalSaidas: Double = 0.0,
    val saldoTotal: Double = 0.0,
    val safrasAtivas: List<CropEntity> = emptyList(),
    val tarefasPendentes: List<TaskEntity> = emptyList(),
    val isSyncing: Boolean = false,
    val syncFeedback: String? = null,
    val isBackupBusy: Boolean = false,
    val backupFeedback: String? = null,
    val isChangingPassword: Boolean = false,
    val passwordFeedback: String? = null
)

class DashboardViewModel(private val repository: AgroRepository) : ViewModel() {

    private val syncActionState = MutableStateFlow(SyncActionState())
    private val backupActionState = MutableStateFlow(BackupActionState())
    private val passwordActionState = MutableStateFlow(PasswordActionState())

    private val dashboardData = combine(
        repository.producerProfile,
        repository.allTransactions,
        repository.allCrops,
        repository.allTasks
    ) { producer, transactions, crops, tasks ->
        val financial = calculateFinancialSummary(transactions)
        val pendentes = tasks.filter { it.status == TaskStatus.A_FAZER }

        DashboardUiState(
            producer = producer,
            totalEntradas = financial.income,
            totalSaidas = financial.expenses,
            saldoTotal = financial.balance,
            safrasAtivas = crops,
            tarefasPendentes = pendentes
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        dashboardData,
        syncActionState,
        backupActionState,
        passwordActionState
    ) { data, syncAction, backupAction, passwordAction ->
        data.copy(
            isSyncing = syncAction.isSyncing,
            syncFeedback = syncAction.feedback,
            isBackupBusy = backupAction.isBusy,
            backupFeedback = backupAction.feedback,
            isChangingPassword = passwordAction.isBusy,
            passwordFeedback = passwordAction.feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun syncNow() {
        if (syncActionState.value.isSyncing) return
        viewModelScope.launch {
            syncActionState.value = SyncActionState(isSyncing = true)
            val success = runCatching { repository.syncPendingData() }.getOrDefault(false)
            syncActionState.value = SyncActionState(
                feedback = if (success) {
                    "Dados conferidos e sincronizados com a nuvem."
                } else {
                    "Não foi possível sincronizar agora. Seus dados continuam salvos no celular."
                }
            )
        }
    }

    fun createBackup(password: String, onReady: (Result<String>) -> Unit) {
        if (backupActionState.value.isBusy) return
        viewModelScope.launch {
            backupActionState.value = BackupActionState(isBusy = true)
            val result = repository.createEncryptedBackup(password)
            backupActionState.value = BackupActionState()
            onReady(result)
        }
    }

    fun restoreBackup(serialized: String, password: String) {
        if (backupActionState.value.isBusy) return
        viewModelScope.launch {
            backupActionState.value = BackupActionState(isBusy = true)
            val result = repository.restoreEncryptedBackup(serialized, password)
            backupActionState.value = BackupActionState(
                feedback = result.fold(
                    onSuccess = { summary ->
                        "Backup restaurado: ${summary.crops} safras, " +
                            "${summary.tasks} tarefas e ${summary.transactions} lançamentos."
                    },
                    onFailure = { error ->
                        error.message ?: "Não foi possível restaurar este backup."
                    }
                )
            )
        }
    }

    fun reportBackupFileSaved(success: Boolean, errorMessage: String? = null) {
        backupActionState.value = BackupActionState(
            feedback = if (success) {
                "Backup protegido salvo com sucesso. Guarde também a senha em local seguro."
            } else {
                errorMessage ?: "Não foi possível salvar o arquivo de backup."
            }
        )
    }

    fun reportBackupError(message: String) {
        backupActionState.value = BackupActionState(feedback = message)
    }

    fun changePassword(
        newPassword: String,
        confirmation: String,
        onFinished: (Boolean) -> Unit
    ) {
        val validationError = accountPasswordError(newPassword, confirmation)
        if (validationError != null) {
            passwordActionState.value = PasswordActionState(feedback = validationError)
            onFinished(false)
            return
        }
        if (passwordActionState.value.isBusy) return
        viewModelScope.launch {
            passwordActionState.value = PasswordActionState(isBusy = true)
            val result = repository.changePassword(newPassword)
            passwordActionState.value = PasswordActionState(
                feedback = result.fold(
                    onSuccess = { "Senha alterada com segurança." },
                    onFailure = { it.message ?: "Não foi possível trocar a senha." }
                )
            )
            onFinished(result.isSuccess)
        }
    }

    fun clearPasswordFeedback() {
        if (!passwordActionState.value.isBusy) passwordActionState.value = PasswordActionState()
    }

    private data class SyncActionState(
        val isSyncing: Boolean = false,
        val feedback: String? = null
    )

    private data class BackupActionState(
        val isBusy: Boolean = false,
        val feedback: String? = null
    )

    private data class PasswordActionState(
        val isBusy: Boolean = false,
        val feedback: String? = null
    )
}

class DashboardViewModelFactory(private val repository: AgroRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
