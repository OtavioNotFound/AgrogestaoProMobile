package com.agrogestao.pro.domain

import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.data.local.entities.TaskStatus
import java.util.Calendar
import java.util.TimeZone

data class TaskReminderSettings(
    val enabled: Boolean = false,
    val daysBefore: Int = 1,
    val hourOfDay: Int = 7
) {
    val isValid: Boolean
        get() = daysBefore in 0..7 && hourOfDay in 0..23
}

data class TaskReminderPlan(
    val taskCloudId: String,
    val dueDate: String,
    val taskUpdatedAtEpochMillis: Long,
    val triggerAtEpochMillis: Long
)

fun taskReminderPlannedEpochMillis(
    dueDate: String,
    settings: TaskReminderSettings,
    timeZone: TimeZone = TimeZone.getDefault()
): Long? {
    if (!settings.isValid) return null
    val (year, zeroBasedMonth, dayOfMonth) = isoDateParts(dueDate) ?: return null
    return Calendar.getInstance(timeZone).apply {
        clear()
        set(year, zeroBasedMonth, dayOfMonth, settings.hourOfDay, 0, 0)
        add(Calendar.DAY_OF_MONTH, -settings.daysBefore)
    }.timeInMillis
}

fun buildTaskReminderPlans(
    tasks: List<TaskEntity>,
    settings: TaskReminderSettings,
    nowEpochMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault()
): List<TaskReminderPlan> {
    if (!settings.enabled || !settings.isValid) return emptyList()
    return tasks.asSequence()
        .filter { !it.isDeleted && it.status != TaskStatus.CONCLUIDO }
        .filter { it.cloudId.isNotBlank() }
        .mapNotNull { task ->
            val planned = taskReminderPlannedEpochMillis(
                dueDate = task.dataLimite,
                settings = settings,
                timeZone = timeZone
            ) ?: return@mapNotNull null
            TaskReminderPlan(
                taskCloudId = task.cloudId,
                dueDate = task.dataLimite,
                taskUpdatedAtEpochMillis = task.updatedAtEpochMillis,
                triggerAtEpochMillis = maxOf(planned, nowEpochMillis + MIN_REMINDER_DELAY_MILLIS)
            )
        }
        .sortedBy(TaskReminderPlan::triggerAtEpochMillis)
        .toList()
}

fun taskReminderDeliverySignature(
    task: TaskEntity,
    settings: TaskReminderSettings
): String = listOf(
    task.dataLimite,
    settings.daysBefore,
    settings.hourOfDay
).joinToString(":")

fun taskReminderScheduleLabel(settings: TaskReminderSettings): String {
    if (!settings.enabled) return "Lembretes pausados"
    val anticipation = when (settings.daysBefore) {
        0 -> "no dia do prazo"
        1 -> "1 dia antes"
        else -> "${settings.daysBefore} dias antes"
    }
    return "$anticipation, às ${settings.hourOfDay.toString().padStart(2, '0')}:00"
}

private const val MIN_REMINDER_DELAY_MILLIS = 1_000L
