package com.agrogestao.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agrogestao.pro.data.local.entities.ProducerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProducerDao {
    @Query("SELECT * FROM produtor WHERE id = 1 LIMIT 1")
    fun getProducerProfile(): Flow<ProducerEntity?>

    @Query("SELECT * FROM produtor WHERE id = 1 LIMIT 1")
    suspend fun getProducerProfileOnce(): ProducerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProducer(producer: ProducerEntity)

    @Query("UPDATE produtor SET remoteUserId = :remoteUserId WHERE id = 1")
    suspend fun updateRemoteUserId(remoteUserId: String)

    @Query(
        "UPDATE produtor SET accessToken = '', refreshToken = '', " +
            "tokenExpiresAtEpochSeconds = 0 WHERE id = 1"
    )
    suspend fun clearLegacySession()

    @Query("UPDATE produtor SET syncStatus = :status WHERE id = 1 AND updatedAtEpochMillis = :expectedUpdatedAt")
    suspend fun updateSyncStatusIfUnchanged(expectedUpdatedAt: Long, status: String): Int
}
