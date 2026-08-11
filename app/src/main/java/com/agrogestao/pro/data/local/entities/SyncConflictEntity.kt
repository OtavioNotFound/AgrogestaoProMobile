package com.agrogestao.pro.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_conflicts",
    indices = [Index(value = ["ownerUserId", "detectedAtEpochMillis"])]
)
data class SyncConflictEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: String,
    val entityType: String,
    val entityCloudId: String,
    val localTimestamp: Long,
    val remoteTimestamp: Long,
    val resolution: String,
    val detectedAtEpochMillis: Long = System.currentTimeMillis()
)
