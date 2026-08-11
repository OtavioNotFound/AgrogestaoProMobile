package com.agrogestao.pro.data.preferences

import android.content.Context
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class FinancialPrivacyPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "agrogestao_financial_privacy",
        Context.MODE_PRIVATE
    )

    fun read(ownerUserId: String): Boolean =
        ownerUserId.isNotBlank() && preferences.getBoolean(key(ownerUserId), false)

    fun save(ownerUserId: String, hidden: Boolean): Boolean {
        require(ownerUserId.isNotBlank()) { "Conta ativa não encontrada." }
        return preferences.edit().putBoolean(key(ownerUserId), hidden).commit()
    }

    fun observe(ownerUserId: String): Flow<Boolean> = callbackFlow {
        if (ownerUserId.isBlank()) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val preferenceKey = key(ownerUserId)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == preferenceKey) trySend(read(ownerUserId))
        }
        trySend(read(ownerUserId))
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    internal fun clearForTests(): Boolean = preferences.edit().clear().commit()

    private fun key(ownerUserId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(ownerUserId.toByteArray(StandardCharsets.UTF_8))
            .take(12)
            .joinToString("") { byte -> "%02x".format(byte) }
        return "hide_values_$digest"
    }
}
