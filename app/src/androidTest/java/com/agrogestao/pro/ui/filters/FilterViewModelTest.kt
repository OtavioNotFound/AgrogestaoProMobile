package com.agrogestao.pro.ui.filters

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agrogestao.pro.data.local.AgroDatabase
import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.data.local.entities.TaskStatus
import com.agrogestao.pro.data.local.entities.TransactionType
import com.agrogestao.pro.data.repository.AgroRepository
import com.agrogestao.pro.data.security.SecureSessionStore
import com.agrogestao.pro.domain.FinancialFilterCriteria
import com.agrogestao.pro.domain.TaskFilterCriteria
import com.agrogestao.pro.ui.kanban.KanbanViewModel
import com.agrogestao.pro.ui.relatorios.RelatorioCreditoViewModel
import com.agrogestao.pro.ui.relatorios.isCurrentCreditReportConsent
import com.agrogestao.pro.ui.safras.SafrasViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FilterViewModelTest {
    private lateinit var context: Context
    private lateinit var sessionStore: SecureSessionStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sessionStore = SecureSessionStore(context).also { it.clear() }
    }

    @After
    fun tearDown() {
        sessionStore.clear()
    }

    @Test
    fun kanbanFiltersUpdateListsCountsAndOptions() = runBlocking {
        val database = newDatabase()
        try {
            val repository = database.repository()
            repository.startOfflineProfile("Filtro", "Sítio", "PE", 2.0).getOrThrow()
            repository.insertTask(task("Adubar", "Adubação", "2026-08-10", TaskStatus.A_FAZER))
            repository.insertTask(task("Irrigar", "Irrigação", "2026-09-10", TaskStatus.A_FAZER))
            repository.insertTask(task("Colher", "Colheita", "2026-08-20", TaskStatus.CONCLUIDO))
            val viewModel = KanbanViewModel(repository)

            val initial = withTimeout(5_000) {
                viewModel.uiState.first { it.totalAFazer == 2 && it.totalConcluido == 1 }
            }
            assertEquals(3, initial.categories.size)

            viewModel.applyFilters(
                TaskFilterCriteria(toDate = "2026-08-31", category = "Adubação")
            )
            val filtered = withTimeout(5_000) {
                viewModel.uiState.first { it.filters.activeCount == 2 }
            }
            assertEquals(listOf("Adubar"), filtered.aFazer.map(TaskEntity::titulo))
            assertTrue(filtered.concluido.isEmpty())
            assertEquals(2, filtered.totalAFazer)
        } finally {
            database.close()
        }
    }

    @Test
    fun financialFiltersRecalculateVisibleBalance() = runBlocking {
        val database = newDatabase()
        try {
            val repository = database.repository()
            repository.startOfflineProfile("Caixa", "Sítio", "BA", 4.0).getOrThrow()
            repository.insertTransaction(transaction("Venda", 500.0, TransactionType.ENTRADA, "2026-08-10"))
            repository.insertTransaction(transaction("Adubo", 120.0, TransactionType.SAIDA, "2026-08-11"))
            repository.insertTransaction(transaction("Venda antiga", 200.0, TransactionType.ENTRADA, "2026-07-01"))
            val viewModel = SafrasViewModel(repository)

            withTimeout(5_000) { viewModel.uiState.first { it.totalTransactions == 3 } }
            viewModel.applyFinancialFilters(
                FinancialFilterCriteria(
                    fromDate = "2026-08-01",
                    transactionType = TransactionType.ENTRADA
                )
            )
            val filtered = withTimeout(5_000) {
                viewModel.uiState.first { it.filters.activeCount == 2 }
            }

            assertEquals(listOf("Venda"), filtered.transacoes.map(FinancialEntity::descricao))
            assertEquals(500.0, filtered.filteredIncome, 0.0)
            assertEquals(0.0, filtered.filteredExpenses, 0.0)
            assertEquals(500.0, filtered.filteredBalance, 0.0)
            assertEquals(3, filtered.totalTransactions)
        } finally {
            database.close()
        }
    }

    @Test
    fun creditReportPeriodRecalculatesValuesAndRejectsInvertedDates() = runBlocking {
        val database = newDatabase()
        try {
            val repository = database.repository()
            repository.startOfflineProfile("Crédito", "Sítio", "PE", 4.0).getOrThrow()
            val producer = repository.producerProfile.first { it != null }!!
            repository.saveProducerProfile(producer.copy(dAPouCAF = "CAF-123"))
            repository.insertCrop(
                CropEntity(
                    nomeCultura = "Milho",
                    areaHectares = 2.0,
                    dataInicio = "2026-01-10",
                    previsaoColheita = "2026-06-10",
                    progressoPercentual = 100,
                    statusManejo = "Concluído"
                )
            )
            repository.insertTransaction(
                transaction("Venda julho", 900.0, TransactionType.ENTRADA, "2026-07-10")
            )
            repository.insertTransaction(
                transaction("Insumo julho", 200.0, TransactionType.SAIDA, "2026-07-20")
            )
            repository.insertTransaction(
                transaction("Venda agosto", 500.0, TransactionType.ENTRADA, "2026-08-01")
            )
            val viewModel = RelatorioCreditoViewModel(repository)

            viewModel.updateReportPeriod("2026-07-01", "2026-07-31")
            val july = withTimeout(5_000) {
                viewModel.uiState.first { it.report?.transactions?.size == 2 }
            }.report!!

            assertEquals(900.0, july.financialSummary.income, 0.0)
            assertEquals(200.0, july.financialSummary.expenses, 0.0)
            assertEquals(700.0, july.financialSummary.balance, 0.0)
            assertTrue(july.completeness.isComplete)

            assertTrue(runCatching {
                viewModel.generateAndArchivePdf(context, july)
            }.isFailure)

            val archivedFile = viewModel.grantConsentAndGeneratePdf(context, july)
            val history = withTimeout(5_000) {
                viewModel.uiState.first {
                    it.history.size == 1 && it.consent.isCurrentCreditReportConsent
                }.history.single()
            }
            assertTrue(archivedFile.isFile)
            assertEquals(700.0, history.balance, 0.0)
            assertEquals(1, history.consentVersion)
            assertTrue(history.consentAcceptedAtEpochMillis > 0L)
            assertTrue(viewModel.archivedFileForSharing(context, history).isSuccess)

            assertTrue(viewModel.revokeReportConsent().isSuccess)
            withTimeout(5_000) {
                viewModel.uiState.first { !it.consent.isCurrentCreditReportConsent }
            }
            assertTrue(viewModel.archivedFileForSharing(context, history).isFailure)
            assertTrue(archivedFile.isFile)
            assertTrue(viewModel.grantConsentAndGetArchivedFile(context, history).isSuccess)

            assertTrue(viewModel.deleteArchivedReport(context, history).isSuccess)
            withTimeout(5_000) { viewModel.uiState.first { it.history.isEmpty() } }
            assertTrue(!archivedFile.exists())

            viewModel.updateReportPeriod("2026-08-31", "2026-08-01")
            val invalid = withTimeout(5_000) {
                viewModel.uiState.first { !it.reportCriteria.isValid }
            }
            assertEquals(null, invalid.report)
        } finally {
            database.close()
        }
    }

    private fun task(
        title: String,
        category: String,
        date: String,
        status: TaskStatus
    ) = TaskEntity(
        titulo = title,
        descricao = "Teste",
        categoria = category,
        dataLimite = date,
        status = status
    )

    private fun transaction(
        description: String,
        value: Double,
        type: TransactionType,
        date: String
    ) = FinancialEntity(
        descricao = description,
        valor = value,
        tipo = type,
        data = date,
        categoria = if (type == TransactionType.ENTRADA) "Venda" else "Insumos"
    )

    private fun newDatabase(): AgroDatabase =
        Room.inMemoryDatabaseBuilder(context, AgroDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun AgroDatabase.repository() = AgroRepository(
        backupDao = backupDao(),
        cropDao = cropDao(),
        taskDao = taskDao(),
        financialDao = financialDao(),
        producerDao = producerDao(),
        reportHistoryDao = reportHistoryDao(),
        reportConsentDao = reportConsentDao(),
        secureSessionStore = sessionStore
    )
}
