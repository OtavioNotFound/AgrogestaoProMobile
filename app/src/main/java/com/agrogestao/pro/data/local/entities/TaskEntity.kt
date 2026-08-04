package com.agrogestao.pro.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.agrogestao.pro.data.remote.SupabaseConfig
import java.util.UUID

enum class TaskStatus {
    A_FAZER,
    EM_PROGRESSO,
    CONCLUIDO
}

@Entity(
    tableName = "tarefas",
    indices = [
        Index(value = ["cloudId"], unique = true),
        Index(value = ["ownerUserId", "syncStatus"]),
        Index(value = ["cropCloudId"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titulo: String,            // ex: "Adubar a roça de milho"
    val descricao: String,         // ex: "Usar 2 sacos de NPK na área norte"
    val categoria: String,         // ex: "Adubação", "Irrigação", "Colheita", "Manutenção"
    val dataLimite: String,        // ISO 8601: "2026-07-30"
    val status: TaskStatus = TaskStatus.A_FAZER,
    val syncStatus: String = SupabaseConfig.STATUS_LOCAL_OFFLINE,
    val isDeleted: Boolean = false,
    val cloudId: String = UUID.randomUUID().toString(),
    val ownerUserId: String = "",
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
    val cropCloudId: String? = null
)
