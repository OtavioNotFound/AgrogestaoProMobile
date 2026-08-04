package com.agrogestao.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agrogestao.pro.data.local.entities.ReportHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportHistoryDao {
    @Query(
        "SELECT * FROM report_history WHERE ownerUserId = :ownerUserId " +
            "ORDER BY createdAtEpochMillis DESC"
    )
    fun observeForOwner(ownerUserId: String): Flow<List<ReportHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(report: ReportHistoryEntity)

    @Query(
        "DELETE FROM report_history WHERE reportId = :reportId AND ownerUserId = :ownerUserId"
    )
    suspend fun deleteOwned(reportId: String, ownerUserId: String): Int

    @Query(
        "UPDATE report_history SET ownerUserId = :newOwnerUserId " +
            "WHERE ownerUserId = :oldOwnerUserId"
    )
    suspend fun reassignOwner(oldOwnerUserId: String, newOwnerUserId: String): Int
}
