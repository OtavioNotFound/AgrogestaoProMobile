package com.agrogestao.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.agrogestao.pro.data.local.entities.CropEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CropDao {
    @Query("SELECT * FROM safras WHERE ownerUserId = :ownerUserId AND isDeleted = 0 ORDER BY id DESC")
    fun getAllCrops(ownerUserId: String): Flow<List<CropEntity>>

    @Query("SELECT * FROM safras WHERE ownerUserId = :ownerUserId AND syncStatus != :syncedStatus")
    suspend fun getPendingSync(ownerUserId: String, syncedStatus: String): List<CropEntity>

    @Query("SELECT COUNT(*) FROM safras WHERE ownerUserId = :ownerUserId AND syncStatus != :syncedStatus")
    fun observePendingSyncCount(ownerUserId: String, syncedStatus: String): Flow<Int>

    @Query("SELECT * FROM safras WHERE ownerUserId = :ownerUserId")
    suspend fun getOwnedRows(ownerUserId: String): List<CropEntity>

    @Query("SELECT * FROM safras WHERE id = :cropId LIMIT 1")
    suspend fun getById(cropId: Long): CropEntity?

    @Query("SELECT * FROM safras WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getByCloudId(cloudId: String): CropEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrop(crop: CropEntity): Long

    @Update
    suspend fun updateCrop(crop: CropEntity)

    @Query("UPDATE safras SET isDeleted = 1, syncStatus = :pendingStatus, updatedAtEpochMillis = :updatedAt WHERE id = :cropId")
    suspend fun markDeleted(cropId: Long, pendingStatus: String, updatedAt: Long)

    @Query("DELETE FROM safras WHERE id = :cropId")
    suspend fun hardDelete(cropId: Long)

    @Query("DELETE FROM safras WHERE id = :cropId AND updatedAtEpochMillis = :expectedUpdatedAt")
    suspend fun hardDeleteIfUnchanged(cropId: Long, expectedUpdatedAt: Long): Int

    @Query("UPDATE safras SET syncStatus = :status WHERE id = :cropId AND updatedAtEpochMillis = :expectedUpdatedAt")
    suspend fun updateSyncStatusIfUnchanged(
        cropId: Long,
        expectedUpdatedAt: Long,
        status: String
    ): Int

    @Query("UPDATE safras SET ownerUserId = :ownerUserId, syncStatus = :pendingStatus, updatedAtEpochMillis = :updatedAt WHERE ownerUserId = ''")
    suspend fun claimUnownedRows(ownerUserId: String, pendingStatus: String, updatedAt: Long)

    @Query("UPDATE safras SET ownerUserId = :newOwnerUserId, syncStatus = :pendingStatus, updatedAtEpochMillis = :updatedAt WHERE ownerUserId = :oldOwnerUserId")
    suspend fun reassignOwner(
        oldOwnerUserId: String,
        newOwnerUserId: String,
        pendingStatus: String,
        updatedAt: Long
    )
}
