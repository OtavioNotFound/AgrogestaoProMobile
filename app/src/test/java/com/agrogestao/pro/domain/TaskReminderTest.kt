package com.agrogestao.pro.domain

import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.data.local.entities.TaskStatus
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskReminderTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun `upcoming task is scheduled at configured local day and hour`() {
        val settings = TaskReminderSettings(enabled = true, daysBefore = 1, hourOfDay = 7)
        val now = utcMillis(2026, Calendar.AUGUST, 2, 12)

        val plan = buildTaskReminderPlans(
            tasks = listOf(task(dueDate = "2026-08-10")),
            settings = settings,
            nowEpochMillis = now,
            timeZone = utc
        ).single()

        assertEquals(
            utcMillis(2026, Calendar.AUGUST, 9, 7),
            plan.triggerAtEpochMillis
        )
        assertEquals("1 dia antes, às 07:00", taskReminderScheduleLabel(settings))
    }

    @Test
    fun `overdue task is immediate and delivery key changes only with deadline or settings`() {
        val settings = TaskReminderSettings(enabled = true, daysBefore = 0, hourOfDay = 7)
        val now = utcMillis(2026, Calendar.AUGUST, 2, 12)
        val original = task(dueDate = "2026-08-01", updatedAt = 100L)

        val plan = buildTaskReminderPlans(
            listOf(original),
            settings,
            now,
            utc
        ).single()

        assertEquals(now + 1_000L, plan.triggerAtEpochMillis)
        assertEquals(
            taskReminderDeliverySignature(original, settings),
            taskReminderDeliverySignature(original.copy(updatedAtEpochMillis = 101L), settings)
        )
        assertNotEquals(
            taskReminderDeliverySignature(original, settings),
            taskReminderDeliverySignature(original.copy(dataLimite = "2026-08-02"), settings)
        )
    }

    @Test
    fun `paused completed deleted and invalid tasks never create work`() {
        val active = task(dueDate = "2026-08-10")
        val completed = active.copy(cloudId = "completed", status = TaskStatus.CONCLUIDO)
        val deleted = active.copy(cloudId = "deleted", isDeleted = true)
        val invalid = active.copy(cloudId = "invalid", dataLimite = "10/08/2026")

        assertTrue(
            buildTaskReminderPlans(
                listOf(active),
                TaskReminderSettings(enabled = false),
                timeZone = utc
            ).isEmpty()
        )
        assertTrue(
            buildTaskReminderPlans(
                listOf(completed, deleted, invalid),
                TaskReminderSettings(enabled = true),
                timeZone = utc
            ).isEmpty()
        )
    }

    private fun task(
        dueDate: String,
        updatedAt: Long = 100L
    ) = TaskEntity(
        titulo = "Irrigar milho",
        descricao = "Talhão norte",
        categoria = "Irrigação",
        dataLimite = dueDate,
        cloudId = "task-${dueDate.replace("-", "")}",
        ownerUserId = "owner-1",
        updatedAtEpochMillis = updatedAt
    )

    private fun utcMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int
    ): Long = Calendar.getInstance(utc).apply {
        clear()
        set(year, month, day, hour, 0, 0)
    }.timeInMillis
}
