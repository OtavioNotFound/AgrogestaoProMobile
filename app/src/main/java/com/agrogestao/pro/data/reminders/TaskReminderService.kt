package com.agrogestao.pro.data.reminders

import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.domain.TaskReminderSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

interface TaskReminderGateway {
    fun observe(ownerUserId: String): Flow<TaskReminderSettings>
    fun save(ownerUserId: String, settings: TaskReminderSettings): Boolean
}

object DisabledTaskReminderGateway : TaskReminderGateway {
    override fun observe(ownerUserId: String): Flow<TaskReminderSettings> =
        flowOf(TaskReminderSettings())

    override fun save(ownerUserId: String, settings: TaskReminderSettings): Boolean = false
}

class TaskReminderService(
    private val preferences: TaskReminderPreferences,
    private val scheduler: TaskReminderScheduler
) : TaskReminderGateway {
    override fun observe(ownerUserId: String): Flow<TaskReminderSettings> =
        preferences.observe(ownerUserId)

    override fun save(ownerUserId: String, settings: TaskReminderSettings): Boolean =
        preferences.save(ownerUserId, settings)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(
        scope: CoroutineScope,
        activeOwnerUserId: Flow<String>,
        activeTasks: Flow<List<TaskEntity>>
    ) {
        scope.launch {
            activeOwnerUserId.flatMapLatest { ownerUserId ->
                if (ownerUserId.isBlank()) {
                    flowOf(ReminderSnapshot("", TaskReminderSettings(), emptyList()))
                } else {
                    combine(activeTasks, preferences.observe(ownerUserId)) { tasks, settings ->
                        ReminderSnapshot(
                            ownerUserId = ownerUserId,
                            settings = settings,
                            tasks = tasks.filter { it.ownerUserId == ownerUserId }
                        )
                    }
                }
            }.collect { snapshot ->
                scheduler.replaceSchedules(
                    ownerUserId = snapshot.ownerUserId,
                    tasks = snapshot.tasks,
                    settings = snapshot.settings
                )
            }
        }
    }

    private data class ReminderSnapshot(
        val ownerUserId: String,
        val settings: TaskReminderSettings,
        val tasks: List<TaskEntity>
    )
}
