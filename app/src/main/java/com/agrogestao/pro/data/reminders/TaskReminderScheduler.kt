package com.agrogestao.pro.data.reminders

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.domain.TaskReminderPlan
import com.agrogestao.pro.domain.TaskReminderSettings
import com.agrogestao.pro.domain.buildTaskReminderPlans
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class TaskReminderScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun replaceSchedules(
        ownerUserId: String,
        tasks: List<TaskEntity>,
        settings: TaskReminderSettings,
        nowEpochMillis: Long = System.currentTimeMillis()
    ) {
        workManager.cancelAllWorkByTag(TAG_ALL_TASK_REMINDERS)
        if (ownerUserId.isBlank()) return
        buildTaskReminderPlans(tasks, settings, nowEpochMillis).forEach { plan ->
            enqueue(ownerUserId, plan, settings, nowEpochMillis)
        }
    }

    fun cancelAll() {
        workManager.cancelAllWorkByTag(TAG_ALL_TASK_REMINDERS)
    }

    private fun enqueue(
        ownerUserId: String,
        plan: TaskReminderPlan,
        settings: TaskReminderSettings,
        nowEpochMillis: Long
    ) {
        val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(
                (plan.triggerAtEpochMillis - nowEpochMillis).coerceAtLeast(0L),
                TimeUnit.MILLISECONDS
            )
            .setInputData(
                Data.Builder()
                    .putString(TaskReminderWorker.KEY_OWNER_USER_ID, ownerUserId)
                    .putString(TaskReminderWorker.KEY_TASK_CLOUD_ID, plan.taskCloudId)
                    .putLong(
                        TaskReminderWorker.KEY_TASK_UPDATED_AT,
                        plan.taskUpdatedAtEpochMillis
                    )
                    .putInt(TaskReminderWorker.KEY_DAYS_BEFORE, settings.daysBefore)
                    .putInt(TaskReminderWorker.KEY_HOUR_OF_DAY, settings.hourOfDay)
                    .build()
            )
            .addTag(TAG_ALL_TASK_REMINDERS)
            .addTag("$TAG_OWNER_PREFIX${digest(ownerUserId)}")
            .build()
        workManager.enqueueUniqueWork(
            "task-reminder-${digest("$ownerUserId:${plan.taskCloudId}")}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .take(12)
        .joinToString("") { byte -> "%02x".format(byte) }

    companion object {
        const val TAG_ALL_TASK_REMINDERS = "agrogestao-task-reminders"
        private const val TAG_OWNER_PREFIX = "agrogestao-task-owner-"
    }
}
