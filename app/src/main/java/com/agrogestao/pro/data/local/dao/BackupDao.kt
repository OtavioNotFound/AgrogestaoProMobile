package com.agrogestao.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.ProducerEntity
import com.agrogestao.pro.data.local.entities.TaskEntity

@Dao
abstract class BackupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertProducer(producer: ProducerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertCrops(crops: List<CropEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertTasks(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertTransactions(transactions: List<FinancialEntity>)

    @Transaction
    open suspend fun mergeBackup(
        producer: ProducerEntity,
        crops: List<CropEntity>,
        tasks: List<TaskEntity>,
        transactions: List<FinancialEntity>
    ) {
        insertProducer(producer)
        if (crops.isNotEmpty()) insertCrops(crops)
        if (tasks.isNotEmpty()) insertTasks(tasks)
        if (transactions.isNotEmpty()) insertTransactions(transactions)
    }
}
