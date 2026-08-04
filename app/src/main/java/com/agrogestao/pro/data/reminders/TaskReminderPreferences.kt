package com.agrogestao.pro.data.reminders

import android.content.Context
import android.content.SharedPreferences
import com.agrogestao.pro.domain.TaskReminderSettings
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class TaskReminderPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun observe(ownerUserId: String): Flow<TaskReminderSettings> = callbackFlow {
        if (ownerUserId.isBlank()) {
            trySend(TaskReminderSettings())
            close()
            return@callbackFlow
        }
        val prefix = accountPrefix(ownerUserId)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key?.startsWith(prefix) == true) trySend(read(ownerUserId))
        }
        trySend(read(ownerUserId))
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    fun read(ownerUserId: String): TaskReminderSettings {
        if (ownerUserId.isBlank()) return TaskReminderSettings()
        val prefix = accountPrefix(ownerUserId)
        return TaskReminderSettings(
            enabled = preferences.getBoolean("${prefix}_enabled", false),
            daysBefore = preferences.getInt("${prefix}_days_before", 1),
            hourOfDay = preferences.getInt("${prefix}_hour", 7)
        ).takeIf(TaskReminderSettings::isValid) ?: TaskReminderSettings()
    }

    fun save(ownerUserId: String, settings: TaskReminderSettings): Boolean {
        require(ownerUserId.isNotBlank()) { "Conta ativa não encontrada." }
        require(settings.isValid) { "Configuração de lembrete inválida." }
        val prefix = accountPrefix(ownerUserId)
        return preferences.edit()
            .putBoolean("${prefix}_enabled", settings.enabled)
            .putInt("${prefix}_days_before", settings.daysBefore)
            .putInt("${prefix}_hour", settings.hourOfDay)
            .putLong("${prefix}_revision", System.currentTimeMillis())
            .commit()
    }

    @Synchronized
    fun markDeliveredIfNew(
        ownerUserId: String,
        taskCloudId: String,
        signature: String
    ): Boolean {
        if (ownerUserId.isBlank() || taskCloudId.isBlank() || signature.isBlank()) return false
        val key = "delivered_${digest("$ownerUserId:$taskCloudId")}"
        if (preferences.getString(key, null) == signature) return false
        return preferences.edit().putString(key, signature).commit()
    }

    internal fun clearForTests(): Boolean = preferences.edit().clear().commit()

    private fun accountPrefix(ownerUserId: String): String = "account_${digest(ownerUserId)}"

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .take(12)
        .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val PREFERENCES_NAME = "agrogestao_task_reminders"
    }
}
