package com.agrogestao.pro.data.security

import android.annotation.SuppressLint
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SecureSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long
)

// Session changes must be durable before the legacy Room fields are cleared.
@SuppressLint("ApplySharedPref")
class SecureSessionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun save(session: SecureSession): Boolean {
        if (session.userId.isBlank() || session.accessToken.isBlank()) return false
        val encrypted = runCatching { encrypt(session) }
            .recoverCatching {
                resetKey()
                encrypt(session)
            }
            .getOrNull()
            ?: return false
        return preferences.edit().putString(SESSION_KEY, encrypted).commit()
    }

    @Synchronized
    fun read(): SecureSession? {
        val encrypted = preferences.getString(SESSION_KEY, null) ?: return null
        return runCatching { decrypt(encrypted) }
            .getOrElse {
                preferences.edit().remove(SESSION_KEY).commit()
                resetKey()
                null
            }
    }

    @Synchronized
    fun clear() {
        preferences.edit().remove(SESSION_KEY).commit()
    }

    private fun encrypt(session: SecureSession): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(ASSOCIATED_DATA)
        val payload = JSONObject().apply {
            put("user_id", session.userId)
            put("access_token", session.accessToken)
            put("refresh_token", session.refreshToken)
            put("expires_at", session.expiresAtEpochSeconds)
        }.toString().toByteArray(StandardCharsets.UTF_8)
        val ciphertext = cipher.doFinal(payload)
        return listOf(
            FORMAT_VERSION,
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        ).joinToString(":")
    }

    private fun decrypt(value: String): SecureSession {
        val parts = value.split(':', limit = 3)
        require(parts.size == 3 && parts[0] == FORMAT_VERSION)
        val iv = Base64.decode(parts[1], Base64.NO_WRAP)
        val ciphertext = Base64.decode(parts[2], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        cipher.updateAAD(ASSOCIATED_DATA)
        val json = JSONObject(
            String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
        )
        return SecureSession(
            userId = json.getString("user_id"),
            accessToken = json.getString("access_token"),
            refreshToken = json.optString("refresh_token"),
            expiresAtEpochSeconds = json.optLong("expires_at")
        ).also {
            require(it.userId.isNotBlank() && it.accessToken.isNotBlank())
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        ).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            generateKey()
        }
    }

    private fun resetKey() {
        runCatching {
            KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
                load(null)
                deleteEntry(KEY_ALIAS)
            }
        }
    }

    companion object {
        const val PREFERENCES_NAME = "agrogestao_secure_session"
        private const val SESSION_KEY = "encrypted_session"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "agrogestao_session_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val FORMAT_VERSION = "v1"
        private val ASSOCIATED_DATA = "AgroGestaoProSessionV1"
            .toByteArray(StandardCharsets.UTF_8)
    }
}
