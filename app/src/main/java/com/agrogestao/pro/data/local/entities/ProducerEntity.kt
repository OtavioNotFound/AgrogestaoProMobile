package com.agrogestao.pro.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.agrogestao.pro.data.remote.SupabaseConfig

@Entity(tableName = "produtor")
data class ProducerEntity(
    @PrimaryKey val id: Int = 1,
    val nomeProdutor: String,
    val email: String,
    val nomePropriedade: String,
    val municipioUF: String,
    val dAPouCAF: String,
    val areaTotalHectares: Double,
    val isLoggedIn: Boolean = false,
    val remoteUserId: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val tokenExpiresAtEpochSeconds: Long = 0,
    val syncStatus: String = SupabaseConfig.STATUS_LOCAL_OFFLINE,
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
)
