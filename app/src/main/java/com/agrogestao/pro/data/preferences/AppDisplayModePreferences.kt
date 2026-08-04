package com.agrogestao.pro.data.preferences

import android.content.Context
import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/** Persiste a escolha visual separadamente para cada conta/proprietário local. */
class AppDisplayModePreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun observe(ownerUserId: String): Flow<Boolean> = callbackFlow {
        if (ownerUserId.isBlank()) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val key = key(ownerUserId)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) trySend(read(ownerUserId))
        }
        trySend(read(ownerUserId))
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    fun read(ownerUserId: String): Boolean =
        ownerUserId.isNotBlank() && preferences.getBoolean(key(ownerUserId), false)

    fun save(ownerUserId: String, simpleMode: Boolean): Boolean {
        require(ownerUserId.isNotBlank()) { "Conta ativa não encontrada." }
        return preferences.edit().putBoolean(key(ownerUserId), simpleMode).commit()
    }

    internal fun clearForTests(): Boolean = preferences.edit().clear().commit()

    private fun key(ownerUserId: String): String = "simple_mode_${digest(ownerUserId)}"

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .take(12)
        .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val PREFERENCES_NAME = "agrogestao_display_mode"
    }
}
