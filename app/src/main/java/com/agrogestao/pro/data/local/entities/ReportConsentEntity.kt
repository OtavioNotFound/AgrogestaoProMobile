package com.agrogestao.pro.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "report_consent")
data class ReportConsentEntity(
    @PrimaryKey val ownerUserId: String,
    val consentVersion: Int,
    val acceptedAtEpochMillis: Long,
    val isGranted: Boolean,
    val revokedAtEpochMillis: Long? = null
)
