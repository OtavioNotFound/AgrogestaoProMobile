package com.agrogestao.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.data.local.entities.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tarefas WHERE ownerUserId = :ownerUserId AND isDeleted = 0 ORDER BY id DESC")
    fun getAllTasks(ownerUserId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tarefas WHERE ownerUserId = :ownerUserId AND syncStatus != :syncedStatus")
    suspend fun getPendingSync(ownerUserId: String, syncedStatus: String): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tarefas WHERE ownerUserId = :ownerUserId AND syncStatus != :syncedStatus")
    fun observePendingSyncCount(ownerUserId: String, syncedStatus: String): Flow<Int>

    @Query("SELECT * FROM tarefas WHERE ownerUserId = :ownerUserId")
    suspend fun getOwnedRows(ownerUserId: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tarefas SET status = :newStatus, syncStatus = :pendingStatus, updatedAtEpochMillis = :updatedAt WHERE id = :taskId")
    suspend fun updateTaskStatus(
        taskId: Long,
        newStatus: TaskStatus,
        pendingStatus: String,
        updatedAt: Long
    )

    @Query("SELECT * FROM tarefas WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Query("SELECT * FROM tarefas WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getByCloudId(cloudId: String): TaskEntity?

    @Query("UPDATE tarefas SET isDeleted = 1, syncStatus = :pendingStatus, updatedAtEpochMillis = :updatedAt WHERE id = :taskId")
    suspend fun markDeleted(taskId: Long, pendingStatus: String, updatedAt: Long)

    @Query("DELETE FROM tarefas WHERE id = :taskId")
    suspend fun hardDelete(taskId: Long)

    @Query("DELETE FROM tarefas WHERE id = :taskId AND updatedAtEpochMillis = :expectedUpdatedAt")
    suspend fun hardDeleteIfUnchanged(taskId: Long, expectedUpdatedAt: Long): Int

    @Query("UPDATE tarefas SET syncStatus = :status WHERE id = :taskId AND updatedAtEpochMillis = :expectedUpdatedAt")
    suspend fun updateSyncStatusIfUnchanged(
        taskId: Long,
        expectedUpdatedAt: Long,
        status: String
    ): Int

    @Query("UPDATE tarefas SET ownerUserId = :ownerUserId, syncStatus = :pendingStatus, updatedAtEpochMillis = :updatedAt WHERE ownerUserId = ''")
    suspend fun claimUnownedRows(ownerUserId: String, pendingStatus: String, updatedAt: Long)

    @Query("UPDATE tarefas SET ownerUserId = :newOwnerUserId, syncStatus = :pendingStatus, updatedAtEpochMillis = :updatedAt WHERE ownerUserId = :oldOwnerUserId")
    suspend fun reassignOwner(
        oldOwnerUserId: String,
        newOwnerUserId: String,
        pendingStatus: String,
        updatedAt: Long
    )
}
