package com.agrogestao.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import androidx.room.Update
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.TaskEntity

data class DailyActivityLocalIds(
    val taskId: Long,
    val transactionId: Long?
)

/** Compound local write used by the fast daily-update flow. */
@Dao
abstract class DailyActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertTransaction(transaction: FinancialEntity): Long

    @Update
    protected abstract suspend fun updateCrop(crop: CropEntity)

    @Transaction
    open suspend fun record(
        task: TaskEntity,
        transaction: FinancialEntity?,
        updatedCrop: CropEntity?
    ): DailyActivityLocalIds {
        val taskId = insertTask(task)
        val transactionId = transaction?.let { insertTransaction(it) }
        updatedCrop?.let { updateCrop(it) }
        return DailyActivityLocalIds(taskId, transactionId)
    }
}
