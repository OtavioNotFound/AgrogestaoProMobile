package com.agrogestao.pro.data.reminders

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.data.local.entities.TaskStatus
import com.agrogestao.pro.domain.TaskReminderSettings
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskReminderSchedulerTest {
    @Test
    fun replacesPersistedWorkWhenTaskIsCompletedOrRemindersArePaused() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val workManager = WorkManager.getInstance(context)
        val scheduler = TaskReminderScheduler(context)
        workManager.cancelAllWorkByTag(TaskReminderScheduler.TAG_ALL_TASK_REMINDERS)
            .result.get(5, TimeUnit.SECONDS)
        val task = TaskEntity(
            titulo = "Adubar",
            descricao = "Talhão norte",
            categoria = "Adubação",
            dataLimite = "2099-12-31",
            cloudId = "00000000-0000-4000-8000-000000000099",
            ownerUserId = "owner-scheduler",
            updatedAtEpochMillis = 1234L
        )
        val enabled = TaskReminderSettings(enabled = true, daysBefore = 1, hourOfDay = 7)

        scheduler.replaceSchedules("owner-scheduler", listOf(task), enabled, 1_000L)
        val scheduled = awaitActiveWork(workManager)
        assertEquals(1, scheduled.size)
        assertTrue(scheduled.single().tags.contains(TaskReminderWorker::class.java.name))

        scheduler.replaceSchedules(
            "owner-scheduler",
            listOf(task.copy(status = TaskStatus.CONCLUIDO)),
            enabled,
            1_000L
        )
        withTimeout(5_000) {
            while (activeWork(workManager).isNotEmpty()) delay(100)
        }
        assertTrue(activeWork(workManager).isEmpty())

        scheduler.replaceSchedules(
            "owner-scheduler",
            listOf(task),
            enabled.copy(enabled = false),
            1_000L
        )
        assertTrue(activeWork(workManager).isEmpty())
    }

    private suspend fun awaitActiveWork(workManager: WorkManager): List<WorkInfo> =
        withTimeout(5_000) {
            var result = activeWork(workManager)
            while (result.isEmpty()) {
                delay(100)
                result = activeWork(workManager)
            }
            result
        }

    private fun activeWork(workManager: WorkManager): List<WorkInfo> =
        workManager.getWorkInfosByTag(TaskReminderScheduler.TAG_ALL_TASK_REMINDERS)
            .get(5, TimeUnit.SECONDS)
            .filter { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
}
