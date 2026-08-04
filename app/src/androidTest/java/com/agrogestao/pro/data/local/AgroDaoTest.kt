package com.agrogestao.pro.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.data.local.entities.ReportHistoryEntity
import com.agrogestao.pro.data.local.entities.ReportConsentEntity
import com.agrogestao.pro.data.remote.SupabaseConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AgroDaoTest {
    private val ownerUserId = "00000000-0000-4000-8000-000000000001"
    private lateinit var database: AgroDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AgroDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun deletedCropIsHiddenButRemainsPendingForCloudSync() = runBlocking {
        val dao = database.cropDao()
        val id = dao.insertCrop(
            CropEntity(
                nomeCultura = "Milho",
                areaHectares = 2.5,
                dataInicio = "2026-07-30",
                previsaoColheita = "2026-10-30",
                progressoPercentual = 0,
                statusManejo = "Preparo do solo",
                syncStatus = SupabaseConfig.STATUS_SYNCED_CLOUD,
                ownerUserId = ownerUserId
            )
        )

        dao.markDeleted(id, SupabaseConfig.STATUS_LOCAL_OFFLINE, updatedAt = 123L)

        assertEquals(emptyList<CropEntity>(), dao.getAllCrops(ownerUserId).first())
        val pending = dao.getPendingSync(ownerUserId, SupabaseConfig.STATUS_SYNCED_CLOUD)
        assertEquals(1, pending.size)
        assertEquals(true, pending.single().isDeleted)
        assertEquals(123L, pending.single().updatedAtEpochMillis)
    }

    @Test
    fun taskAssociationAndLocalUserIsolationArePreserved() = runBlocking {
        val cropId = "00000000-0000-4000-8000-000000000010"
        val otherOwner = "00000000-0000-4000-8000-000000000002"
        database.taskDao().insertTask(
            TaskEntity(
                titulo = "Adubar milho",
                descricao = "Aplicar NPK",
                categoria = "Adubação",
                dataLimite = "2026-08-10",
                ownerUserId = ownerUserId,
                cropCloudId = cropId
            )
        )
        database.taskDao().insertTask(
            TaskEntity(
                titulo = "Tarefa de outro produtor",
                descricao = "Não deve aparecer",
                categoria = "Manejo",
                dataLimite = "2026-08-11",
                ownerUserId = otherOwner
            )
        )

        val visibleTasks = database.taskDao().getAllTasks(ownerUserId).first()
        assertEquals(1, visibleTasks.size)
        assertEquals("Adubar milho", visibleTasks.single().titulo)
        assertEquals(cropId, visibleTasks.single().cropCloudId)
    }

    @Test
    fun reportHistoryIsIsolatedByOwnerAndCanOnlyDeleteOwnedRows() = runBlocking {
        val dao = database.reportHistoryDao()
        val otherOwner = "00000000-0000-4000-8000-000000000002"
        dao.insert(reportHistory("report-a", ownerUserId, 200L))
        dao.insert(reportHistory("report-b", otherOwner, 300L))
        dao.insert(reportHistory("report-c", ownerUserId, 100L))

        assertEquals(
            listOf("report-a", "report-c"),
            dao.observeForOwner(ownerUserId).first().map(ReportHistoryEntity::reportId)
        )
        assertEquals(0, dao.deleteOwned("report-b", ownerUserId))
        assertEquals(1, dao.deleteOwned("report-a", ownerUserId))
        assertEquals(
            listOf("report-c"),
            dao.observeForOwner(ownerUserId).first().map(ReportHistoryEntity::reportId)
        )
    }

    @Test
    fun reportConsentIsAccountScopedAndRevocationKeepsItsAuditTimestamp() = runBlocking {
        val dao = database.reportConsentDao()
        val otherOwner = "00000000-0000-4000-8000-000000000002"
        dao.upsert(
            ReportConsentEntity(
                ownerUserId = ownerUserId,
                consentVersion = 1,
                acceptedAtEpochMillis = 1_000L,
                isGranted = true
            )
        )
        dao.upsert(
            ReportConsentEntity(
                ownerUserId = otherOwner,
                consentVersion = 1,
                acceptedAtEpochMillis = 2_000L,
                isGranted = true
            )
        )

        assertEquals(1_000L, dao.observeForOwner(ownerUserId).first()!!.acceptedAtEpochMillis)
        dao.upsert(
            dao.getForOwner(ownerUserId)!!.copy(
                isGranted = false,
                revokedAtEpochMillis = 3_000L
            )
        )

        val revoked = dao.getForOwner(ownerUserId)!!
        assertEquals(false, revoked.isGranted)
        assertEquals(3_000L, revoked.revokedAtEpochMillis)
        assertEquals(true, dao.getForOwner(otherOwner)!!.isGranted)
    }

    private fun reportHistory(id: String, owner: String, timestamp: Long) =
        ReportHistoryEntity(
            reportId = id,
            ownerUserId = owner,
            fileName = "$id.pdf",
            relativePath = "owner/$id.pdf",
            createdAtEpochMillis = timestamp,
            generatedDate = "2026-08-02",
            fromDate = "2026-01-01",
            toDate = "2026-07-31",
            income = 100.0,
            expenses = 20.0,
            balance = 80.0,
            isComplete = true,
            missingItems = "",
            sha256 = "abc",
            fileSizeBytes = 100L
        )
}
