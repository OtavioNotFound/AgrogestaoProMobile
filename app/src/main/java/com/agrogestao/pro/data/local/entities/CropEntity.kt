package com.agrogestao.pro.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.agrogestao.pro.data.remote.SupabaseConfig
import java.util.UUID

@Entity(
    tableName = "safras",
    indices = [
        Index(value = ["cloudId"], unique = true),
        Index(value = ["ownerUserId", "syncStatus"])
    ]
)
data class CropEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nomeCultura: String,      // ex: "Milho Irrigado", "Feijão Caupi", "Mandioca"
    val areaHectares: Double,      // ex: 5.5
    val dataInicio: String,        // ISO 8601: "2026-03-15"
    val previsaoColheita: String,  // ISO 8601: "2026-08-20"
    val progressoPercentual: Int,  // 0 a 100
    val statusManejo: String,      // ex: "Irrigação ativa", "Aguardando adubação"
    val syncStatus: String = SupabaseConfig.STATUS_LOCAL_OFFLINE,
    val isDeleted: Boolean = false,
    val cloudId: String = UUID.randomUUID().toString(),
    val ownerUserId: String = "",
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
)
