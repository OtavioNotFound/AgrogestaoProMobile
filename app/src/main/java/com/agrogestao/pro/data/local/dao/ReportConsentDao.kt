package com.agrogestao.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agrogestao.pro.data.local.entities.ReportConsentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportConsentDao {
    @Query("SELECT * FROM report_consent WHERE ownerUserId = :ownerUserId LIMIT 1")
    fun observeForOwner(ownerUserId: String): Flow<ReportConsentEntity?>

    @Query("SELECT * FROM report_consent WHERE ownerUserId = :ownerUserId LIMIT 1")
    suspend fun getForOwner(ownerUserId: String): ReportConsentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(consent: ReportConsentEntity)

    @Query(
        "UPDATE report_consent SET ownerUserId = :newOwnerUserId " +
            "WHERE ownerUserId = :oldOwnerUserId"
    )
    suspend fun reassignOwner(oldOwnerUserId: String, newOwnerUserId: String): Int
}
