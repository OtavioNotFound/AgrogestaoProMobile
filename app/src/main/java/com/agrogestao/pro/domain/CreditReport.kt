package com.agrogestao.pro.domain

import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.ProducerEntity
import com.agrogestao.pro.data.remote.SupabaseConfig

const val CREDIT_REPORT_ORIGIN =
    "Registros cadastrados pelo produtor no AgroGestão Pro."

const val CREDIT_REPORT_DISCLAIMER =
    "Este relatório é apenas informativo, não é documento oficial, não substitui CAF, DAP, " +
        "projeto técnico ou comprovantes e não garante aprovação de crédito. A instituição " +
        "financeira é responsável pela conferência dos dados e pela decisão."

const val CREDIT_REPORT_FORMAT_VERSION = 2
const val CURRENT_CREDIT_REPORT_CONSENT_VERSION = 1
const val CREDIT_REPORT_CONSENT_PURPOSE =
    "Gerar um resumo com dados do produtor, da propriedade, das safras e das movimentações " +
        "financeiras do período, para compartilhamento voluntário com uma instituição " +
        "financeira ou outra pessoa escolhida pelo produtor. O app não envia o relatório " +
        "automaticamente."

data class CreditReportCriteria(
    val fromDate: String,
    val toDate: String
) {
    val isValid: Boolean
        get() = isoDateParts(fromDate) != null &&
            isoDateParts(toDate) != null &&
            fromDate <= toDate
}

data class CreditReportCompleteness(
    val missingItems: List<String>
) {
    val isComplete: Boolean
        get() = missingItems.isEmpty()
}

data class CreditReportSyncSummary(
    val syncedRecords: Int,
    val localOrPendingRecords: Int
) {
    val totalRecords: Int
        get() = syncedRecords + localOrPendingRecords
}

data class CreditReportConsentProof(
    val consentVersion: Int,
    val acceptedAtEpochMillis: Long,
    val purpose: String = CREDIT_REPORT_CONSENT_PURPOSE
) {
    val isValid: Boolean
        get() = consentVersion == CURRENT_CREDIT_REPORT_CONSENT_VERSION &&
            acceptedAtEpochMillis > 0L &&
            purpose.isNotBlank()
}

data class CreditReportSnapshot(
    val producer: ProducerEntity?,
    val crops: List<CropEntity>,
    val transactions: List<FinancialEntity>,
    val criteria: CreditReportCriteria,
    val financialSummary: FinancialSummary,
    val completeness: CreditReportCompleteness,
    val syncSummary: CreditReportSyncSummary,
    val generatedDate: String,
    val formatVersion: Int = CREDIT_REPORT_FORMAT_VERSION,
    val consentProof: CreditReportConsentProof? = null,
    val dataOrigin: String = CREDIT_REPORT_ORIGIN,
    val disclaimer: String = CREDIT_REPORT_DISCLAIMER
)

fun CreditReportSnapshot.withConsentProof(
    consentVersion: Int,
    acceptedAtEpochMillis: Long
): CreditReportSnapshot = copy(
    consentProof = CreditReportConsentProof(
        consentVersion = consentVersion,
        acceptedAtEpochMillis = acceptedAtEpochMillis
    )
)

fun buildCreditReportSnapshot(
    producer: ProducerEntity?,
    crops: List<CropEntity>,
    transactions: List<FinancialEntity>,
    criteria: CreditReportCriteria,
    generatedDate: String = todayIso()
): CreditReportSnapshot {
    require(criteria.isValid) { "Período do relatório inválido." }

    val periodTransactions = filterTransactions(
        transactions,
        FinancialFilterCriteria(
            fromDate = criteria.fromDate,
            toDate = criteria.toDate
        )
    )
    val includedStatuses = buildList {
        producer?.let { add(it.syncStatus) }
        addAll(crops.map(CropEntity::syncStatus))
        addAll(periodTransactions.map(FinancialEntity::syncStatus))
    }
    val syncedRecords = includedStatuses.count {
        it == SupabaseConfig.STATUS_SYNCED_CLOUD
    }

    return CreditReportSnapshot(
        producer = producer,
        crops = crops,
        transactions = periodTransactions,
        criteria = criteria,
        financialSummary = calculateFinancialSummary(periodTransactions),
        completeness = CreditReportCompleteness(
            missingItems = missingCreditReportItems(producer, crops, periodTransactions)
        ),
        syncSummary = CreditReportSyncSummary(
            syncedRecords = syncedRecords,
            localOrPendingRecords = includedStatuses.size - syncedRecords
        ),
        generatedDate = generatedDate
    )
}

private fun missingCreditReportItems(
    producer: ProducerEntity?,
    crops: List<CropEntity>,
    transactions: List<FinancialEntity>
): List<String> = buildList {
    if (producer?.nomeProdutor.isNullOrBlank()) add("Nome do produtor")
    if (producer?.nomePropriedade.isNullOrBlank()) add("Nome da propriedade")
    if (producer?.municipioUF.isNullOrBlank()) add("Município e estado")
    if (producer?.dAPouCAF.isNullOrBlank()) add("CAF ou DAP")
    if (producer == null || producer.areaTotalHectares <= 0.0) add("Área total da propriedade")
    if (crops.isEmpty()) add("Ao menos uma safra")
    if (transactions.isEmpty()) add("Movimentações financeiras no período")
}
