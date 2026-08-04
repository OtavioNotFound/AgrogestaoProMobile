package com.agrogestao.pro.data.reminders

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.agrogestao.pro.MainActivity
import com.agrogestao.pro.R
import com.agrogestao.pro.data.local.AgroDatabase
import com.agrogestao.pro.data.local.entities.TaskStatus
import com.agrogestao.pro.domain.TaskReminderSettings
import com.agrogestao.pro.domain.formatDateForDisplay
import com.agrogestao.pro.domain.taskReminderDeliverySignature
import com.agrogestao.pro.domain.todayIso

class TaskReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ownerUserId = inputData.getString(KEY_OWNER_USER_ID).orEmpty()
        val taskCloudId = inputData.getString(KEY_TASK_CLOUD_ID).orEmpty()
        val expectedUpdatedAt = inputData.getLong(KEY_TASK_UPDATED_AT, -1L)
        val requestedSettings = TaskReminderSettings(
            enabled = true,
            daysBefore = inputData.getInt(KEY_DAYS_BEFORE, -1),
            hourOfDay = inputData.getInt(KEY_HOUR_OF_DAY, -1)
        )
        if (
            ownerUserId.isBlank() || taskCloudId.isBlank() || expectedUpdatedAt <= 0L ||
            !requestedSettings.isValid
        ) return Result.success()

        val database = AgroDatabase.getDatabase(applicationContext)
        val preferences = TaskReminderPreferences(applicationContext)
        val producer = database.producerDao().getProducerProfileOnce()
        if (
            producer?.isLoggedIn != true ||
            producer.remoteUserId != ownerUserId
        ) return Result.success()

        val settings = preferences.read(ownerUserId)
        if (!settings.enabled || settings != requestedSettings) return Result.success()
        val task = database.taskDao().getByCloudId(taskCloudId)
            ?.takeIf {
                it.ownerUserId == ownerUserId &&
                    it.updatedAtEpochMillis == expectedUpdatedAt &&
                    !it.isDeleted &&
                    it.status != TaskStatus.CONCLUIDO
            }
            ?: return Result.success()

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val signature = taskReminderDeliverySignature(task, settings)
        if (!preferences.markDeliveredIfNew(
                ownerUserId,
                task.cloudId,
                signature
            )
        ) return Result.success()

        showNotification(task.cloudId, task.titulo, task.dataLimite, task.descricao)
        return Result.success()
    }

    @Suppress("DEPRECATION")
    private fun showNotification(
        taskCloudId: String,
        title: String,
        dueDate: String,
        description: String
    ) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Lembretes de tarefas",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    this.description =
                        "Avisos locais de tarefas agrícolas próximas ou atrasadas"
                }
            )
        }
        val notificationTitle = when {
            dueDate < todayIso() -> "Tarefa atrasada: $title"
            dueDate == todayIso() -> "Tarefa para hoje: $title"
            else -> "Lembrete de tarefa: $title"
        }
        val message = buildString {
            append("Prazo: ${formatDateForDisplay(dueDate)}")
            description.trim().takeIf(String::isNotBlank)?.let { append(" • $it") }
        }
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_TASKS, true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId(taskCloudId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(applicationContext, CHANNEL_ID)
        } else {
            Notification.Builder(applicationContext)
        }
        manager.notify(
            notificationId(taskCloudId),
            builder
                .setSmallIcon(R.drawable.ic_launcher_legacy)
                .setColor(ContextCompat.getColor(applicationContext, R.color.launcher_background))
                .setContentTitle(notificationTitle)
                .setContentText(message)
                .setStyle(Notification.BigTextStyle().bigText(message))
                .setCategory(Notification.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
        )
    }

    private fun notificationId(taskCloudId: String): Int =
        taskCloudId.hashCode() and Int.MAX_VALUE

    companion object {
        const val KEY_OWNER_USER_ID = "owner_user_id"
        const val KEY_TASK_CLOUD_ID = "task_cloud_id"
        const val KEY_TASK_UPDATED_AT = "task_updated_at"
        const val KEY_DAYS_BEFORE = "days_before"
        const val KEY_HOUR_OF_DAY = "hour_of_day"
        const val CHANNEL_ID = "task_reminders_v1"
    }
}
