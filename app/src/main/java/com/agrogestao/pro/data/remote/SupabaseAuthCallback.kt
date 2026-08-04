package com.agrogestao.pro.data.remote

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class SupabaseAuthCallback(
    val accessToken: String?,
    val refreshToken: String?,
    val expiresInSeconds: Long,
    val type: String?,
    val errorCode: String?
) {
    val hasSession: Boolean
        get() = !accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()
}

data class PasswordRecoverySession(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long
)

object SupabaseAuthCallbackParser {
    private const val CALLBACK_SCHEME = "com.agrogestao.pro"
    private const val CALLBACK_HOST = "auth"
    private const val CALLBACK_PATH = "/callback"
    private const val MAX_CALLBACK_LENGTH = 32_768

    fun parse(rawUrl: String?): SupabaseAuthCallback? {
        if (rawUrl.isNullOrBlank() || rawUrl.length > MAX_CALLBACK_LENGTH) return null
        val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return null
        if (
            !uri.scheme.equals(CALLBACK_SCHEME, ignoreCase = true) ||
            !uri.host.equals(CALLBACK_HOST, ignoreCase = true) ||
            uri.path.orEmpty().trimEnd('/') != CALLBACK_PATH
        ) {
            return null
        }

        val parameters = buildMap {
            putAll(decodeParameters(uri.rawQuery))
            putAll(decodeParameters(uri.rawFragment))
        }
        return SupabaseAuthCallback(
            accessToken = parameters["access_token"]?.takeIf(String::isNotBlank),
            refreshToken = parameters["refresh_token"]?.takeIf(String::isNotBlank),
            expiresInSeconds = parameters["expires_in"]?.toLongOrNull()?.coerceAtLeast(0) ?: 0,
            type = parameters["type"]?.takeIf(String::isNotBlank),
            errorCode = parameters["error_code"]
                ?.takeIf(String::isNotBlank)
                ?: parameters["error"]?.takeIf(String::isNotBlank)
        )
    }

    private fun decodeParameters(rawParameters: String?): Map<String, String> {
        if (rawParameters.isNullOrBlank()) return emptyMap()
        return rawParameters
            .split('&')
            .mapNotNull { item ->
                val separator = item.indexOf('=')
                val rawName = if (separator >= 0) item.substring(0, separator) else item
                val rawValue = if (separator >= 0) item.substring(separator + 1) else ""
                val name = decode(rawName).takeIf(String::isNotBlank) ?: return@mapNotNull null
                name to decode(rawValue)
            }
            .toMap()
    }

    private fun decode(value: String): String = runCatching {
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }.getOrDefault(value)
}
