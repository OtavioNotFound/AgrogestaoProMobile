package com.agrogestao.pro

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.agrogestao.pro.data.local.AgroDatabase
import com.agrogestao.pro.data.repository.AgroRepository
import com.agrogestao.pro.data.reminders.TaskReminderPreferences
import com.agrogestao.pro.data.reminders.TaskReminderScheduler
import com.agrogestao.pro.data.reminders.TaskReminderService
import com.agrogestao.pro.data.security.SecureSessionStore
import com.agrogestao.pro.data.sync.PendingSyncWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AgroGestaoApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AgroDatabase by lazy {
        AgroDatabase.getDatabase(this)
    }

    val repository: AgroRepository by lazy {
        AgroRepository(
            backupDao = database.backupDao(),
            cropDao = database.cropDao(),
            taskDao = database.taskDao(),
            financialDao = database.financialDao(),
            producerDao = database.producerDao(),
            reportHistoryDao = database.reportHistoryDao(),
            reportConsentDao = database.reportConsentDao(),
            secureSessionStore = SecureSessionStore(this)
        )
    }

    val taskReminderPreferences: TaskReminderPreferences by lazy {
        TaskReminderPreferences(this)
    }

    val taskReminderService: TaskReminderService by lazy {
        TaskReminderService(
            preferences = taskReminderPreferences,
            scheduler = TaskReminderScheduler(this)
        )
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            repository.prepareLocalSession()
            taskReminderService.start(
                scope = applicationScope,
                activeOwnerUserId = repository.activeOwnerUserId,
                activeTasks = repository.allTasks
            )
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workManager = WorkManager.getInstance(this@AgroGestaoApp)
            workManager.enqueueUniquePeriodicWork(
                "agrogestao-pending-sync",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<PendingSyncWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()
            )
            workManager.enqueueUniqueWork(
                "agrogestao-startup-sync",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<PendingSyncWorker>()
                    .setConstraints(constraints)
                    .build()
            )
        }
    }
}
