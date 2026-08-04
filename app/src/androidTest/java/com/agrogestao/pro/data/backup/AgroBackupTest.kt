package com.agrogestao.pro.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agrogestao.pro.data.local.AgroDatabase
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.data.local.entities.TaskStatus
import com.agrogestao.pro.data.local.entities.TransactionType
import com.agrogestao.pro.data.repository.AgroRepository
import com.agrogestao.pro.data.security.SecureSessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AgroBackupTest {
    private lateinit var context: Context
    private lateinit var sessionStore: SecureSessionStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sessionStore = SecureSessionStore(context)
        sessionStore.clear()
    }

    @After
    fun tearDown() {
        sessionStore.clear()
    }

    @Test
    fun encryptedCodecRoundTripsWithoutExposingPersonalData() {
        val snapshot = sampleSnapshot()
        val serialized = AgroBackupCodec.encode(snapshot, PASSWORD)

        assertFalse(serialized.contains("Maria Segura"))
        assertFalse(serialized.contains("maria@example.com"))
        assertFalse(serialized.contains("Venda confidencial"))
        val decoded = AgroBackupCodec.decode(serialized, PASSWORD)
        assertEquals(snapshot.ownerUserId, decoded.ownerUserId)
        assertEquals(snapshot.ownerEmail, decoded.ownerEmail)
        assertEquals(snapshot.producer, decoded.producer)
        assertEquals(snapshot.exportedAtEpochMillis, decoded.exportedAtEpochMillis)
        assertEquals(snapshot.crops.single().cloudId, decoded.crops.single().cloudId)
        assertEquals(snapshot.crops.single().nomeCultura, decoded.crops.single().nomeCultura)
        assertEquals(snapshot.tasks.single().cloudId, decoded.tasks.single().cloudId)
        assertEquals(snapshot.tasks.single().cropCloudId, decoded.tasks.single().cropCloudId)
        assertEquals(
            snapshot.transactions.single().cloudId,
            decoded.transactions.single().cloudId
        )
    }

    @Test
    fun wrongPasswordAndTamperedFileAreRejected() {
        val serialized = AgroBackupCodec.encode(sampleSnapshot(), PASSWORD)

        assertTrue(
            runCatching { AgroBackupCodec.decode(serialized, "senha-errada") }
                .exceptionOrNull() is BackupException
        )

        val envelope = JSONObject(serialized)
        val ciphertext = envelope.getString("ciphertext")
        val replacement = if (ciphertext.last() == 'A') 'B' else 'A'
        envelope.put("ciphertext", ciphertext.dropLast(1) + replacement)
        assertTrue(
            runCatching { AgroBackupCodec.decode(envelope.toString(), PASSWORD) }
                .exceptionOrNull() is BackupException
        )
    }

    @Test
    fun repositoryRestoreMergesRecordsAndPreservesCloudAssociations() = runBlocking {
        val database = newDatabase()
        try {
            val repository = database.repository()
            assertTrue(
                repository.startOfflineProfile(
                    nome = "Maria original",
                    propriedade = "Sítio original",
                    municipio = "Petrolina - PE",
                    area = 9.0
                ).isSuccess
            )
            val owner = database.producerDao().getProducerProfileOnce()?.remoteUserId.orEmpty()
            repository.insertCrop(sampleSnapshot().crops.single())
            val crop = database.cropDao().getAllCrops(owner).first().single()
            repository.insertTask(sampleSnapshot().tasks.single().copy(cropCloudId = crop.cloudId))
            repository.insertTransaction(
                sampleSnapshot().transactions.single().copy(cropCloudId = crop.cloudId)
            )

            val originalTask = database.taskDao().getAllTasks(owner).first().single()
            val originalTransaction = database.financialDao()
                .getAllTransactions(owner).first().single()
            val backup = repository.createEncryptedBackup(PASSWORD).getOrThrow()

            repository.updateCrop(crop.copy(nomeCultura = "Safra alterada"))
            repository.updateTask(originalTask.copy(titulo = "Tarefa alterada"))
            repository.deleteTransaction(originalTransaction.id)
            val summary = repository.restoreEncryptedBackup(backup, PASSWORD).getOrThrow()

            assertEquals(BackupRestoreSummary(1, 1, 1), summary)
            val restoredCrop = database.cropDao().getAllCrops(owner).first().single()
            val restoredTask = database.taskDao().getAllTasks(owner).first().single()
            val restoredTransaction = database.financialDao()
                .getAllTransactions(owner).first().single()
            assertEquals("Milho", restoredCrop.nomeCultura)
            assertEquals(crop.cloudId, restoredCrop.cloudId)
            assertEquals(originalTask.cloudId, restoredTask.cloudId)
            assertEquals(restoredCrop.cloudId, restoredTask.cropCloudId)
            assertEquals(originalTransaction.cloudId, restoredTransaction.cloudId)
            assertEquals(restoredCrop.cloudId, restoredTransaction.cropCloudId)
            assertEquals(owner, restoredCrop.ownerUserId)
        } finally {
            database.close()
        }
    }

    @Test
    fun repositoryRejectsBackupFromAnotherAccountWithoutChangingData() = runBlocking {
        val firstDatabase = newDatabase()
        val secondDatabase = newDatabase()
        try {
            val firstRepository = firstDatabase.repository()
            val secondRepository = secondDatabase.repository()
            assertTrue(firstRepository.startOfflineProfile("Um", "Sítio Um", "BA", 1.0).isSuccess)
            assertTrue(secondRepository.startOfflineProfile("Dois", "Sítio Dois", "PE", 2.0).isSuccess)
            firstRepository.insertCrop(sampleSnapshot().crops.single())
            val backup = firstRepository.createEncryptedBackup(PASSWORD).getOrThrow()
            val secondOwner = secondDatabase.producerDao()
                .getProducerProfileOnce()?.remoteUserId.orEmpty()

            val result = secondRepository.restoreEncryptedBackup(backup, PASSWORD)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("outra conta") == true)
            assertTrue(secondDatabase.cropDao().getAllCrops(secondOwner).first().isEmpty())
            assertEquals("Dois", secondDatabase.producerDao().getProducerProfileOnce()?.nomeProdutor)
        } finally {
            firstDatabase.close()
            secondDatabase.close()
        }
    }

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

    private fun sampleSnapshot(): BackupSnapshot {
        val owner = "00000000-0000-4000-8000-000000000001"
        val cropId = "00000000-0000-4000-8000-000000000011"
        return BackupSnapshot(
            ownerUserId = owner,
            ownerEmail = "maria@example.com",
            producer = ProducerBackup(
                nomeProdutor = "Maria Segura",
                nomePropriedade = "Sítio Verde",
                municipioUF = "Petrolina - PE",
                dAPouCAF = "CAF-123",
                areaTotalHectares = 12.5
            ),
            crops = listOf(
                CropEntity(
                    nomeCultura = "Milho",
                    areaHectares = 3.0,
                    dataInicio = "2026-08-01",
                    previsaoColheita = "2026-12-10",
                    progressoPercentual = 20,
                    statusManejo = "Irrigação ativa",
                    cloudId = cropId,
                    ownerUserId = owner
                )
            ),
            tasks = listOf(
                TaskEntity(
                    titulo = "Adubar milho",
                    descricao = "Aplicar composto",
                    categoria = "Adubação",
                    dataLimite = "2026-08-20",
                    status = TaskStatus.A_FAZER,
                    cloudId = "00000000-0000-4000-8000-000000000012",
                    ownerUserId = owner,
                    cropCloudId = cropId
                )
            ),
            transactions = listOf(
                FinancialEntity(
                    descricao = "Venda confidencial",
                    valor = 1500.0,
                    tipo = TransactionType.ENTRADA,
                    data = "2026-08-02",
                    categoria = "Venda",
                    cloudId = "00000000-0000-4000-8000-000000000013",
                    ownerUserId = owner,
                    cropCloudId = cropId
                )
            ),
            exportedAtEpochMillis = 1_800_000_000_000
        )
    }

    private companion object {
        const val PASSWORD = "senha-forte-123"
    }
}
