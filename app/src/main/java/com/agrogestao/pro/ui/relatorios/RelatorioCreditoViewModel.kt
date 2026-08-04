package com.agrogestao.pro.ui.relatorios

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.ProducerEntity
import com.agrogestao.pro.data.local.entities.ReportHistoryEntity
import com.agrogestao.pro.data.local.entities.ReportConsentEntity
import com.agrogestao.pro.data.report.CreditReportArchiveStore
import com.agrogestao.pro.data.report.ReportIntegrityStatus
import com.agrogestao.pro.data.repository.AgroRepository
import com.agrogestao.pro.domain.CreditReportCriteria
import com.agrogestao.pro.domain.CreditReportSnapshot
import com.agrogestao.pro.domain.CURRENT_CREDIT_REPORT_CONSENT_VERSION
import com.agrogestao.pro.domain.buildCreditReportSnapshot
import com.agrogestao.pro.domain.todayIso
import com.agrogestao.pro.domain.todayPlusMonthsIso
import com.agrogestao.pro.domain.withConsentProof
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class RelatorioUiState(
    val producer: ProducerEntity? = null,
    val safras: List<CropEntity> = emptyList(),
    val reportCriteria: CreditReportCriteria = defaultCreditReportCriteria(),
    val report: CreditReportSnapshot? = null,
    val history: List<ReportHistoryEntity> = emptyList(),
    val consent: ReportConsentEntity? = null
)

val ReportConsentEntity?.isCurrentCreditReportConsent: Boolean
    get() = this?.isGranted == true &&
        consentVersion == CURRENT_CREDIT_REPORT_CONSENT_VERSION &&
        acceptedAtEpochMillis > 0L &&
        revokedAtEpochMillis == null

private data class ReportLocalState(
    val history: List<ReportHistoryEntity>,
    val consent: ReportConsentEntity?
)

class RelatorioCreditoViewModel(private val repository: AgroRepository) : ViewModel() {
    private val reportCriteria = MutableStateFlow(defaultCreditReportCriteria())
    private val reportLocalState = combine(
        repository.reportHistory,
        repository.reportConsent
    ) { history, consent ->
        ReportLocalState(history, consent)
    }

    val uiState: StateFlow<RelatorioUiState> = combine(
        repository.producerProfile,
        repository.allCrops,
        repository.allTransactions,
        reportCriteria,
        reportLocalState
    ) { producer, crops, transactions, criteria, localState ->
        RelatorioUiState(
            producer = producer,
            safras = crops,
            reportCriteria = criteria,
            report = criteria.takeIf(CreditReportCriteria::isValid)?.let {
                buildCreditReportSnapshot(
                    producer = producer,
                    crops = crops,
                    transactions = transactions,
                    criteria = it
                )
            },
            history = localState.history,
            consent = localState.consent
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RelatorioUiState()
    )

    fun updateReportPeriod(fromDate: String, toDate: String) {
        reportCriteria.value = CreditReportCriteria(fromDate, toDate)
    }

    suspend fun grantConsentAndGeneratePdf(
        context: Context,
        report: CreditReportSnapshot
    ): File = withContext(Dispatchers.IO) {
        val consent = repository.grantReportConsent(
            consentVersion = CURRENT_CREDIT_REPORT_CONSENT_VERSION
        )
        archivePdf(context, report, consent)
    }

    suspend fun generateAndArchivePdf(
        context: Context,
        report: CreditReportSnapshot
    ): File = withContext(Dispatchers.IO) {
        val consent = uiState.value.consent.takeIf {
            it.isCurrentCreditReportConsent
        } ?: error("Autorize o uso dos dados antes de gerar o relatório.")
        archivePdf(context, report, consent)
    }

    private suspend fun archivePdf(
        context: Context,
        report: CreditReportSnapshot,
        consent: ReportConsentEntity
    ): File {
        val ownerUserId = report.producer?.remoteUserId.orEmpty()
        val consentedReport = report.withConsentProof(
            consentVersion = consent.consentVersion,
            acceptedAtEpochMillis = consent.acceptedAtEpochMillis
        )
        val archived = CreditReportArchiveStore.create(context, ownerUserId, consentedReport)
        return try {
            repository.saveReportHistory(archived.history)
            archived.file
        } catch (error: Throwable) {
            CreditReportArchiveStore.deleteFile(context, archived.history)
            throw error
        }
    }

    suspend fun archivedFileForSharing(
        context: Context,
        history: ReportHistoryEntity
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            check(uiState.value.consent.isCurrentCreditReportConsent) {
                "Autorize o compartilhamento novamente antes de continuar."
            }
            verifiedArchivedFile(context, history)
        }
    }

    suspend fun grantConsentAndGetArchivedFile(
        context: Context,
        history: ReportHistoryEntity
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            repository.grantReportConsent(CURRENT_CREDIT_REPORT_CONSENT_VERSION)
            verifiedArchivedFile(context, history)
        }
    }

    suspend fun grantReportConsent(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            repository.grantReportConsent(CURRENT_CREDIT_REPORT_CONSENT_VERSION)
        }.map { Unit }
    }

    suspend fun revokeReportConsent(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            check(repository.revokeReportConsent()) {
                "Nenhum consentimento ativo foi encontrado."
            }
        }
    }

    private fun verifiedArchivedFile(
        context: Context,
        history: ReportHistoryEntity
    ): File {
        check(
            history.consentVersion == CURRENT_CREDIT_REPORT_CONSENT_VERSION &&
                history.consentAcceptedAtEpochMillis > 0L
        ) {
            "Este PDF foi criado antes do registro de consentimento. Gere um novo relatório."
        }
        return when (CreditReportArchiveStore.verify(context, history)) {
            ReportIntegrityStatus.VALID -> CreditReportArchiveStore.resolve(context, history)
            ReportIntegrityStatus.MISSING -> error(
                "O arquivo deste relatório não está mais no celular."
            )
            ReportIntegrityStatus.MODIFIED -> error(
                "O arquivo foi alterado e não será compartilhado."
            )
        }
    }

    suspend fun deleteArchivedReport(
        context: Context,
        history: ReportHistoryEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            check(CreditReportArchiveStore.deleteFile(context, history)) {
                "Não foi possível remover o arquivo do relatório."
            }
            check(repository.deleteReportHistory(history.reportId)) {
                "O relatório não pertence à conta atual."
            }
        }
    }

    fun updateProducerInfo(
        nome: String,
        propriedade: String,
        municipio: String,
        caf: String,
        area: Double
    ) {
        viewModelScope.launch {
            val current = uiState.value.producer ?: return@launch
            repository.saveProducerProfile(
                current.copy(
                    nomeProdutor = nome,
                    nomePropriedade = propriedade,
                    municipioUF = municipio,
                    dAPouCAF = caf,
                    areaTotalHectares = area
                )
            )
        }
    }
}

private fun defaultCreditReportCriteria() = CreditReportCriteria(
    fromDate = todayPlusMonthsIso(-12),
    toDate = todayIso()
)

class RelatorioCreditoViewModelFactory(private val repository: AgroRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RelatorioCreditoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RelatorioCreditoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
