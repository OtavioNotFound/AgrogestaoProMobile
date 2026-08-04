package com.agrogestao.pro.domain

import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.ProducerEntity
import com.agrogestao.pro.data.local.entities.TransactionType
import com.agrogestao.pro.data.remote.SupabaseConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditReportTest {
    @Test
    fun `report includes only movements inside inclusive period`() {
        val report = buildCreditReportSnapshot(
            producer = producer(),
            crops = listOf(crop()),
            transactions = listOf(
                transaction(1, "2026-01-01", 1_000.0, TransactionType.ENTRADA),
                transaction(2, "2026-01-31", 250.0, TransactionType.SAIDA),
                transaction(3, "2026-02-01", 5_000.0, TransactionType.ENTRADA)
            ),
            criteria = CreditReportCriteria("2026-01-01", "2026-01-31"),
            generatedDate = "2026-02-02"
        )

        assertEquals(listOf(1L, 2L), report.transactions.map(FinancialEntity::id))
        assertEquals(1_000.0, report.financialSummary.income, 0.001)
        assertEquals(250.0, report.financialSummary.expenses, 0.001)
        assertEquals(750.0, report.financialSummary.balance, 0.001)
        assertTrue(report.completeness.isComplete)
    }

    @Test
    fun `report explains every missing item instead of claiming completeness`() {
        val report = buildCreditReportSnapshot(
            producer = producer(
                name = "",
                property = "",
                location = "",
                caf = "",
                area = 0.0
            ),
            crops = emptyList(),
            transactions = emptyList(),
            criteria = CreditReportCriteria("2026-01-01", "2026-12-31")
        )

        assertFalse(report.completeness.isComplete)
        assertEquals(
            listOf(
                "Nome do produtor",
                "Nome da propriedade",
                "Município e estado",
                "CAF ou DAP",
                "Área total da propriedade",
                "Ao menos uma safra",
                "Movimentações financeiras no período"
            ),
            report.completeness.missingItems
        )
    }

    @Test
    fun `sync summary separates cloud records from local or pending records`() {
        val report = buildCreditReportSnapshot(
            producer = producer(syncStatus = SupabaseConfig.STATUS_SYNCED_CLOUD),
            crops = listOf(crop(syncStatus = SupabaseConfig.STATUS_LOCAL_OFFLINE)),
            transactions = listOf(
                transaction(
                    id = 1,
                    date = "2026-06-01",
                    value = 10.0,
                    type = TransactionType.ENTRADA,
                    syncStatus = SupabaseConfig.STATUS_SYNCED_CLOUD
                )
            ),
            criteria = CreditReportCriteria("2026-01-01", "2026-12-31")
        )

        assertEquals(2, report.syncSummary.syncedRecords)
        assertEquals(1, report.syncSummary.localOrPendingRecords)
        assertEquals(3, report.syncSummary.totalRecords)
    }

    @Test
    fun `consent proof is explicit versioned and attached without changing report data`() {
        val report = buildCreditReportSnapshot(
            producer = producer(),
            crops = listOf(crop()),
            transactions = emptyList(),
            criteria = CreditReportCriteria("2026-01-01", "2026-12-31")
        )

        assertEquals(CREDIT_REPORT_FORMAT_VERSION, report.formatVersion)
        assertEquals(null, report.consentProof)

        val consented = report.withConsentProof(
            consentVersion = CURRENT_CREDIT_REPORT_CONSENT_VERSION,
            acceptedAtEpochMillis = 1_234L
        )

        assertTrue(consented.consentProof!!.isValid)
        assertEquals(CREDIT_REPORT_CONSENT_PURPOSE, consented.consentProof!!.purpose)
        assertEquals(report.financialSummary, consented.financialSummary)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `report rejects inverted period`() {
        buildCreditReportSnapshot(
            producer = producer(),
            crops = listOf(crop()),
            transactions = emptyList(),
            criteria = CreditReportCriteria("2026-12-31", "2026-01-01")
        )
    }

    private fun producer(
        name: String = "Maria da Silva",
        property: String = "Sítio Esperança",
        location: String = "Petrolina - PE",
        caf: String = "CAF-123",
        area: Double = 12.5,
        syncStatus: String = SupabaseConfig.STATUS_SYNCED_CLOUD
    ) = ProducerEntity(
        nomeProdutor = name,
        email = "maria@example.com",
        nomePropriedade = property,
        municipioUF = location,
        dAPouCAF = caf,
        areaTotalHectares = area,
        syncStatus = syncStatus
    )

    private fun crop(
        syncStatus: String = SupabaseConfig.STATUS_SYNCED_CLOUD
    ) = CropEntity(
        id = 1,
        nomeCultura = "Milho",
        areaHectares = 4.5,
        dataInicio = "2026-01-10",
        previsaoColheita = "2026-05-10",
        progressoPercentual = 60,
        statusManejo = "Em andamento",
        syncStatus = syncStatus
    )

    private fun transaction(
        id: Long,
        date: String,
        value: Double,
        type: TransactionType,
        syncStatus: String = SupabaseConfig.STATUS_SYNCED_CLOUD
    ) = FinancialEntity(
        id = id,
        descricao = "Movimentação $id",
        valor = value,
        tipo = type,
        data = date,
        categoria = if (type == TransactionType.ENTRADA) "Venda" else "Insumo",
        syncStatus = syncStatus
    )
}
