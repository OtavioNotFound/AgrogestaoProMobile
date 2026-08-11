package com.agrogestao.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.agrogestao.pro.data.local.entities.FinancialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialDao {
    @Query("SELECT * FROM financeiro WHERE ownerUserId = :ownerUserId AND isDeleted = 0 ORDER BY id DESC")
    fun getAllTransactions(ownerUserId: String): Flow<List<FinancialEntity>>

    @Query("SELECT * FROM financeiro WHERE ownerUserId = :ownerUserId AND syncStatus != :syncedStatus")
    suspend fun getPendingSync(ownerUserId: String, syncedStatus: String): List<FinancialEntity>

    @Query("SELECT COUNT(*) FROM financeiro WHERE ownerUserId = :ownerUserId AND syncStatus != :syncedStatus")
    fun observePendingSyncCount(ownerUserId: String, syncedStatus: String): Flow<Int>

    @Query("SELECT * FROM financeiro WHERE ownerUserId = :ownerUserId")
    suspend fun getOwnedRows(ownerUserId: String): List<FinancialEntity>

    @Query("SELECT * FROM financeiro WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FinancialEntity?

    @Query("SELECT * FROM financeiro WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getByCloudId(cloudId: String): FinancialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FinancialEntity): Long

    @Update
    suspend fun updateTransaction(transaction: FinancialEntity)

    @Query("UPDATE financeiro SET isDeleted = 1, syncStatus = :pendingStatus, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun markDeleted(id: Long, pendingStatus: String, updatedAt: Long)

    @Query("DELETE FROM financeiro WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("DELETE FROM financeiro WHERE id = :id AND updatedAtEpochMillis = :expectedUpdatedAt")
    suspend fun hardDeleteIfUnchanged(id: Long, expectedUpdatedAt: Long): Int

    @Query("UPDATE financeiro SET syncStatus = :status WHERE id = :id AND updatedAtEpochMillis = :expectedUpdatedAt")
    suspend fun updateSyncStatusIfUnchanged(
        id: Long,
        expectedUpdatedAt: Long,
        status: String
    ): Int

    @Query("UPDATE financeiro SET ownerUserId = :ownerUserId, syncStatus = :pendingStatus, updatedAtEpochMillis = :updatedAt WHERE ownerUserId = ''")
    suspend fun claimUnownedRows(ownerUserId: String, pendingStatus: String, updatedAt: Long)

    @Query("UPDATE financeiro SET ownerUserId = :newOwnerUserId, syncStatus = :pendingStatus, updatedAtEpochMillis = :updatedAt WHERE ownerUserId = :oldOwnerUserId")
    suspend fun reassignOwner(
        oldOwnerUserId: String,
        newOwnerUserId: String,
        pendingStatus: String,
        updatedAt: Long
    )
}
