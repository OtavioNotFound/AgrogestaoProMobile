package com.agrogestao.pro.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.agrogestao.pro.data.local.AgroDatabase
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.data.local.entities.TaskStatus
import com.agrogestao.pro.data.local.entities.TransactionType
import com.agrogestao.pro.data.remote.SupabaseConfig
import com.agrogestao.pro.data.repository.AgroRepository
import com.agrogestao.pro.data.security.SecureSessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveSupabaseSyncTest {
    @Test
    fun dataRoundTripsBetweenTwoLocalDatabases() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        val email = arguments.getString("liveSupabaseEmail").orEmpty()
        val password = arguments.getString("liveSupabasePassword").orEmpty()
        assumeTrue("Credenciais temporárias não fornecidas", email.isNotBlank() && password.isNotBlank())

        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionStore = SecureSessionStore(context).also { it.clear() }
        val firstDatabase = newDatabase(context)
        val secondDatabase = newDatabase(context)
        try {
            val firstRepository = firstDatabase.repository(sessionStore)
            val firstSignIn = firstRepository.signIn(email, password)
            assertTrue(firstSignIn.exceptionOrNull()?.message, firstSignIn.isSuccess)

            firstRepository.insertCrop(
                CropEntity(
                    nomeCultura = "Milho teste nuvem",
                    areaHectares = 3.5,
                    dataInicio = "2026-08-02",
                    previsaoColheita = "2026-12-10",
                    progressoPercentual = 15,
                    statusManejo = "Plantio concluído"
                )
            )
            val ownerUserId = firstDatabase.producerDao()
                .getProducerProfileOnce()
                ?.remoteUserId
                .orEmpty()
            val crop = firstDatabase.cropDao().getAllCrops(ownerUserId).first().single()
            firstRepository.insertTask(
                TaskEntity(
                    titulo = "Irrigar lote teste",
                    descricao = "Validação da sincronização",
                    categoria = "Irrigação",
                    dataLimite = "2026-08-10",
                    status = TaskStatus.A_FAZER,
                    cropCloudId = crop.cloudId
                )
            )
            firstRepository.insertTransaction(
                FinancialEntity(
                    descricao = "Semente teste",
                    valor = 125.50,
                    tipo = TransactionType.SAIDA,
                    data = "2026-08-02",
                    categoria = "Insumos",
                    cropCloudId = crop.cloudId
                )
            )
            val task = firstDatabase.taskDao().getAllTasks(ownerUserId).first().single()
            val transaction = firstDatabase.financialDao()
                .getAllTransactions(ownerUserId)
                .first()
                .single()
            firstRepository.updateCrop(
                crop.copy(
                    nomeCultura = "Milho editado na nuvem",
                    progressoPercentual = 75,
                    statusManejo = "Colheita próxima"
                )
            )
            firstRepository.updateTask(
                task.copy(
                    titulo = "Irrigar lote editado",
                    status = TaskStatus.EM_PROGRESSO
                )
            )
            firstRepository.updateTransaction(
                transaction.copy(
                    descricao = "Venda teste editada",
                    valor = 225.75,
                    tipo = TransactionType.ENTRADA,
                    categoria = "Venda"
                )
            )
            assertTrue(firstRepository.syncPendingData())
            assertEquals(
                SupabaseConfig.STATUS_SYNCED_CLOUD,
                firstDatabase.producerDao().getProducerProfileOnce()?.syncStatus
            )
            val encryptedBackup = firstRepository
                .createEncryptedBackup("backup-nuvem-123")
                .getOrThrow()

            val secondRepository = secondDatabase.repository(sessionStore)
            val secondSignIn = secondRepository.signIn(email, password)
            assertTrue(secondSignIn.exceptionOrNull()?.message, secondSignIn.isSuccess)
            assertTrue(secondRepository.syncPendingData())

            val secondOwner = secondDatabase.producerDao()
                .getProducerProfileOnce()
                ?.remoteUserId
                .orEmpty()
            val downloadedCrops = secondDatabase.cropDao().getAllCrops(secondOwner).first()
            val downloadedTasks = secondDatabase.taskDao().getAllTasks(secondOwner).first()
            val downloadedTransactions = secondDatabase.financialDao()
                .getAllTransactions(secondOwner)
                .first()
            assertEquals(1, downloadedCrops.size)
            assertEquals(1, downloadedTasks.size)
            assertEquals(1, downloadedTransactions.size)
            assertEquals("Milho editado na nuvem", downloadedCrops.single().nomeCultura)
            assertEquals(75, downloadedCrops.single().progressoPercentual)
            assertEquals("Irrigar lote editado", downloadedTasks.single().titulo)
            assertEquals(TaskStatus.EM_PROGRESSO, downloadedTasks.single().status)
            assertEquals("Venda teste editada", downloadedTransactions.single().descricao)
            assertEquals(225.75, downloadedTransactions.single().valor, 0.0)
            assertEquals(TransactionType.ENTRADA, downloadedTransactions.single().tipo)

            secondRepository.updateCrop(
                downloadedCrops.single().copy(nomeCultura = "Alteração posterior ao backup")
            )
            assertTrue(secondRepository.syncPendingData())
            assertTrue(
                secondRepository.restoreEncryptedBackup(
                    encryptedBackup,
                    "backup-nuvem-123"
                ).isSuccess
            )
            assertTrue(secondRepository.syncPendingData())
            assertTrue(firstRepository.syncPendingData())
            assertEquals(
                "Milho editado na nuvem",
                firstDatabase.cropDao().getAllCrops(ownerUserId).first().single().nomeCultura
            )

            val restoredCrops = secondDatabase.cropDao().getAllCrops(secondOwner).first()
            val restoredTasks = secondDatabase.taskDao().getAllTasks(secondOwner).first()
            val restoredTransactions = secondDatabase.financialDao()
                .getAllTransactions(secondOwner).first()
            secondRepository.deleteTask(restoredTasks.single().id)
            secondRepository.deleteTransaction(restoredTransactions.single().id)
            secondRepository.deleteCrop(restoredCrops.single().id)
            assertTrue(secondRepository.syncPendingData())
            assertTrue(firstRepository.syncPendingData())
            assertTrue(firstDatabase.cropDao().getAllCrops(ownerUserId).first().isEmpty())
            assertTrue(firstDatabase.taskDao().getAllTasks(ownerUserId).first().isEmpty())
            assertTrue(firstDatabase.financialDao().getAllTransactions(ownerUserId).first().isEmpty())
        } finally {
            firstDatabase.close()
            secondDatabase.close()
            sessionStore.clear()
        }
    }

    private fun newDatabase(context: Context): AgroDatabase =
        Room.inMemoryDatabaseBuilder(context, AgroDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun AgroDatabase.repository(sessionStore: SecureSessionStore) = AgroRepository(
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
