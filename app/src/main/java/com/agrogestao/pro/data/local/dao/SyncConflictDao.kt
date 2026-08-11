package com.agrogestao.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.agrogestao.pro.data.local.entities.SyncConflictEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncConflictDao {
    @Query("SELECT * FROM sync_conflicts WHERE ownerUserId = :owner ORDER BY detectedAtEpochMillis DESC LIMIT 100")
    fun observe(owner: String): Flow<List<SyncConflictEntity>>

    @Insert
    suspend fun insert(conflict: SyncConflictEntity)

    @Query("DELETE FROM sync_conflicts WHERE ownerUserId = :owner")
    suspend fun clear(owner: String)
}
