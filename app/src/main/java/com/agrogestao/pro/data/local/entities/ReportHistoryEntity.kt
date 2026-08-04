package com.agrogestao.pro.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "report_history",
    indices = [Index(value = ["ownerUserId", "createdAtEpochMillis"])]
)
data class ReportHistoryEntity(
    @PrimaryKey val reportId: String,
    val ownerUserId: String,
    val fileName: String,
    val relativePath: String,
    val createdAtEpochMillis: Long,
    val generatedDate: String,
    val fromDate: String,
    val toDate: String,
    val income: Double,
    val expenses: Double,
    val balance: Double,
    val isComplete: Boolean,
    val missingItems: String,
    val sha256: String,
    val fileSizeBytes: Long,
    val reportFormatVersion: Int = 1,
    val consentVersion: Int = 0,
    val consentAcceptedAtEpochMillis: Long = 0L
)
