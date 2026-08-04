package com.agrogestao.pro.data.security

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agrogestao.pro.data.local.AgroDatabase
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.ProducerEntity
import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.data.local.entities.TaskStatus
import com.agrogestao.pro.data.local.entities.TransactionType
import com.agrogestao.pro.data.repository.AgroRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureSessionStoreTest {
    private lateinit var context: Context
    private lateinit var store: SecureSessionStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = SecureSessionStore(context)
        store.clear()
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun sessionIsEncryptedAtRestAndCanBeCleared() {
        val session = SecureSession(
            userId = "00000000-0000-4000-8000-000000000001",
            accessToken = "access-token-muito-secreto",
            refreshToken = "refresh-token-muito-secreto",
            expiresAtEpochSeconds = 1_800_000_000
        )

        assertTrue(store.save(session))
        assertEquals(session, store.read())

        val raw = context.getSharedPreferences(
            SecureSessionStore.PREFERENCES_NAME,
            Context.MODE_PRIVATE
        ).all.values.joinToString()
        assertFalse(raw.contains(session.accessToken))
        assertFalse(raw.contains(session.refreshToken))

        store.clear()
        assertNull(store.read())
    }

    @Test
    fun legacyRoomTokensAreMigratedAndCleared() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AgroDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val producerDao = database.producerDao()
            producerDao.insertOrUpdateProducer(
                ProducerEntity(
                    nomeProdutor = "Maria",
                    email = "maria@example.com",
                    nomePropriedade = "Sítio Verde",
                    municipioUF = "Juazeiro - BA",
                    dAPouCAF = "CAF-123",
                    areaTotalHectares = 8.0,
                    isLoggedIn = true,
                    remoteUserId = "00000000-0000-4000-8000-000000000001",
                    accessToken = "access-legado",
                    refreshToken = "refresh-legado",
                    tokenExpiresAtEpochSeconds = 1_800_000_000
                )
            )
            val repository = AgroRepository(
                backupDao = database.backupDao(),
                cropDao = database.cropDao(),
                taskDao = database.taskDao(),
                financialDao = database.financialDao(),
                producerDao = producerDao,
                reportHistoryDao = database.reportHistoryDao(),
                reportConsentDao = database.reportConsentDao(),
                secureSessionStore = store
            )

            repository.prepareLocalSession()

            assertEquals("access-legado", store.read()?.accessToken)
            assertEquals("refresh-legado", store.read()?.refreshToken)
            val migratedProducer = producerDao.getProducerProfileOnce()
            assertEquals("", migratedProducer?.accessToken)
            assertEquals("", migratedProducer?.refreshToken)
            assertEquals(0L, migratedProducer?.tokenExpiresAtEpochSeconds)
            assertEquals(true, migratedProducer?.isLoggedIn)
        } finally {
            database.close()
        }
    }

    @Test
    fun offlineProfileSurvivesRestartAndOwnsNewData() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AgroDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = AgroRepository(
                backupDao = database.backupDao(),
                cropDao = database.cropDao(),
                taskDao = database.taskDao(),
                financialDao = database.financialDao(),
                producerDao = database.producerDao(),
                reportHistoryDao = database.reportHistoryDao(),
                reportConsentDao = database.reportConsentDao(),
                secureSessionStore = store
            )

            assertTrue(
                repository.startOfflineProfile(
                    nome = "Produtor teste",
                    propriedade = "Sítio local",
                    municipio = "Petrolina - PE",
                    area = 5.0
                ).isSuccess
            )
            val originalOwner = database.producerDao()
                .getProducerProfileOnce()
                ?.remoteUserId
                .orEmpty()
            assertTrue(originalOwner.startsWith("local:"))
            assertNull(store.read())

            repository.insertCrop(
                CropEntity(
                    nomeCultura = "Milho",
                    areaHectares = 2.5,
                    dataInicio = "2026-08-02",
                    previsaoColheita = "2026-12-10",
                    progressoPercentual = 10,
                    statusManejo = "Plantio concluído"
                )
            )
            repository.prepareLocalSession()

            val restoredProducer = database.producerDao().getProducerProfileOnce()
            assertEquals(true, restoredProducer?.isLoggedIn)
            assertEquals(originalOwner, restoredProducer?.remoteUserId)
            val crops = database.cropDao().getAllCrops(originalOwner).first()
            assertEquals(1, crops.size)
            assertEquals(originalOwner, crops.single().ownerUserId)
        } finally {
            database.close()
        }
    }

    @Test
    fun editingRecordsPreservesCloudIdentityOwnerAndAssociation() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AgroDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = AgroRepository(
                backupDao = database.backupDao(),
                cropDao = database.cropDao(),
                taskDao = database.taskDao(),
                financialDao = database.financialDao(),
                producerDao = database.producerDao(),
                reportHistoryDao = database.reportHistoryDao(),
                reportConsentDao = database.reportConsentDao(),
                secureSessionStore = store
            )
            assertTrue(
                repository.startOfflineProfile(
                    nome = "Produtor editor",
                    propriedade = "Sítio edição",
                    municipio = "Petrolina - PE",
                    area = 12.0
                ).isSuccess
            )
            val owner = database.producerDao().getProducerProfileOnce()?.remoteUserId.orEmpty()

            repository.insertCrop(
                CropEntity(
                    nomeCultura = "Milho",
                    areaHectares = 2.0,
                    dataInicio = "2026-08-02",
                    previsaoColheita = "2026-12-02",
                    progressoPercentual = 10,
                    statusManejo = "Plantio"
                )
            )
            val originalCrop = database.cropDao().getAllCrops(owner).first().single()
            repository.insertTask(
                TaskEntity(
                    titulo = "Irrigar",
                    descricao = "Setor norte",
                    categoria = "Irrigação",
                    dataLimite = "2026-08-05",
                    cropCloudId = originalCrop.cloudId
                )
            )
            repository.insertTransaction(
                FinancialEntity(
                    descricao = "Sementes",
                    valor = 100.0,
                    tipo = TransactionType.SAIDA,
                    data = "2026-08-02",
                    categoria = "Insumos",
                    cropCloudId = originalCrop.cloudId
                )
            )
            val originalTask = database.taskDao().getAllTasks(owner).first().single()
            val originalTransaction = database.financialDao()
                .getAllTransactions(owner)
                .first()
                .single()

            repository.updateCrop(
                originalCrop.copy(
                    nomeCultura = "Milho verde",
                    areaHectares = 3.5,
                    progressoPercentual = 80,
                    statusManejo = "Colheita próxima"
                )
            )
            repository.updateTask(
                originalTask.copy(
                    titulo = "Irrigar setor sul",
                    descricao = "Durante 30 minutos",
                    categoria = "Manejo hídrico",
                    dataLimite = "2026-08-06",
                    status = TaskStatus.EM_PROGRESSO
                )
            )
            repository.updateTransaction(
                originalTransaction.copy(
                    descricao = "Venda antecipada",
                    valor = 350.0,
                    tipo = TransactionType.ENTRADA,
                    data = "2026-08-07",
                    categoria = "Venda"
                )
            )

            val editedCrop = database.cropDao().getAllCrops(owner).first().single()
            val editedTask = database.taskDao().getAllTasks(owner).first().single()
            val editedTransaction = database.financialDao()
                .getAllTransactions(owner)
                .first()
                .single()
            assertEquals(originalCrop.cloudId, editedCrop.cloudId)
            assertEquals(owner, editedCrop.ownerUserId)
            assertEquals(80, editedCrop.progressoPercentual)
            assertTrue(editedCrop.updatedAtEpochMillis > originalCrop.updatedAtEpochMillis)
            assertEquals(originalTask.cloudId, editedTask.cloudId)
            assertEquals(owner, editedTask.ownerUserId)
            assertEquals(originalCrop.cloudId, editedTask.cropCloudId)
            assertEquals(TaskStatus.EM_PROGRESSO, editedTask.status)
            assertEquals(originalTransaction.cloudId, editedTransaction.cloudId)
            assertEquals(owner, editedTransaction.ownerUserId)
            assertEquals(originalCrop.cloudId, editedTransaction.cropCloudId)
            assertEquals(TransactionType.ENTRADA, editedTransaction.tipo)
        } finally {
            database.close()
        }
    }
}
