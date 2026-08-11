package com.agrogestao.pro.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agrogestao.pro.data.local.AgroDatabase
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.ProducerEntity
import com.agrogestao.pro.data.local.entities.TaskStatus
import com.agrogestao.pro.data.local.entities.TransactionType
import com.agrogestao.pro.data.security.SecureSessionStore
import com.agrogestao.pro.domain.DailyActivityRequest
import com.agrogestao.pro.domain.DailyActivityType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyActivityRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun oneQuickPurchaseFeedsHistoryAndCashAndCanBeUndone() = runBlocking {
        val database = newDatabase()
        try {
            prepareOwner(database)
            val repository = database.repository()

            val receipt = repository.recordDailyActivity(
                DailyActivityRequest(
                    type = DailyActivityType.BOUGHT,
                    dateIso = "2026-08-07",
                    amountCents = 12_550L,
                    note = "Sementes"
                )
            )

            val task = repository.allTasks.first().single()
            val transaction = repository.allTransactions.first().single()
            assertEquals(TaskStatus.CONCLUIDO, task.status)
            assertEquals("Compra", task.categoria)
            assertEquals("Compra: Sementes", task.titulo)
            assertEquals(12_550L, transaction.valorCentavos)
            assertEquals(TransactionType.SAIDA, transaction.tipo)
            assertEquals("Sementes", transaction.descricao)

            assertTrue(repository.undoDailyActivity(receipt))
            assertTrue(repository.allTasks.first().isEmpty())
            assertTrue(repository.allTransactions.first().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun plantingUpdatesTheSelectedAreaAndUndoRestoresIt() = runBlocking {
        val database = newDatabase()
        try {
            prepareOwner(database)
            val cropId = database.cropDao().insertCrop(sampleCrop())
            val crop = requireNotNull(database.cropDao().getById(cropId))
            val repository = database.repository()

            val receipt = repository.recordDailyActivity(
                DailyActivityRequest(
                    type = DailyActivityType.PLANTED,
                    dateIso = "2026-08-07",
                    note = "Feijão",
                    cropCloudId = crop.cloudId
                )
            )

            val planted = requireNotNull(database.cropDao().getById(cropId))
            assertEquals(10, planted.progressoPercentual)
            assertTrue(planted.statusManejo.startsWith("Plantio registrado"))

            assertTrue(repository.undoDailyActivity(receipt))
            val restored = requireNotNull(database.cropDao().getById(cropId))
            assertEquals(0, restored.progressoPercentual)
            assertEquals("Aguardando plantio", restored.statusManejo)
        } finally {
            database.close()
        }
    }

    private fun newDatabase(): AgroDatabase =
        Room.inMemoryDatabaseBuilder(context, AgroDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private suspend fun prepareOwner(database: AgroDatabase) {
        database.producerDao().insertOrUpdateProducer(
            ProducerEntity(
                nomeProdutor = "José",
                email = "",
                nomePropriedade = "Sítio Teste",
                municipioUF = "",
                dAPouCAF = "",
                areaTotalHectares = 2.0,
                isLoggedIn = true,
                remoteUserId = OWNER
            )
        )
    }

    private fun sampleCrop() = CropEntity(
        nomeCultura = "Feijão",
        areaHectares = 1.5,
        dataInicio = "2026-08-07",
        previsaoColheita = "2026-11-07",
        progressoPercentual = 0,
        statusManejo = "Aguardando plantio",
        ownerUserId = OWNER
    )

    private fun AgroDatabase.repository() = AgroRepository(
        backupDao = backupDao(),
        cropDao = cropDao(),
        taskDao = taskDao(),
        financialDao = financialDao(),
        producerDao = producerDao(),
        reportHistoryDao = reportHistoryDao(),
        reportConsentDao = reportConsentDao(),
        secureSessionStore = SecureSessionStore(context),
        syncConflictDao = syncConflictDao(),
        dailyActivityDao = dailyActivityDao()
    )

    private companion object {
        const val OWNER = "local:daily-activity-test"
    }
}
