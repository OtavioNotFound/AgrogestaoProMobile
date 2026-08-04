package com.agrogestao.pro.data.reminders

import android.Manifest
import android.app.NotificationManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.agrogestao.pro.data.local.AgroDatabase
import com.agrogestao.pro.data.local.entities.ProducerEntity
import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.data.local.entities.TaskStatus
import com.agrogestao.pro.data.remote.SupabaseConfig
import com.agrogestao.pro.domain.TaskReminderSettings
import com.agrogestao.pro.domain.todayIso
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskReminderWorkerTest {
    @Test
    fun postsOnceAndRechecksCompletionAndActiveAccount() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = AgroDatabase.getDatabase(context)
        val originalProducer = database.producerDao().getProducerProfileOnce()
        val owner = "local:task-reminder-worker-test"
        val preferences = TaskReminderPreferences(context).also { it.clearForTests() }
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancelAll()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }

        database.producerDao().insertOrUpdateProducer(
            ProducerEntity(
                id = 1,
                nomeProdutor = "Lembrete",
                email = "",
                nomePropriedade = "Sítio",
                municipioUF = "PE",
                dAPouCAF = "",
                areaTotalHectares = 2.0,
                isLoggedIn = true,
                remoteUserId = owner,
                syncStatus = SupabaseConfig.STATUS_LOCAL_OFFLINE
            )
        )
        val taskId = database.taskDao().insertTask(
            TaskEntity(
                titulo = "Irrigar hoje",
                descricao = "Talhão norte",
                categoria = "Irrigação",
                dataLimite = todayIso(),
                ownerUserId = owner
            )
        )
        val task = database.taskDao().getTaskById(taskId)!!
        val settings = TaskReminderSettings(enabled = true, daysBefore = 0, hourOfDay = 7)
        assertTrue(preferences.save(owner, settings))

        val firstResult = worker(context, owner, task, settings).doWork()
        assertTrue(firstResult is ListenableWorker.Result.Success)
        assertEquals(1, notificationManager.activeNotifications.size)
        assertTrue(
            notificationManager.activeNotifications.single().notification.extras
                .getString("android.title").orEmpty().contains("Irrigar hoje")
        )

        notificationManager.cancelAll()
        worker(context, owner, task, settings).doWork()
        assertTrue(notificationManager.activeNotifications.isEmpty())

        database.taskDao().updateTaskStatus(
            task.id,
            TaskStatus.CONCLUIDO,
            SupabaseConfig.STATUS_LOCAL_OFFLINE,
            task.updatedAtEpochMillis + 1L
        )
        val completed = database.taskDao().getTaskById(task.id)!!
        worker(context, owner, completed, settings).doWork()
        assertTrue(notificationManager.activeNotifications.isEmpty())

        database.taskDao().hardDelete(task.id)
        database.producerDao().insertOrUpdateProducer(
            originalProducer ?: database.producerDao().getProducerProfileOnce()!!.copy(
                isLoggedIn = false
            )
        )
        worker(context, owner, completed, settings).doWork()
        assertTrue(notificationManager.activeNotifications.isEmpty())
        preferences.clearForTests()
        Unit
    }

    private fun worker(
        context: android.content.Context,
        owner: String,
        task: TaskEntity,
        settings: TaskReminderSettings
    ): TaskReminderWorker = TestListenableWorkerBuilder<TaskReminderWorker>(context)
        .setInputData(
            Data.Builder()
                .putString(TaskReminderWorker.KEY_OWNER_USER_ID, owner)
                .putString(TaskReminderWorker.KEY_TASK_CLOUD_ID, task.cloudId)
                .putLong(TaskReminderWorker.KEY_TASK_UPDATED_AT, task.updatedAtEpochMillis)
                .putInt(TaskReminderWorker.KEY_DAYS_BEFORE, settings.daysBefore)
                .putInt(TaskReminderWorker.KEY_HOUR_OF_DAY, settings.hourOfDay)
                .build()
        )
        .build()
}
