package com.agrogestao.pro.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.agrogestao.pro.data.remote.SupabaseConfig
import java.util.UUID

enum class TransactionType {
    ENTRADA, // Venda de colheita / receita
    SAIDA    // Compra de insumos / combustível / gasto
}

@Entity(
    tableName = "financeiro",
    indices = [
        Index(value = ["cloudId"], unique = true),
        Index(value = ["ownerUserId", "syncStatus"]),
        Index(value = ["cropCloudId"])
    ]
)
data class FinancialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val descricao: String,    // ex: "Venda de 20 sacos de milho", "Compra de Sementes"
    val valor: Double,        // ex: 1500.00
    val tipo: TransactionType,
    val data: String,         // ISO 8601: "2026-07-22"
    val categoria: String,    // ex: "Venda", "Adubo", "Combustível", "Ferramentas"
    val syncStatus: String = SupabaseConfig.STATUS_LOCAL_OFFLINE,
    val isDeleted: Boolean = false,
    val cloudId: String = UUID.randomUUID().toString(),
    val ownerUserId: String = "",
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
    val cropCloudId: String? = null
)
